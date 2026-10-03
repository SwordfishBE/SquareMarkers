package net.squaremarkers.core.layers;

import net.squaremarkers.core.DeathStore;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class DeathMarkerLayerTest {
    @Test void configuredZonesUseDeathDateForSummerAndWinterTime() {
        long summer = Instant.parse("2026-07-02T14:15:10.087Z").toEpochMilli();
        long winter = Instant.parse("2026-01-02T14:15:10.087Z").toEpochMilli();
        assertEquals("Time of death (CEST): 2026-07-02 16:15:10",
            DeathMarkerLayer.formatDeathTime(summer, ZoneId.of("Europe/Brussels")));
        assertEquals("Time of death (CET): 2026-01-02 15:15:10",
            DeathMarkerLayer.formatDeathTime(winter, ZoneId.of("Europe/Brussels")));
        assertEquals("Time of death (BST): 2026-07-02 15:15:10",
            DeathMarkerLayer.formatDeathTime(summer, ZoneId.of("Europe/London")));
        assertEquals("Time of death (GMT): 2026-01-02 14:15:10",
            DeathMarkerLayer.formatDeathTime(winter, ZoneId.of("Europe/London")));
        assertEquals("Time of death (UTC+01:00): 2026-07-02 15:15:10",
            DeathMarkerLayer.formatDeathTime(summer, ZoneId.of("UTC+01:00")));
    }
    @Test void popupShowsReadableUtcDeathTimeWithoutExpiryOrMilliseconds() {
        MapWorld world = (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> {
                if (method.getName().equals("identifier")) return WorldIdentifier.parse("minecraft:overworld");
                throw new UnsupportedOperationException(method.getName());
            });
        long time = Instant.parse("2026-10-02T14:15:10.087Z").toEpochMilli();
        var death = new DeathStore.Death("00000000-0000-0000-0000-000000000001", "Player", "minecraft:overworld",
            10, 65, 20, time, time + 1800_000);
        String popup = new DeathMarkerLayer(world).createPopup(death);
        assertEquals("<b>Player&#39;s last death</b><br>Position: 10, 65, 20"
            + "<br>Time of death (UTC): 2026-10-02 14:15:10", popup);
    }
}
