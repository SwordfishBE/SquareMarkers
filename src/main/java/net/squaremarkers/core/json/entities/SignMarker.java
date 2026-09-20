package net.squaremarkers.core.json.entities;

import net.squaremarkers.core.interfaces.entities.ISignMarker;
import net.squaremarkers.core.json.repositories.MarkerRepository;

public class SignMarker extends PointMarker implements ISignMarker {

	private String[] text;

	public SignMarker(MarkerRepository<SignMarker> repository, Point point, String[] text) {
		super(repository, point);
		this.text = text;
	}

	@Override
	public String[] getText() {
		return text;
	}

	@Override
	public void setText(String[] text) {
		this.text = text;
		markDirty();
	}

	@Override
	public String getKey() {
		return super.getKey();
	}
}
