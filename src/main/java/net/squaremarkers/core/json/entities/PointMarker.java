package net.squaremarkers.core.json.entities;

import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.interfaces.entities.IPointMarker;
import net.squaremarkers.core.json.repositories.MarkerRepository;

public abstract class PointMarker extends Marker implements IPointMarker {

	private Point pos;

	public PointMarker(MarkerRepository<? extends PointMarker> repository, Point point) {
		super(repository);
		pos = point;
	}

	@Override
	public IPoint getPosition() {
		return pos;
	}

	public void setPosition(Point point) {
		pos = point;
	}
}
