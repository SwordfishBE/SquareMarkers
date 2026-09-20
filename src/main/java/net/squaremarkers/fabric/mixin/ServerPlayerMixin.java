package net.squaremarkers.fabric.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.interfaces.IBoundary;
import net.squaremarkers.core.layers.primitive.AreaMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

	@Unique
	private final ServerPlayer player = (ServerPlayer) (Object) this;
	@Unique
	private IBoundary boundary = null;
	@Unique
	private AreaMarkerLayer boundaryLayer;
	@Unique
	private long boundaryRevision = Long.MIN_VALUE;
	@Unique
	private BlockPos lastCheckedPosition;

	public ServerPlayerMixin(Level world, GameProfile profile) {
		super(world, profile);
	}

	@Override
	@Shadow
	public abstract @NonNull ServerLevel level();

	@Override
	protected void applyInput() {
		super.applyInput();

		if (!MarkersConfig.FEEDBACK_AREA_ENTER_ENABLED || !MarkersConfig.AREA_MARKERS_ENABLED) {
			clearBoundaryCache();
			return;
		}

		var pos = blockPosition();
		var worldIdentifier = level().dimension().identifier().toString();

		var markerLayer = SquareMarkersCore.api()
				.getWorld(worldIdentifier)
				.getLayer(AreaMarkerLayer.class, Layers.Keys.AREAS);
		if (markerLayer == null) {
			clearBoundaryCache();
			return;
		}
		long revision = markerLayer.revision();
		if (markerLayer == boundaryLayer && revision == boundaryRevision && pos.equals(lastCheckedPosition)) {
			return;
		}

		IBoundary previous = boundary;
		IBoundary current = markerLayer.getContaining(pos.getX(), pos.getZ()).orElse(null);
		boundaryLayer = markerLayer;
		boundaryRevision = revision;
		lastCheckedPosition = pos;

		String previousKey = previous == null ? null : previous.areaMarker().getKey();
		String currentKey = current == null ? null : current.areaMarker().getKey();
		boundary = current;
		if (java.util.Objects.equals(previousKey, currentKey)) {
			return;
		}
		if (previous != null) {
			FeedbackHelper.sendOverlayMessage(player, "[-] " + previous.areaMarker().getName(), previous.areaMarker().getColor());
		}
		if (current != null) {
			FeedbackHelper.sendOverlayMessage(player, "[+] " + current.areaMarker().getName(), current.areaMarker().getColor());
		}
	}

	@Unique
	private void clearBoundaryCache() {
		boundary = null;
		boundaryLayer = null;
		boundaryRevision = Long.MIN_VALUE;
		lastCheckedPosition = null;
	}

}
