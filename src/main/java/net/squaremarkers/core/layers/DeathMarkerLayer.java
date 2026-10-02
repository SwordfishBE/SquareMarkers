package net.squaremarkers.core.layers;

import net.squaremarkers.core.*;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.MapWorld;
import java.time.Instant;

public final class DeathMarkerLayer extends MarkerLayer<DeathStore.Death> {
    public DeathMarkerLayer(MapWorld world) {
        super(Layers.Keys.DEATHS, "Player Deaths", world, MarkersConfig.DEATH_MARKERS_PRIORITY);
    }
    @Override public void load() {
        DeathMarkers.current().stream().filter(death -> death.world().equals(worldIdentifier)).forEach(this::loadMarker);
    }
    @Override protected String markerKey(DeathStore.Death death) { return death.player(); }
    @Override public MarkerBuilder<?> createBuilder(DeathStore.Death death) {
        return IconMarkerBuilder.newIconMarker(death.player(), Icons.Keys.DEATH, death.x(), death.z()).centerIcon(16, 16);
    }
    @Override protected String createTooltip(DeathStore.Death death) {
        return HtmlHelper.sanitize(death.name()) + "'s last death";
    }
    @Override protected String createPopup(DeathStore.Death death) {
        return "<b>" + HtmlHelper.sanitize(death.name()) + "'s last death</b><br>"
            + "Position: " + death.x() + ", " + death.y() + ", " + death.z()
            + "<br>Time (UTC): " + Instant.ofEpochMilli(death.diedAt())
            + "<br>Expires (UTC): " + Instant.ofEpochMilli(death.expiresAt());
    }
}
