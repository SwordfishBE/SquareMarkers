package net.squaremarkers.fabric.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
			return;
		}

		var pos = blockPosition();
		var worldIdentifier = level().dimension().identifier().toString();

		if (boundary != null && boundary.contains(pos.getX(), pos.getZ())) {
			return;
		}

		var markerLayer = SquareMarkersCore.api()
				.getWorld(worldIdentifier)
				.getLayer(AreaMarkerLayer.class, Layers.Keys.AREAS);
		if (markerLayer == null) {
			return;
		}
		markerLayer
				.getContaining(pos.getX(), pos.getZ())
				.ifPresentOrElse(
				boundary ->
				{
					this.boundary = boundary;
					FeedbackHelper.sendOverlayMessage(
							player,
							"[+] " + boundary.areaMarker().getName(),
							boundary.areaMarker().getColor()
					);
				},
				() -> {
					if (boundary == null) {
						return;
					}
					FeedbackHelper.sendOverlayMessage(
							player,
							"[-] " + boundary.areaMarker().getName(),
							boundary.areaMarker().getColor()
					);
					boundary = null;
				}
		);
	}

}
