package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.EndGatewayMarkerLayer;
import net.squaremarkers.core.layers.EndPortalMarkerLayer;
import net.squaremarkers.core.objects.InteractionResult;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.helpers.PortalHelper;
import net.squaremarkers.fabric.helpers.GatewayHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PortalProcessor.class)
public class PortalProcessorMixin {

	@Shadow
	private BlockPos entryPosition;

	// On a player traveling through a nether portal, try to create a marker
	@Inject(method = "getPortalDestination", at = @At("RETURN"))
	public void onTick(ServerLevel serverLevel, Entity entity, CallbackInfoReturnable<TeleportTransition> cir) {
		if (portalsDisabled()) {
			return;
		}
		// origin
		tryCreateMarker(serverLevel, entryPosition, entity);
		// destination
		if (cir.getReturnValue() == null) {
			return;
		}
		tryCreateMarker(cir.getReturnValue().newLevel(), BlockPos.containing(cir.getReturnValue().position()), entity);
	}

	@Unique
	private void tryCreateMarker(Level world, BlockPos pos, Entity entity) {
		if (!(world instanceof ServerLevel level)) return;
		BlockState portalBlock = PortalHelper.loadedState(level, pos);
		if (portalBlock == null) return;
		InteractionResult result = InteractionResult.skip();
		if (portalBlock.is(Blocks.NETHER_PORTAL) && PortalHelper.hasNetherPortalLayer(level)) {
			BlockPos center = PortalHelper.getLoadedNetherPortalCenter(level, pos, portalBlock);
			if (center != null) {
				var layer = SquareMarkersCore.squaremapHandler().getLayer(world.dimension().identifier().toString(),
					net.squaremarkers.core.layers.NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
				FeedbackHelper.sendFeedback(layer.add(center.getX(), center.getY(), center.getZ()), world, center);
			}
		}
		if (portalBlock.is(Blocks.END_GATEWAY)) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(world.dimension().identifier().toString())
					.getLayer(EndGatewayMarkerLayer.class, Layers.Keys.END_GATEWAYS);
			if (markerLayer == null) {
				return;
			}
			result = markerLayer.add(pos.getX(), pos.getY(), pos.getZ());
			GatewayHelper.refresh(level, pos);
		}
		if (portalBlock.is(Blocks.END_PORTAL)) {
			pos = PortalHelper.getEndPortalCenter(world, pos);
			if (pos == null) return;
			var markerLayer = SquareMarkersCore.api()
					.getWorld(world.dimension().identifier().toString())
					.getLayer(EndPortalMarkerLayer.class, Layers.Keys.END_PORTALS);
			if (markerLayer == null) {
				return;
			}
			result = markerLayer.add(pos.getX(), pos.getY(), pos.getZ());
		}
		if (entity instanceof ServerPlayer serverPlayer) {
			FeedbackHelper.sendFeedback(result, serverPlayer);
		}
	}

	@Unique
	private boolean portalsDisabled() {
		return !(MarkersConfig.NETHER_PORTAL_MARKERS_ENABLED || MarkersConfig.END_GATEWAY_MARKERS_ENABLED || MarkersConfig.END_PORTAL_MARKERS_ENABLED);
	}

}
