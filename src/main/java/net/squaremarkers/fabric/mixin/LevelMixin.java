package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.helpers.PortalHelper;
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
    private static final ThreadLocal<Deque<BlockChange>> SQUAREMARKERS_BLOCK_CHANGES =
        ThreadLocal.withInitial(ArrayDeque::new);

    @Unique
    private record BlockChange(BlockState oldState, BlockPos portalCenter) {
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void squaremarkers$captureOldState(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof ServerLevel) {
            BlockState oldState = level.getBlockState(pos);
            SQUAREMARKERS_BLOCK_CHANGES.get().push(new BlockChange(
                oldState,
                squaremarkers$findAffectedPortalCenter(pos, oldState, blockState)
            ));
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void squaremarkers$onBlockChanged(BlockPos pos, BlockState blockState, int updateFlags, int updateLimit,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        Deque<BlockChange> changes = SQUAREMARKERS_BLOCK_CHANGES.get();
        BlockChange change = changes.isEmpty() ? new BlockChange(blockState, null) : changes.pop();
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        var result = BlockListener.onChange(level, pos, change.oldState(), blockState, change.portalCenter());
        if (result != null) {
            FeedbackHelper.sendFeedback(result, level, pos);
        }
    }

    @Unique
    private BlockPos squaremarkers$findAffectedPortalCenter(BlockPos changedPos, BlockState oldState, BlockState newState) {
        boolean breakingPortal = oldState.is(Blocks.NETHER_PORTAL) && !newState.is(Blocks.NETHER_PORTAL);
        boolean breakingFrame = oldState.is(Blocks.OBSIDIAN) && !newState.is(Blocks.OBSIDIAN);
        if (!breakingPortal && !breakingFrame) {
            return null;
        }
        BlockPos center = PortalHelper.findAffectedNetherPortalCenter(level, changedPos, oldState);
        if (center != null) {
            SquareMarkersCore.debug("Detected Nether portal removal at " + center);
        }
        return center;
    }

}
