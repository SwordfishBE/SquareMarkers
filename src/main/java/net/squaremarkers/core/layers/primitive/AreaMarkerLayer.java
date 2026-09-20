package net.squaremarkers.core.layers.primitive;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.helpers.ConvexHull;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.interfaces.IAreaMarkerRepository;
import net.squaremarkers.core.interfaces.IBoundary;
import net.squaremarkers.core.interfaces.entities.IAreaMarker;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.markers.AreaMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.objects.CircleBoundary;
import net.squaremarkers.core.objects.InteractionResult;
import net.squaremarkers.core.objects.PolygonBoundary;
import net.squaremarkers.core.registries.Layers;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.*;
import xyz.jpenilla.squaremap.api.MapWorld;

public class AreaMarkerLayer extends StoredMarkerLayer<IAreaMarker, IAreaMarkerRepository<? extends IAreaMarker>> {

	private final Map<String, IBoundary> boundaries = new HashMap<>();
	private long revision;

	public AreaMarkerLayer(@NotNull MapWorld world) {
		super(Layers.Keys.AREAS, Layers.Labels.AREAS, world, MarkersConfig.AREA_MARKERS_PRIORITY);
    }

    @Override
    public void load() {
		boundaries.clear();
		clearMarkers();
	    getRepository().foreach(this::loadMarker);
		revision++;
    }

	@Override
	public MarkerBuilder<?> createBuilder(IAreaMarker area) {
		super.removeMarker(area);
		boundaries.remove(area.getKey());
		var points = area.getPoints();
		if (points == null || points.isEmpty()) {
			return null;
		}
		// If there are 2 points in line, make a circle instead of a polygon
		if (points.size() == 2 && areInline(points)) {
			var sorted = points.stream()
					.sorted(Comparator.comparingInt(IPoint::x).thenComparingInt(IPoint::z))
					.toList();
			var center = sorted.get(0).middle(sorted.get(1));
			var radius = (int) Math.round(sorted.get(0).distance(sorted.get(1)) / 2);
			boundaries.put(area.getKey(), new CircleBoundary(center, radius, area));
			return AreaMarkerBuilder.newAreaMarker(area.getKey(), center, radius)
					.fill(area.getColor())
					.stroke(area.getColor());
		}

		var orderedPoints = ConvexHull.calculate(new ArrayList<>(area.getPoints()));
		boundaries.put(area.getKey(), new PolygonBoundary(area.getMinCorner(), area.getMaxCorner(), orderedPoints, area));
		if (!orderedPoints.isEmpty()) {
			return AreaMarkerBuilder.newAreaMarker(area.getKey(), orderedPoints)
					.fill(area.getColor())
					.stroke(area.getColor());
		}
		return null;
	}

	private boolean areInline(Collection<? extends IPoint> points) {
		IPoint lastPoint = null;
		for (var point : points) {
			if (lastPoint == null) {
				lastPoint = point;
				continue;
			}
			if (lastPoint.x() != point.x() && lastPoint.z() != point.z()) {
				return false;
			}
			lastPoint = point;
		}
		return true;
	}

	private String createContent(IAreaMarker area) {
		var popupBuilder = new StringBuilder();
		popupBuilder.append(HtmlHelper.sanitize(area.getName()));
		if (MarkersConfig.AREA_MARKERS_SHOW_SIZE) {
			IBoundary boundary = boundaries.get(area.getKey());
			if (boundary == null) {
				var points = area.getPoints();
				if (points.size() == 2 && areInline(points)) {
					var sorted = points.stream().sorted(Comparator.comparingInt(IPoint::x).thenComparingInt(IPoint::z)).toList();
					var center = sorted.get(0).middle(sorted.get(1));
					boundary = new CircleBoundary(center, (int) Math.round(sorted.get(0).distance(sorted.get(1)) / 2), area);
				} else {
					boundary = new PolygonBoundary(area.getMinCorner(), area.getMaxCorner(), ConvexHull.calculate(new ArrayList<>(points)), area);
				}
			}
			var polygonArea = boundary.size();
			var areaFormatted = new DecimalFormat("#.#").format(polygonArea);
			popupBuilder
					.append("<br><i>")
					.append(areaFormatted)
					.append(" b²</i>");
		}
		return popupBuilder.toString();
	}

	@Override
	protected @Nullable String createPermanentCenteredTooltip(IAreaMarker object) {
		return MarkersConfig.AREA_MARKERS_MARKERS_ALWAYS_SHOW_NAME ? createContent(object) : null;
	}

	@Override
	protected @Nullable String createPopup(IAreaMarker object) {
		return !MarkersConfig.AREA_MARKERS_MARKERS_ALWAYS_SHOW_NAME ? createContent(object) : null;
	}

	@Override
	public Optional<? extends IAreaMarker> getMarker(String key) {
		return getRepository().stream()
				.filter(m -> m.getKey().equals(key))
				.findFirst();
	}

	@Override
	public Optional<String> getClosestMarker(int x, int y, int z) {
		return getRepository().stream()
				.min(Comparator.comparingDouble(m -> distanceFromArea(m, x, y, z)))
				.map(IAreaMarker::getKey);
	}

	private double distanceFromArea(IAreaMarker area, int x, int y, int z) {
		return area.getPoints().stream()
				.min(Comparator.comparingDouble(m -> m.distance(x, y, z)))
				.map(m -> m.distance(x, y, z))
				.orElse(Double.MAX_VALUE);
	}

    /**
     * Add a new point to an area
     */
    public InteractionResult addPoint(@Language("HTML") String label, int color, int x, int y, int z) {
	    var area = getRepository().getOrCreate(label, color);
	    if (area.addPoint(x, y, z)) {
		    loadMarker(area);
			revision++;
		    if (area.getPoints().size() == 1) {
			    return InteractionResult.added("Created area: " + label);
		    }
		    return InteractionResult.added("Added point to area: " + label);
        }
	    return InteractionResult.failure("Could not add point to area: " + label);
    }

    /**
     * Remove a point from an area
     */
    public InteractionResult removePoint(@Language("HTML") String label, int color, int x, int y, int z) {
	    var area = getRepository().get(label, color);
	    if (area != null && area.removePoint(x, y, z)) {
		    if (area.isEmpty()) {
			    super.removeMarker(area);
				boundaries.remove(area.getKey());
			    getRepository().remove(label, color);
				revision++;
			    return InteractionResult.removed("Removed area: " + label);
		    }
			loadMarker(area);
			revision++;
		    return InteractionResult.removed("Removed point from area: " + label);
        }
	    return InteractionResult.skip();
    }

	public Optional<IBoundary> getContaining(int x, int z) {
		return boundaries.values().stream()
		   .filter(b -> b.contains(x, z))
		   .findFirst();
	}

	public long revision() {
		return revision;
	}

	@Override
	protected IAreaMarkerRepository<? extends IAreaMarker> getRepository() {
		return SquareMarkersCore.storage()
				.getWorldRepository(worldIdentifier)
				.getAreaMarkerRepository(getKey());
	}

}
