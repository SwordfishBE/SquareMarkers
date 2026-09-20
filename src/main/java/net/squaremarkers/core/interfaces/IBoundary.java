package net.squaremarkers.core.interfaces;

import net.squaremarkers.core.interfaces.entities.IAreaMarker;

public interface IBoundary {

	boolean contains(int x, int z);

	IAreaMarker areaMarker();

	double size();

}
