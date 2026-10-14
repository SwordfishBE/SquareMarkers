package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.squaremarkers.fabric.helpers.GatewayHelper;
import net.squaremarkers.fabric.interfaces.EndGatewayInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TheEndGatewayBlockEntity.class)
public abstract class EndGatewayBlockEntityMixin implements EndGatewayInterface {
    @Shadow private BlockPos exitPortal;

    @Override public BlockPos squareMarkers$getExitPortal() { return exitPortal; }

    @Inject(method = "setExitPosition", at = @At("RETURN"))
    private void squareMarkers$onExitChanged(BlockPos destination, boolean exact, CallbackInfo ci) {
        var gateway = (TheEndGatewayBlockEntity) (Object) this;
        if (gateway.getLevel() instanceof ServerLevel level) GatewayHelper.refresh(level, gateway);
    }

    @Inject(method = "getPortalPosition", at = @At("RETURN"))
    private void squareMarkers$onTravel(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
        GatewayHelper.refresh(level, (TheEndGatewayBlockEntity) (Object) this);
    }
}
