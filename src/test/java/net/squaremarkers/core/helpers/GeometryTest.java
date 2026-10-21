package net.squaremarkers.core.helpers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.json.entities.Point;
import org.junit.jupiter.api.Test;
import java.awt.geom.Path2D;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class GeometryTest {
    @Test
    void indexedOutlierRemovalPreservesPreviousResultsAndTieOrder() {
        int previous = MarkersConfig.AREA_MARKERS_MAX_SIZE;
        try {
            var random = new java.util.Random(417);
            for (int size : new int[]{1, 16, 512}) {
                MarkersConfig.AREA_MARKERS_MAX_SIZE = size;
                for (int attempt = 0; attempt < 100; attempt++) {
                    var points = new java.util.ArrayList<IPoint>();
                    for (int i = 0; i < 50; i++) points.add(new Point(random.nextInt(2001) - 1000,
                        random.nextInt(10), random.nextInt(2001) - 1000));
                    assertEquals(oldLimit(points, size), ConvexHull.limitPoints(points));
                }
            }
            MarkersConfig.AREA_MARKERS_MAX_SIZE = 16;
            assertEquals(oldLimit(List.of(new Point(-100, 0, 0), new Point(100, 0, 0)), 16),
                ConvexHull.limitPoints(List.of(new Point(-100, 0, 0), new Point(100, 0, 0))));
        } finally { MarkersConfig.AREA_MARKERS_MAX_SIZE = previous; }
    }

    @Test
    void largeScatteredAreaDoesNotScanEveryPointForEveryRemoval() {
        int previous = MarkersConfig.AREA_MARKERS_MAX_SIZE;
        try {
            MarkersConfig.AREA_MARKERS_MAX_SIZE = 512;
            var points = new java.util.ArrayList<IPoint>();
            var random = new java.util.Random(42);
            for (int i = 0; i < 20_000; i++) points.add(new Point(random.nextInt(60_000_001) - 30_000_000,
                64, random.nextInt(60_000_001) - 30_000_000));
            assertTimeout(java.time.Duration.ofSeconds(5), () -> {
                var limited = ConvexHull.limitPoints(points);
                assertFalse(limited.isEmpty());
                assertEquals(20_000, points.size(), "Input must remain unchanged");
                for (IPoint a : limited) for (IPoint b : limited) assertTrue(a.distance(b) <= 512);
            });
        } finally { MarkersConfig.AREA_MARKERS_MAX_SIZE = previous; }
    }

    /** Compatibility oracle for the old centroid/removal policy, intentionally used only on small inputs. */
    private static List<IPoint> oldLimit(List<IPoint> input, int size) {
        var points = new java.util.ArrayList<>(input);
        points.sort(null);
        double extent = Math.hypot((double) points.getLast().x() - points.getFirst().x(),
            (double) points.stream().mapToInt(IPoint::z).max().orElseThrow()
                - points.stream().mapToInt(IPoint::z).min().orElseThrow());
        if (extent <= size) return points;
        while (points.size() > 1) {
            long sumX = 0, sumZ = 0;
            for (IPoint point : points) { sumX += point.x(); sumZ += point.z(); }
            var center = new Point((int) (sumX / points.size()), 0, (int) (sumZ / points.size()));
            IPoint furthest = points.stream().max(java.util.Comparator.comparingDouble(p -> p.distance(center))).orElseThrow();
            if (furthest.distance(center) <= size / 2) break;
            points.remove(furthest);
        }
        return points;
    }

    @Test
    void fallbackShapeAlsoRespectsSmallLimits() {
        int previous = MarkersConfig.AREA_MARKERS_MAX_SIZE;
        try {
            for (int size : new int[]{1, 2, 8, 16}) {
                MarkersConfig.AREA_MARKERS_MAX_SIZE = size;
                var hull = ConvexHull.calculate(List.of(new Point(0, 0, 0)));
                for (IPoint a : hull) for (IPoint b : hull) assertTrue(a.distance(b) <= size);
            }
        } finally {
            MarkersConfig.AREA_MARKERS_MAX_SIZE = previous;
        }
    }
    @Test
    void appliesSizeLimitToRectanglesCirclesAndHiddenExtremePoints() {
        int previous = MarkersConfig.AREA_MARKERS_MAX_SIZE;
        try {
            MarkersConfig.AREA_MARKERS_MAX_SIZE = 512;
            for (List<IPoint> input : List.of(
                List.<IPoint>of(new Point(0, 0, 0), new Point(10000, 0, 10000)),
                List.<IPoint>of(new Point(0, 0, 0), new Point(0, 0, 10000)),
                List.<IPoint>of(new Point(0, 0, 0), new Point(1, 0, 10000), new Point(2, 0, 0)))) {
                var filtered = ConvexHull.limitPoints(input);
                assertTrue(filtered.size() < input.size());
                var hull = ConvexHull.calculate(input);
                for (IPoint a : hull) for (IPoint b : hull) assertTrue(a.distance(b) <= 512);
            }
        } finally {
            MarkersConfig.AREA_MARKERS_MAX_SIZE = previous;
        }
    }

    @Test
    void preservesHoleAndIslandInsideItRegardlessOfLoopOrder() {
        var parts = PolygonLoops.group(List.of(square(16, 16, 64), square(32, 32, 16), square(0, 0, 96)));
        assertEquals(2, parts.size());
        assertEquals(1, parts.stream().mapToInt(part -> part.holes().size()).sum());
        assertTrue(filled(parts, 8, 8));
        assertFalse(filled(parts, 24, 24));
        assertTrue(filled(parts, 40, 40));
        assertFalse(filled(parts, 100, 100));
    }

    @Test
    void cornerTouchingClaimsRemainFilledAndSeparate() {
        var parts = PolygonLoops.group(List.of(square(0, 0, 16), square(16, 16, 16)));
        assertEquals(2, parts.size());
        assertTrue(filled(parts, 8, 8));
        assertTrue(filled(parts, 24, 24));
        assertFalse(filled(parts, 8, 24));
    }

    @Test
    void spatialIndexPreservesNestedHolesAmongThousandsOfIslands() {
        var loops = new java.util.ArrayList<List<IPoint>>();
        for (int i = 0; i < 3000; i++) loops.add(square(1000 + (i % 100) * 32, (i / 100) * 32, 16));
        loops.add(square(0, 0, 96));
        loops.add(square(16, 16, 64));
        loops.add(square(32, 32, 16));
        var parts = PolygonLoops.group(loops);
        assertEquals(3002, parts.size());
        assertEquals(1, parts.stream().mapToInt(part -> part.holes().size()).sum());
        assertTrue(filled(parts, 8, 8));
        assertFalse(filled(parts, 24, 24));
        assertTrue(filled(parts, 40, 40));
        assertTrue(filled(parts, 1008, 8));
        assertFalse(filled(parts, 1024, 8));
    }

    private static boolean filled(List<PolygonLoops.Part> parts, int x, int z) {
        return parts.stream().anyMatch(part -> path(part.exterior()).contains(x, z)
            && part.holes().stream().noneMatch(hole -> path(hole).contains(x, z)));
    }

    private static Path2D path(List<IPoint> points) {
        var path = new Path2D.Double();
        path.moveTo(points.getFirst().x(), points.getFirst().z());
        points.forEach(point -> path.lineTo(point.x(), point.z()));
        path.closePath();
        return path;
    }

    private static List<IPoint> square(int x, int z, int size) {
        return List.of(new Point(x, 0, z), new Point(x + size, 0, z),
            new Point(x + size, 0, z + size), new Point(x, 0, z + size));
    }
}
