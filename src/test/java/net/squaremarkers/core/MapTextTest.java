package net.squaremarkers.core;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.json.entities.Point;
import net.squaremarkers.core.json.entities.SimpleMarker;
import net.squaremarkers.core.layers.DeathMarkerLayer;
import net.squaremarkers.core.layers.EndPortalMarkerLayer;
import net.squaremarkers.core.layers.NetherPortalMarkerLayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;

import static org.junit.jupiter.api.Assertions.*;

class MapTextTest {
    @AfterEach void resetTemplates() { FeedbackMessages.reload(Map.of()); }

    @Test void translatedDeathPopupUsesCoordinatesZoneAndSafeSinglePassNames() throws Exception {
        FeedbackMessages.reload(Map.of("messages.death.title", "Laatste dood van {name}",
            "messages.death.position", "Locatie: {x} / {y} / {z}",
            "messages.death.time", "Overleden ({timezone}): {time}"));
        var layer = new DeathMarkerLayer(world("minecraft:overworld"));
        var popup = DeathMarkerLayer.class.getDeclaredMethod("createPopup", DeathStore.Death.class);
        popup.setAccessible(true);
        long at = Instant.parse("2026-10-03T19:46:34.087Z").toEpochMilli();
        var death = new DeathStore.Death("00000000-0000-0000-0000-000000000001", "Alex {time}<script>x</script>", "minecraft:overworld",
            -167, -64, -130, at, at + 1800_000);
        ZoneId previous = MarkersConfig.DEATH_MARKERS_TIMEZONE;
        try {
            MarkersConfig.DEATH_MARKERS_TIMEZONE = ZoneId.of("Europe/Brussels");
            assertEquals("<b>Laatste dood van Alex {time}&lt;script&gt;x&lt;/script&gt;</b>"
                + "<br>Locatie: -167 / -64 / -130<br>Overleden (CEST): 2026-10-03 21:46:34", popup.invoke(layer, death));
            FeedbackMessages.reload(Map.of("messages.death.title", "", "messages.death.position", "", "messages.death.time", ""));
            assertEquals("", popup.invoke(layer, death));
        } finally {
            MarkersConfig.DEATH_MARKERS_TIMEZONE = previous;
        }
    }

    @Test void translatedPortalButtonsKeepDestinationsAndEscapeUnsafeHtml() throws Exception {
        FeedbackMessages.reload(Map.of("messages.portal.go-to-end", "Naar het End",
            "messages.portal.go-to-nether", "Naar de Nether <script>x</script>",
            "messages.portal.go-to-overworld", "Naar de Overworld"));
        var marker = new SimpleMarker(null, new Point(80, 64, 160));
        var endPopup = EndPortalMarkerLayer.class.getDeclaredMethod("createPopup", ISimpleMarker.class);
        endPopup.setAccessible(true);
        String end = (String) endPopup.invoke(new EndPortalMarkerLayer(world("minecraft:overworld")), marker);
        assertTrue(end.contains("Naar het End"));
        assertTrue(end.contains("minecraft_the_end"));
        var netherPopup = NetherPortalMarkerLayer.class.getDeclaredMethod("createPopup", ISimpleMarker.class);
        netherPopup.setAccessible(true);
        String nether = (String) netherPopup.invoke(new NetherPortalMarkerLayer(world("minecraft:overworld")), marker);
        assertTrue(nether.contains("Naar de Nether &lt;script&gt;x&lt;/script&gt;"));
        assertTrue(nether.contains("minecraft_the_nether"));
        assertTrue(nether.contains("params.set('x', '10')"));
        String overworld = (String) netherPopup.invoke(new NetherPortalMarkerLayer(world("minecraft:the_nether")), marker);
        assertTrue(overworld.contains("Naar de Overworld"));
        assertTrue(overworld.contains("minecraft_overworld"));
        assertTrue(overworld.contains("params.set('x', '640')"));
    }

    private static MapWorld world(String id) {
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse(id);
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
