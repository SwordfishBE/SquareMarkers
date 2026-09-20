package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.listeners.BlockListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {

    @Unique
    private final Level level = (Level) (Object) this;

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    public void onBlockChanged(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit, CallbackInfoReturnable<Boolean> cir) {
        var result = BlockListener.onChange(level, pos, level.getBlockState(pos), blockState);
        if (result != null) {
            FeedbackHelper.sendFeedback(result, level, pos);
        }
    }

}
