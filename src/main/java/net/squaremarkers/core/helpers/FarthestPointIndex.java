package net.squaremarkers.core.helpers;

import net.squaremarkers.core.interfaces.entities.IPoint;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Temporary, shrinking spatial index. Keeps the original tie order when removing outliers. */
final class FarthestPointIndex {
    private final List<IPoint> points;
    private final Node[] leaves;
    private final Node root;

    FarthestPointIndex(List<IPoint> points) {
        this.points = points;
        leaves = new Node[points.size()];
        Integer[] order = new Integer[points.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        root = new Node(order, 0, order.length, null);
    }

    int size() { return root.count; }
    int centerX() { return (int) (root.sumX / root.count); }
    int centerZ() { return (int) (root.sumZ / root.count); }
    boolean contains(int index) { return leaves[index].count != 0; }

    int farthest(int x, int z) {
        Candidate result = new Candidate();
        root.find(x, z, result);
        return result.index;
    }

    void remove(int index) {
        Node leaf = leaves[index];
        leaf.count = 0;
        leaf.sumX = leaf.sumZ = 0;
        for (Node node = leaf.parent; node != null; node = node.parent) node.update();
    }

    private static final class Candidate {
        int index = -1;
        double distance = -1;
    }

    private final class Node {
        final Node parent, left, right;
        final int index;
        int count, minX, maxX, minZ, maxZ;
        long sumX, sumZ;

        Node(Integer[] order, int from, int to, Node parent) {
            this.parent = parent;
            minX = minZ = Integer.MAX_VALUE;
            maxX = maxZ = Integer.MIN_VALUE;
            for (int i = from; i < to; i++) {
                IPoint point = points.get(order[i]);
                minX = Math.min(minX, point.x()); maxX = Math.max(maxX, point.x());
                minZ = Math.min(minZ, point.z()); maxZ = Math.max(maxZ, point.z());
                sumX += point.x(); sumZ += point.z();
            }
            count = to - from;
            if (count == 1) {
                index = order[from];
                leaves[index] = this;
                left = right = null;
            } else {
                index = -1;
                boolean splitX = (long) maxX - minX >= (long) maxZ - minZ;
                Arrays.sort(order, from, to, Comparator.comparingInt(i ->
                    splitX ? points.get(i).x() : points.get(i).z()));
                int middle = (from + to) >>> 1;
                left = new Node(order, from, middle, this);
                right = new Node(order, middle, to, this);
            }
        }

        void update() {
            count = left.count + right.count;
            sumX = left.sumX + right.sumX; sumZ = left.sumZ + right.sumZ;
            if (count == 0) return;
            Node a = left.count == 0 ? right : left;
            Node b = right.count == 0 ? a : right;
            minX = Math.min(a.minX, b.minX); maxX = Math.max(a.maxX, b.maxX);
            minZ = Math.min(a.minZ, b.minZ); maxZ = Math.max(a.maxZ, b.maxZ);
        }

        double upperDistance(int x, int z) {
            if (count == 0) return -1;
            return Math.hypot(Math.max(Math.abs((double) minX - x), Math.abs((double) maxX - x)),
                Math.max(Math.abs((double) minZ - z), Math.abs((double) maxZ - z)));
        }

        void find(int x, int z, Candidate best) {
            if (count == 0 || upperDistance(x, z) < best.distance) return;
            if (index >= 0) {
                double distance = points.get(index).distance(x, z);
                if (distance > best.distance || distance == best.distance && index < best.index) {
                    best.index = index; best.distance = distance;
                }
                return;
            }
            Node first = left.upperDistance(x, z) >= right.upperDistance(x, z) ? left : right;
            Node second = first == left ? right : left;
            first.find(x, z, best);
            second.find(x, z, best);
        }
    }
}
