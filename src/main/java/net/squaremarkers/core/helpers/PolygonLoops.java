package net.squaremarkers.core.helpers;

import net.squaremarkers.core.interfaces.entities.IPoint;

import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import net.squaremarkers.core.json.entities.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CancellationException;

/** Groups boundary loops into filled exteriors with their immediate holes. */
public final class PolygonLoops {
    private PolygonLoops() {}

    public record Part(List<IPoint> exterior, List<List<IPoint>> holes) {}

    public static List<Part> group(List<List<IPoint>> loops) {
        loops = normalize(loops);
        List<Path2D.Double> paths = new ArrayList<>();
        List<Point2D.Double> probes = new ArrayList<>();
        for (List<IPoint> loop : loops) {
            Path2D.Double path = new Path2D.Double();
            if (!loop.isEmpty()) {
                path.moveTo(loop.getFirst().x(), loop.getFirst().z());
                loop.forEach(point -> path.lineTo(point.x(), point.z()));
                path.closePath();
            }
            paths.add(path);
            probes.add(interiorPoint(loop, path));
        }
        List<Rectangle2D> bounds = paths.stream().map(Path2D::getBounds2D).toList();
        Integer[] indices = new Integer[loops.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        BoundsNode index = indices.length == 0 ? null : new BoundsNode(indices, 0, indices.length, bounds);
        int[] parent = new int[loops.size()];
        Arrays.fill(parent, -1);
        for (int i = 0; i < loops.size(); i++) {
            checkInterrupted();
            parent[i] = index.parent(i, probes.get(i), bounds, paths, -1);
        }
        int[] depth = new int[loops.size()];
        Arrays.fill(depth, -1);
        List<List<List<IPoint>>> holes = new ArrayList<>();
        for (int i = 0; i < loops.size(); i++) holes.add(new ArrayList<>());
        for (int i = 0; i < loops.size(); i++) {
            // Iterative memoization also handles deeply nested boundaries without recursion.
            List<Integer> chain = new ArrayList<>();
            int current = i;
            while (current != -1 && depth[current] == -1) {
                chain.add(current);
                current = parent[current];
            }
            int value = current == -1 ? -1 : depth[current];
            for (int j = chain.size() - 1; j >= 0; j--) depth[chain.get(j)] = ++value;
            if (depth[i] % 2 != 0) holes.get(parent[i]).add(loops.get(i));
        }
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < loops.size(); i++) {
            if (loops.get(i).size() < 3 || depth[i] % 2 != 0) continue;
            parts.add(new Part(loops.get(i), List.copyOf(holes.get(i))));
        }
        return List.copyOf(parts);
    }

    /** Bounding-volume tree: disjoint islands never need all-pairs containment tests. */
    private static final class BoundsNode {
        private final Rectangle2D box;
        private final BoundsNode left, right;
        private final Integer[] indices;
        private final int from, to;

        BoundsNode(Integer[] indices, int from, int to, List<Rectangle2D> bounds) {
            checkInterrupted();
            this.indices = indices; this.from = from; this.to = to;
            box = (Rectangle2D) bounds.get(indices[from]).clone();
            for (int i = from + 1; i < to; i++) box.add(bounds.get(indices[i]));
            if (to - from <= 8) { left = null; right = null; return; }
            boolean splitX = box.getWidth() >= box.getHeight();
            Arrays.sort(indices, from, to, Comparator.comparingDouble(i ->
                splitX ? bounds.get(i).getCenterX() : bounds.get(i).getCenterY()));
            int mid = (from + to) >>> 1;
            left = new BoundsNode(indices, from, mid, bounds);
            right = new BoundsNode(indices, mid, to, bounds);
        }

        int parent(int child, Point2D point, List<Rectangle2D> bounds, List<Path2D.Double> paths, int best) {
            if (!box.contains(point)) return best;
            if (left != null) return right.parent(child, point, bounds, paths,
                left.parent(child, point, bounds, paths, best));
            Rectangle2D childBounds = bounds.get(child);
            for (int i = from; i < to; i++) {
                int candidate = indices[i];
                Rectangle2D candidateBounds = bounds.get(candidate);
                if (candidate == child || !candidateBounds.contains(childBounds)) continue;
                if (best != -1 && area(candidateBounds) >= area(bounds.get(best))) continue;
                if (paths.get(candidate).contains(point)) best = candidate;
            }
            return best;
        }

        private static double area(Rectangle2D box) { return box.getWidth() * box.getHeight(); }
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    private static Point2D.Double interiorPoint(List<IPoint> loop, Path2D.Double path) {
        for (int i = 0; i < loop.size(); i++) {
            IPoint a = loop.get(i), b = loop.get((i + 1) % loop.size());
            double dx = (double) b.x() - a.x(), dz = (double) b.z() - a.z();
            double length = Math.hypot(dx, dz);
            if (length == 0) continue;
            double x = (a.x() + (double) b.x()) / 2, z = (a.z() + (double) b.z()) / 2;
            for (int side : new int[]{1, -1}) {
                Point2D.Double probe = new Point2D.Double(x - side * dz / length * 0.01,
                    z + side * dx / length * 0.01);
                if (path.contains(probe)) return probe;
            }
        }
        throw new IllegalArgumentException("Boundary loop has no interior");
    }

    /** Splits corner-touching loops before classifying holes; chunk edges are axis-aligned. */
    private static List<List<IPoint>> normalize(List<List<IPoint>> loops) {
        Path2D.Double combined = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (List<IPoint> loop : loops) {
            checkInterrupted();
            if (loop.size() < 3) continue;
            combined.moveTo(loop.getFirst().x(), loop.getFirst().z());
            loop.forEach(point -> combined.lineTo(point.x(), point.z()));
            combined.closePath();
        }
        List<List<IPoint>> result = new ArrayList<>();
        List<IPoint> current = new ArrayList<>();
        double[] coordinates = new double[6];
        for (PathIterator iterator = new Area(combined).getPathIterator(null); !iterator.isDone(); iterator.next()) {
            checkInterrupted();
            int segment = iterator.currentSegment(coordinates);
            if (segment == PathIterator.SEG_MOVETO) current = new ArrayList<>();
            if (segment == PathIterator.SEG_MOVETO || segment == PathIterator.SEG_LINETO) {
                current.add(new Point((int) Math.round(coordinates[0]), 0, (int) Math.round(coordinates[1])));
            } else if (segment == PathIterator.SEG_CLOSE && current.size() >= 3) {
                result.add(List.copyOf(current));
            }
        }
        return result;
    }
}
