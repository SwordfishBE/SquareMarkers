package net.squaremarkers.core.helpers;

import net.squaremarkers.core.interfaces.entities.IPoint;

import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import net.squaremarkers.core.json.entities.Point;
import java.util.ArrayList;
import java.util.List;

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
        int[] depth = new int[loops.size()];
        for (int i = 0; i < loops.size(); i++) {
            if (loops.get(i).isEmpty()) continue;
            Point2D.Double point = probes.get(i);
            for (int j = 0; j < loops.size(); j++) {
                if (i != j && paths.get(j).contains(point)) depth[i]++;
            }
        }
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < loops.size(); i++) {
            if (loops.get(i).size() < 3 || depth[i] % 2 != 0) continue;
            List<List<IPoint>> holes = new ArrayList<>();
            for (int j = 0; j < loops.size(); j++) {
                if (loops.get(j).isEmpty() || depth[j] != depth[i] + 1) continue;
                if (paths.get(i).contains(probes.get(j))) holes.add(loops.get(j));
            }
            parts.add(new Part(loops.get(i), List.copyOf(holes)));
        }
        return List.copyOf(parts);
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
            if (loop.size() < 3) continue;
            combined.moveTo(loop.getFirst().x(), loop.getFirst().z());
            loop.forEach(point -> combined.lineTo(point.x(), point.z()));
            combined.closePath();
        }
        List<List<IPoint>> result = new ArrayList<>();
        List<IPoint> current = new ArrayList<>();
        double[] coordinates = new double[6];
        for (PathIterator iterator = new Area(combined).getPathIterator(null); !iterator.isDone(); iterator.next()) {
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
