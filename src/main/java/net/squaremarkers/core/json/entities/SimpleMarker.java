package net.squaremarkers.core.json.entities;

import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.json.repositories.MarkerRepository;

public class SimpleMarker extends PointMarker implements ISimpleMarker {

	private String name;
	private Integer color;

	public SimpleMarker(MarkerRepository<SimpleMarker> repository, Point point) {
		super(repository, point);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
		markDirty();
	}

	@Override
	public int getColor() {
		return color == null ? -1 : color;
	}

	@Override
	public void setColor(int color) {
		this.color = color;
		markDirty();
	}

}
