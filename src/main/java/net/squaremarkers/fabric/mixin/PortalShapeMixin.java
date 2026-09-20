package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.helpers.PortalHelper;
import net.squaremarkers.fabric.interfaces.NetherPortalInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PortalShape.class)
public class PortalShapeMixin implements NetherPortalInterface {

    @Final
    @Shadow
    private Direction.Axis axis;

    @Final
    @Shadow
    private BlockPos bottomLeft;

    @Shadow
    @Final
    private int width;

    @Inject(method = "findEmptyPortalShape", at = @At("RETURN"))
    private static void onNewPortal(LevelAccessor level, BlockPos pos, Direction.Axis preferredAxis, CallbackInfoReturnable<Optional<PortalShape>> cir) {
        cir.getReturnValue().ifPresent(netherPortal -> {
            if (netherPortal instanceof NetherPortalInterface np) {
                np.squareMarkers$createMarker((Level) level);
            }
        });
    }

    @Override
    public void squareMarkers$createMarker(Level world) {
        var center = PortalHelper.getNetherPortalCenter(bottomLeft, axis, width);
        var markerLayer = SquareMarkersCore.api()
                .getWorld(world.dimension().identifier().toString())
                .getLayer(NetherPortalMarkerLayer.class, Layers.Keys.NETHER_PORTALS);
        if (markerLayer == null) {
            return;
        }
        var result = markerLayer.add(center.getX(), center.getY(), center.getZ());
		FeedbackHelper.sendFeedback(result, world, center);
	}

    @Override
    public BlockPos squareMarkers$getPortalCenter() {
        return PortalHelper.getNetherPortalCenter(bottomLeft, axis, width);
    }

}
