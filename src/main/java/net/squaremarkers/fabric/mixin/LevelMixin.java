package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.listeners.BlockListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(Level.class)
public class LevelMixin {

    @Unique
    private final Level level = (Level) (Object) this;

    @Unique
    private static final ThreadLocal<Deque<BlockState>> SQUAREMARKERS_OLD_STATES =
        ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void squaremarkers$captureOldState(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel) {
            SQUAREMARKERS_OLD_STATES.get().push(level.getBlockState(pos));
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void squaremarkers$onBlockChanged(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        Deque<BlockState> states = SQUAREMARKERS_OLD_STATES.get();
        BlockState oldState = states.isEmpty() ? blockState : states.pop();
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        var result = BlockListener.onChange(level, pos, oldState, blockState);
        if (result != null) {
            FeedbackHelper.sendFeedback(result, level, pos);
        }
    }

}
