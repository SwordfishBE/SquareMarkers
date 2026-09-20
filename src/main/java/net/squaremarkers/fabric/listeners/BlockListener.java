package net.squaremarkers.fabric.listeners;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.BeaconMarkerLayer;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.layers.SignsMarkerLayer;
import net.squaremarkers.core.layers.primitive.AreaMarkerLayer;
import net.squaremarkers.core.objects.InteractionResult;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.PortalHelper;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class BlockListener {

	@Nullable
	public static InteractionResult onPlace(@NonNull Level level, @NonNull BlockPos pos) {
		// area markers
		var blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof BannerBlockEntity banner && level.getBlockState(pos.below()).is(Blocks.LODESTONE)) {
			@Language("HTML") var name = banner.getName().tryCollapseToString();
			if (name == null) {
				return null;
			}
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(AreaMarkerLayer.class, Layers.Keys.AREAS);
			if (markerLayer == null) {
				return null;
			}
			return markerLayer.addPoint(
					name, banner.getBaseColor().getTextureDiffuseColor(),
					pos.getX(), pos.getY(), pos.getZ()
			);
		}
		return null;
	}

	@Nullable
	public static InteractionResult onDestroy(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state) {
		BlockPos portalCenter = PortalHelper.findAffectedNetherPortalCenter(level, pos, state);
		if (portalCenter != null) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
			if (markerLayer != null) {
				return markerLayer.remove(portalCenter.getX(), portalCenter.getY(), portalCenter.getZ());
			}
		}
		// beacon markers
		if (state.is(Blocks.BEACON)) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(BeaconMarkerLayer.class, Layers.Keys.BEACONS);
			if (markerLayer == null) {
				return null;
			}
			return markerLayer.remove(pos.getX(), pos.getY(), pos.getZ());
		}
		// area markers
		if (level instanceof ServerLevel serverWorld) {
			if (state.is(Blocks.LODESTONE)) {
				pos = pos.above();
			}
			var blockEntity = serverWorld.getBlockEntity(pos);
			if (blockEntity instanceof BannerBlockEntity banner) {
				var markerLayer = SquareMarkersCore.api()
						.getWorld(level.dimension().identifier().toString())
						.getLayer(AreaMarkerLayer.class, Layers.Keys.AREAS);
				if (markerLayer == null) {
					return null;
				}
				@Language("HTML") var name = banner.getName().tryCollapseToString();
				if (name == null) {
					return null;
				}
				return markerLayer.removePoint(
						name, banner.getBaseColor().getTextureDiffuseColor(),
						pos.getX(), pos.getY(), pos.getZ()
				);
			}
		}
		return null;
	}

	@Nullable
	public static InteractionResult onChange(@NonNull Level level, @NonNull BlockPos pos, @NonNull BlockState state,
	                                         @NonNull BlockState newState, @Nullable BlockPos portalCenter) {
		// beacon
		if (broke(Blocks.BEACON, state, newState)) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(BeaconMarkerLayer.class, Layers.Keys.BEACONS);
			if (markerLayer == null) {
				return null;
			}
			return markerLayer.remove(pos.getX(), pos.getY(), pos.getZ());
		}
		// nether portal
		if (portalCenter != null) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
			if (markerLayer == null) {
				return null;
			}
			InteractionResult result = markerLayer.remove(portalCenter.getX(), portalCenter.getY(), portalCenter.getZ());
			SquareMarkersCore.debug("Processed Nether portal removal at " + portalCenter + ": " + result.state());
			return result;
		}
		// signs
		if (broke(BlockTags.SIGNS, state, newState)) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(SignsMarkerLayer.class, Layers.Keys.SIGNS);
			if (markerLayer == null) {
				return null;
			}
			return markerLayer.remove(pos.getX(), pos.getY(), pos.getZ());
		}
		// banners
		if (broke(BlockTags.BANNERS, state, newState)) {
			var markerLayer = SquareMarkersCore.api()
					.getWorld(level.dimension().identifier().toString())
					.getLayer(AreaMarkerLayer.class, Layers.Keys.AREAS);
			if (markerLayer == null) {
				return null;
			}
			var blockEntity = level.getBlockEntity(pos);
			if (!(blockEntity instanceof BannerBlockEntity banner)) {
				return null;
			}
			@Language("HTML") var name = banner.getName().tryCollapseToString();
			if (name == null) {
				return null;
			}
			return markerLayer.removePoint(
					name, banner.getBaseColor().getTextureDiffuseColor(),
					pos.getX(), pos.getY(), pos.getZ()
			);
		}
		return null;
	}

	private static boolean broke(Block block, BlockState state, BlockState newState) {
		return state.is(block) && !newState.is(block);
	}

	private static boolean broke(TagKey<Block> block, BlockState state, BlockState newState) {
		return state.is(block) && !newState.is(block);
	}

}
