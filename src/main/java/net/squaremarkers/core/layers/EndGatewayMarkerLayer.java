package net.squaremarkers.core.layers;

import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.FeedbackMessages;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.interfaces.entities.IPoint;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.layers.primitive.NamedPortalMarkerLayer;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import org.jetbrains.annotations.NotNull;
import xyz.jpenilla.squaremap.api.MapWorld;
import java.util.function.Function;

public class EndGatewayMarkerLayer extends NamedPortalMarkerLayer {
    @Override protected boolean renameEnabled() { return MarkersConfig.END_GATEWAY_MARKERS_RENAME; }
    @Override protected boolean onlyShowNamed() { return MarkersConfig.END_GATEWAY_MARKERS_ONLY_SHOW_NAMED; }

    public EndGatewayMarkerLayer(@NotNull MapWorld world) {
        super(Icons.Keys.END_GATEWAY, Layers.Keys.END_GATEWAYS, Layers.Labels.END_GATEWAYS, Layers.Tooltips.END_GATEWAYS, world, MarkersConfig.END_GATEWAY_MARKERS_PRIORITY);
    }

    /** Updates registered sources only; unchanged destinations do not rebuild or dirty storage. */
    public void setDestination(int x, int y, int z, IPoint destination) {
        var marker = getRepository().get(x, y, z);
        if (marker != null) updateDestination(marker, destination);
    }

    public void refreshDestinations(Function<ISimpleMarker, IPoint> resolver) {
        getRepository().foreach(marker -> updateDestination(marker, resolver.apply(marker)));
    }

    private void updateDestination(ISimpleMarker marker, IPoint destination) {
        IPoint current = marker.getGatewayDestination();
        if (current == null ? destination == null : destination != null && current.x() == destination.x()
            && current.y() == destination.y() && current.z() == destination.z()) return;
        marker.setGatewayDestination(destination);
        updateMarker(marker);
    }

    @Override protected String createPopup(ISimpleMarker marker) {
        var target = marker.getGatewayDestination();
        return target == null ? null : HtmlHelper.travelPopup(createTooltip(marker), worldIdentifier.replace(':', '_'),
            target.x(), target.z(), FeedbackMessages.PORTAL_GO_TO_END_GATEWAY.text());
    }
}
