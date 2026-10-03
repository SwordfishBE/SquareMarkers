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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.interfaces.NetherPortalInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.squaremarkers.core.objects.InteractionResult.State;

public class UseItemOnListener implements BlockEvents.UseItemOnCallback, BlockEvents.UseWithoutItemCallback {

	@Override
	public @Nullable InteractionResult useItemOn(@NonNull ItemStack itemStack, @NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull InteractionHand interactionHand, @NonNull BlockHitResult blockHitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !blockState.is(Blocks.NETHER_PORTAL)) return null;
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
			return useNameTagOnPortal(level, serverPlayer, interactionHand, itemStack, portalCenter);
		}
		return usePortal(level, serverPlayer, portalCenter);
	}

	@Override
	public @Nullable InteractionResult useWithoutItem(@NonNull BlockState blockState, @NonNull Level level, @NonNull BlockPos blockPos, @NonNull Player player, @NonNull BlockHitResult blockHitResult) {
		if (!(player instanceof ServerPlayer serverPlayer)) return null;
		if (preferredHand(hasNamedTag(player.getMainHandItem()), hasNamedTag(player.getOffhandItem())) != InteractionHand.MAIN_HAND) return null;
		var center = getPortalCenter(level, blockPos, blockState);
		return center == null ? null : usePortal(level, serverPlayer, center);
	}

	static InteractionHand preferredHand(boolean mainHasNamedTag, boolean offHasNamedTag) {
		return !mainHasNamedTag && offHasNamedTag ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	private static boolean hasNamedTag(ItemStack stack) {
		return stack.is(Items.NAME_TAG) && stack.getCustomName() != null;
	}

	private @Nullable InteractionResult usePortal(Level level, ServerPlayer player, BlockPos center) {
		var layer = SquareMarkersCore.api().getWorld(level.dimension().identifier().toString())
			.getLayer(NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
		if (layer == null) return null;
		var result = layer.interact(center.getX(), center.getY(), center.getZ());
		if (result.state() == State.SKIP) return null;
		FeedbackHelper.sendFeedback(result, player);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		return InteractionResult.SUCCESS;
	}

	private InteractionResult useNameTagOnPortal(Level level, ServerPlayer player, InteractionHand interactionHand, ItemStack nameTagItem, BlockPos portalCenter) {
		var customName = nameTagItem.getCustomName();
		if (customName == null) {
			return InteractionResult.PASS;
		}
		String name = customName.getString();
		var layer = SquareMarkersCore.api()
				.getWorld(level.dimension().identifier().toString())
				.getLayer(NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
		if (layer == null) {
			return InteractionResult.PASS;
		}
		var result = layer.setName(
				portalCenter.getX(), portalCenter.getY(), portalCenter.getZ(), name
		);
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
	private BlockPos getPortalCenter(LevelAccessor level, BlockPos pos, BlockState blockState) {
		if (!blockState.is(Blocks.NETHER_PORTAL)) {
			return null;
		}
		Direction.Axis axis = blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS);
		var shape = PortalShape.findAnyShape(level, pos, axis);
		return shape instanceof NetherPortalInterface portal ? portal.squareMarkers$getPortalCenter() : null;
	}

}
