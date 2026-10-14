package net.squaremarkers.fabric.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EndPortalFrameTest {
    private final BlockPos center = new BlockPos(15, 64, -16);

    private boolean isPortal(BlockPos pos) {
        return pos.getY() == center.getY() && Math.abs(pos.getX() - center.getX()) <= 1
            && Math.abs(pos.getZ() - center.getZ()) <= 1;
    }

    @Test void allTwelveFramesResolveToTheSameCenter() {
        for (Direction inward : Direction.Plane.HORIZONTAL) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos frame = center.relative(inward.getOpposite(), 2).relative(inward.getClockWise(), offset);
                assertEquals(center, PortalHelper.findActiveEndPortalFromFrame(frame, inward, this::isPortal));
            }
        }
    }

    @Test void incompletePortalsWrongFacingAndVerticalFramesDoNotRegister() {
        BlockPos frame = center.north(2);
        assertNull(PortalHelper.findActiveEndPortalFromFrame(frame, Direction.SOUTH,
            pos -> !pos.equals(center) && isPortal(pos)));
        assertNull(PortalHelper.findActiveEndPortalFromFrame(frame, Direction.NORTH, this::isPortal));
        assertNull(PortalHelper.findActiveEndPortalFromFrame(frame, Direction.UP, this::isPortal));
        assertNull(PortalHelper.findActiveEndPortalFromFrame(frame, Direction.SOUTH, pos -> false));
    }
}
