package net.squaremarkers.fabric;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarkerCommandsTest {
    @Test void filteredPortalMessageExplainsNameFilterInsteadOfOwnerExclusion() {
        var world = (xyz.jpenilla.squaremap.api.MapWorld) java.lang.reflect.Proxy.newProxyInstance(
            xyz.jpenilla.squaremap.api.MapWorld.class.getClassLoader(),
            new Class<?>[]{xyz.jpenilla.squaremap.api.MapWorld.class}, (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return xyz.jpenilla.squaremap.api.WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
        var portal = new net.squaremarkers.core.layers.NetherPortalMarkerLayer(world);
        String message = MarkerCommands.remainingHiddenMessage(portal);
        assertTrue(message.contains("only-show-named"));
        assertTrue(message.contains("Rename"));
        assertFalse(message.contains("owner"));
        assertTrue(MarkerCommands.remainingHiddenMessage(
            new net.squaremarkers.core.layers.primitive.AreaMarkerLayer(world)).contains("owner-level"));
    }

    @Test void integrationColorsAlwaysIncludeAnExplicitTextStatus() {
        var enabled = MarkerCommands.integrationStatus(true, true);
        assertEquals("enabled", enabled.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), enabled.getStyle().getColor());
        var disabled = MarkerCommands.integrationStatus(true, false);
        assertEquals("disabled", disabled.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW), disabled.getStyle().getColor());
        var missing = MarkerCommands.integrationStatus(false, false);
        assertEquals("not installed", missing.getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY), missing.getStyle().getColor());
        assertEquals("not installed", MarkerCommands.integrationStatus(false, true).getString());
    }
}
