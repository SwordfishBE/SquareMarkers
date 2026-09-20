package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.BeaconMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.interfaces.BeaconBlockEntityInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin implements BeaconBlockEntityInterface {

    @Shadow
    private int levels;

    @Inject(method = "updateBase", at = @At("RETURN"))
    private static void updateLevel(Level level, int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
        BlockPos blockPos = new BlockPos(x, y, z);
	    BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof BeaconBlockEntityInterface beaconBlockEntityInterface
                && cir.getReturnValue() != beaconBlockEntityInterface.squareMarkers$getLevel()
        ) {
	        var markerLayer = SquareMarkersCore.api()
			        .getWorld(level.dimension().identifier().toString())
			        .getLayer(BeaconMarkerLayer.class, Layers.Keys.BEACONS);
	        if (markerLayer == null) {
		        return;
	        }
	        var result = cir.getReturnValue() > 0
			        ? markerLayer.add(blockPos.getX(), blockPos.getY(), blockPos.getZ())
			        : markerLayer.remove(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	        FeedbackHelper.sendFeedback(result, level, blockPos);
        }
    }

    @Override
    public int squareMarkers$getLevel() {
	    return levels;
    }

}
