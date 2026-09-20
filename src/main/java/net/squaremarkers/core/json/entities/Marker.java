package net.squaremarkers.core.json.entities;

import net.squaremarkers.core.interfaces.entities.IMarker;
import net.squaremarkers.core.json.repositories.MarkerRepository;

public abstract class Marker implements IMarker {

	private transient MarkerRepository<? extends Marker> repository;

	public Marker(MarkerRepository<? extends Marker> repository) {
		this.repository = repository;
	}

	public void SetContext(MarkerRepository<? extends Marker> repository) {
		this.repository = repository;
	}

	@Override
	public String getWorldIdentifier() {
		return repository.worldIdentifier;
	}

	@Override
	public String getLayerKey() {
		return repository.layerKey;
	}

	protected void markDirty() {
		repository.markDirty();
	}

}
