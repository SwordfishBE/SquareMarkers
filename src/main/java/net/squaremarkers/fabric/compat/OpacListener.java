package net.squaremarkers.fabric.compat;

import net.minecraft.resources.Identifier;
import net.squaremarkers.fabric.compat.layers.OPACAreaMarkerLayer;
import org.jetbrains.annotations.NotNull;
import xaero.pac.common.claims.player.api.IPlayerChunkClaimAPI;
import xaero.pac.common.claims.tracker.api.IClaimsManagerListenerAPI;

public class OpacListener implements IClaimsManagerListenerAPI {

	@Override
	public void onWholeRegionChange(@NotNull Identifier world, int x, int z) {
		OPACAreaMarkerLayer markerLayer = OpacHandler.activeLayer(world.toString());
		if (markerLayer != null) {
			markerLayer.invalidate();
		}
	}

	@Override
	public void onChunkChange(@NotNull Identifier world, int x, int z, IPlayerChunkClaimAPI p) {
		OPACAreaMarkerLayer markerLayer = OpacHandler.activeLayer(world.toString());
		if (markerLayer == null) {
			return;
		}
		if (p == null) {
			// chunk unclaimed
			markerLayer.removeChunk(x, z, true);
		} else {
			// chunk claimed
			OpacChunk chunk = OpacHandler.getChunk(markerLayer.getServer(), p.getPlayerId(), x, z);
			if (chunk != null) {
				markerLayer.addChunk(chunk, true);
			}
		}
	}

	@Override
	public void onDimensionChange(Identifier world) {
		OPACAreaMarkerLayer markerLayer = OpacHandler.activeLayer(world.toString());
		if (markerLayer != null) {
			markerLayer.invalidate();
		}
	}

}
