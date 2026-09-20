package net.squaremarkers.core.objects;

import net.squaremarkers.core.interfaces.IBoundary;
import net.squaremarkers.core.interfaces.entities.IAreaMarker;
import net.squaremarkers.core.interfaces.entities.IPoint;

public record CircleBoundary(IPoint center, int radius, IAreaMarker areaMarker) implements IBoundary {

	@Override
	public boolean contains(int x, int z) {
		return center.distance(x, z) <= radius;
	}

	@Override
	public double size() {
		return Math.PI * radius * radius;
	}

}
