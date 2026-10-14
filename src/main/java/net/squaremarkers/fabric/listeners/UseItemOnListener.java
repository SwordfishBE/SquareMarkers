package net.squaremarkers.fabric.listeners;

import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.layers.EndPortalMarkerLayer;
import net.squaremarkers.core.layers.EndGatewayMarkerLayer;
import net.squaremarkers.core.layers.primitive.NamedPortalMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.helpers.PortalHelper;
import net.squaremarkers.fabric.helpers.GatewayHelper;
import net.squaremarkers.fabric.interfaces.NetherPortalInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.squaremarkers.core.objects.InteractionResult.State;

public class UseItemOnListener implements BlockEvents.UseItemOnCallback, BlockEvents.UseWithoutItemCallback {

	@Override
	public @Nullable InteractionResult useItemOn(@NonNull ItemStack itemStack, @NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull InteractionHand interactionHand, @NonNull BlockHitResult blockHitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !isPortal(blockState)) return null;
		if (interactionHand != preferredHand(hasNamedTag(player.getMainHandItem()), hasNamedTag(player.getOffhandItem()))) {
			// PASS skips the empty-hand block fallback, allowing an offhand name tag to be used.
			return interactionHand == InteractionHand.MAIN_HAND ? InteractionResult.PASS : null;
		}
		if (itemStack.isEmpty()) return null;
		var portalCenter = getPortalCenter(level, blockPos, blockState);
		if (portalCenter == null) {
			return null;
		}
		if (hasNamedTag(itemStack)) {
			return useNameTagOnPortal(portalLayer(level, blockState), serverPlayer, interactionHand, itemStack, portalCenter);
		}
		return usePortal(portalLayer(level, blockState), serverPlayer, interactionHand, portalCenter);
	}

	@Override
	public @Nullable InteractionResult useWithoutItem(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull BlockHitResult blockHitResult) {
		if (!(player instanceof ServerPlayer serverPlayer)) return null;
		if (preferredHand(hasNamedTag(player.getMainHandItem()), hasNamedTag(player.getOffhandItem())) != InteractionHand.MAIN_HAND) return null;
		var center = getPortalCenter(level, blockPos, blockState);
		return center == null ? null : usePortal(portalLayer(level, blockState), serverPlayer, InteractionHand.MAIN_HAND, center);
	}

	static InteractionHand preferredHand(boolean mainHasNamedTag, boolean offHasNamedTag) {
		return !mainHasNamedTag && offHasNamedTag ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	private static boolean hasNamedTag(ItemStack stack) {
		return stack.is(Items.NAME_TAG) && stack.getCustomName() != null;
	}

	private @Nullable InteractionResult usePortal(NamedPortalMarkerLayer layer, ServerPlayer player, InteractionHand hand, BlockPos center) {
		if (layer == null) return null;
		var result = layer.interact(center.getX(), center.getY(), center.getZ());
		if (result.state() == State.SKIP) return null;
		FeedbackHelper.sendFeedback(result, player);
		player.swing(hand, SwingAnimation.DEFAULT, true);
		return InteractionResult.SUCCESS;
	}

	private InteractionResult useNameTagOnPortal(NamedPortalMarkerLayer layer, ServerPlayer player, InteractionHand interactionHand, ItemStack nameTagItem, BlockPos portalCenter) {
		var customName = nameTagItem.getCustomName();
		if (customName == null) {
			return InteractionResult.PASS;
		}
		String name = customName.getString();
		if (layer == null) {
			return InteractionResult.PASS;
		}
		var result = layer.setName(
				portalCenter.getX(), portalCenter.getY(), portalCenter.getZ(), name
		);
		if (layer instanceof EndGatewayMarkerLayer && player.level() instanceof net.minecraft.server.level.ServerLevel level) {
			GatewayHelper.refresh(level, portalCenter);
		}
		FeedbackHelper.sendFeedback(result, player);
		if (result.state().equals(State.ADDED)) {
			nameTagItem.consume(1, player);
		}
		if (result.state() == State.ADDED || result.state() == State.FEEDBACK) {
			player.swing(interactionHand, SwingAnimation.DEFAULT, true);
		}
		return InteractionResult.SUCCESS;
	}

	@Nullable
	private BlockPos getPortalCenter(Level level, BlockPos pos, BlockState blockState) {
		if (blockState.is(Blocks.END_GATEWAY)) return pos;
		if (blockState.is(Blocks.END_PORTAL_FRAME)) return PortalHelper.getEndPortalCenterFromFrame(level, pos, blockState);
		if (blockState.is(Blocks.END_PORTAL)) return PortalHelper.getEndPortalCenter(level, pos);
		if (!blockState.is(Blocks.NETHER_PORTAL)) {
			return null;
		}
		Direction.Axis axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
		var shape = PortalShape.findAnyShape(level, pos, axis);
		return shape instanceof NetherPortalInterface portal ? portal.squareMarkers$getPortalCenter() : null;
	}

	private static boolean isPortal(BlockState state) {
		return state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.END_GATEWAY);
	}

	private static @Nullable NamedPortalMarkerLayer portalLayer(Level level, BlockState state) {
		String world = level.dimension().identifier().toString();
		if (state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME)) return SquareMarkersCore.squaremapHandler().getLayer(world, EndPortalMarkerLayer.class, Layers.Keys.END_PORTALS);
		if (state.is(Blocks.END_GATEWAY)) return SquareMarkersCore.squaremapHandler().getLayer(world, EndGatewayMarkerLayer.class, Layers.Keys.END_GATEWAYS);
		return SquareMarkersCore.squaremapHandler().getLayer(world, NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
	}

}
