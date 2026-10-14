package net.squaremarkers.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.squaremarkers.fabric.helpers.FeedbackHelper;
import net.squaremarkers.fabric.helpers.PortalHelper;
import net.squaremarkers.fabric.listeners.BlockListener;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class LevelMixin {

    @WrapMethod(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z")
    private boolean squaremarkers$onBlockChanged(BlockPos pos, BlockState state, int flags, int limit,
                                                 Operation<Boolean> original) {
        if (!((Object) this instanceof ServerLevel level)) return original.call(pos, state, flags, limit);
        // Invocation-local state also handles nested calls and exceptional exits without retaining records.
        BlockState oldState = level.getBlockState(pos);
        boolean breakingPortal = oldState.is(Blocks.NETHER_PORTAL) && !state.is(Blocks.NETHER_PORTAL);
        boolean breakingFrame = oldState.is(Blocks.OBSIDIAN) && !state.is(Blocks.OBSIDIAN);
        BlockPos center = breakingPortal || breakingFrame
            ? PortalHelper.findAffectedNetherPortalCenter(level, pos, oldState) : null;
        boolean changed = original.call(pos, state, flags, limit);
        if (changed) {
            var result = BlockListener.onChange(level, pos, oldState, state, center);
            if (result != null) FeedbackHelper.sendFeedback(result, level, pos);
        }
        return changed;
    }

}
