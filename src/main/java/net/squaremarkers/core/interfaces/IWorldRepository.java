package net.squaremarkers.core.interfaces;

import net.squaremarkers.core.interfaces.entities.IAreaMarker;
import net.squaremarkers.core.interfaces.entities.ISignMarker;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;

public interface IWorldRepository {

	IAreaMarkerRepository<? extends IAreaMarker> getAreaMarkerRepository(String layerKey);

	ISimpleMarkerRepository<? extends ISimpleMarker> getSimpleMarkerRepository(String layerKey);

	ISignMarkerRepository<? extends ISignMarker> getSignMarkerRepository(String layerKey);

}