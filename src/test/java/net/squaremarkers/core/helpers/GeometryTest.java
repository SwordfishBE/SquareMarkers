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
