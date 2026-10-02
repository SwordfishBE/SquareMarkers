package net.squaremarkers.fabric;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarkerCommandsTest {
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
