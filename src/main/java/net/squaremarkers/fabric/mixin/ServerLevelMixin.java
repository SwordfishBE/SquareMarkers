package net.squaremarkers.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.LightningMarkerLayer;
import net.squaremarkers.core.registries.Layers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("RETURN"))
    private void squareMarkers$onAddFreshEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !(entity instanceof LightningBolt lightning)) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        LightningMarkerLayer layer = SquareMarkersCore.api()
            .getWorld(level.dimension().identifier().toString())
            .getLayer(LightningMarkerLayer.class, Layers.Keys.LIGHTNING);
        if (layer != null) {
            layer.show(lightning.getBlockX(), lightning.getBlockY(), lightning.getBlockZ());
        }
    }
}
