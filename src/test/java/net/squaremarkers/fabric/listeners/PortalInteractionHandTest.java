package net.squaremarkers.fabric.listeners;

import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PortalInteractionHandTest {
    @Test void namedOffhandTagTakesPriorityOverOrdinaryMainHandFeedback() {
        assertEquals(InteractionHand.OFF_HAND, UseItemOnListener.preferredHand(false, true));
        assertEquals(InteractionHand.MAIN_HAND, UseItemOnListener.preferredHand(true, false));
        // Two named tags must not trigger two renames/consume both tags for one click.
        assertEquals(InteractionHand.MAIN_HAND, UseItemOnListener.preferredHand(true, true));
        assertEquals(InteractionHand.MAIN_HAND, UseItemOnListener.preferredHand(false, false));
    }
}
