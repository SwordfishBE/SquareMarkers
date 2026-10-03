package net.squaremarkers.core.layers.primitive;

import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.FeedbackMessages;
import net.squaremarkers.core.helpers.HtmlHelper;
import net.squaremarkers.core.interfaces.ISimpleMarkerRepository;
import net.squaremarkers.core.interfaces.entities.ISimpleMarker;
import net.squaremarkers.core.markers.IconMarkerBuilder;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.objects.InteractionResult;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Optional;
import xyz.jpenilla.squaremap.api.MapWorld;

public abstract class SimpleMarkerLayer extends StoredMarkerLayer<ISimpleMarker, ISimpleMarkerRepository<? extends ISimpleMarker>> {

    public final String iconId;
    public final String key;
    public final String label;
    @Language("HTML")
    public final String tooltip;

    public SimpleMarkerLayer(String icon, String key, String label, @Language("HTML") String tooltip, @NotNull MapWorld world, int priority) {
        super(key, label, world, priority);
        iconId = icon;
        this.key = key;
        this.label = label;
        this.tooltip = tooltip;
    }

    @Override
    public void load() {
        getRepository().foreach(this::loadMarker);
    }

    final protected boolean addInternal(int x, int y, int z) {
        if (hasMarker(toMarkerKey(x, y, z))) {
            return false; // already exists
        }
        var marker = getRepository().create(x, y, z);
        if (marker != null) {
            loadMarker(marker);
			return true;
        }
		return false;
    }

    /**
     * Add a new marker
     *
     * @param x x coordinate of marker
     * @param y y coordinate of marker
     * @param z z coordinate of marker
     */
    public InteractionResult add(int x, int y, int z) {
        boolean added = addInternal(x, y, z);
        return added ? InteractionResult.added(FeedbackMessages.MARKER_ADD.text("type", tooltip)) : InteractionResult.skip();
    }

    /** Shows an existing marker's name without creating or changing a marker. */
    public InteractionResult interact(int x, int y, int z) {
        var marker = getRepository().get(x, y, z);
        if (marker == null || marker.getName() == null || marker.getName().isEmpty()) {
            return InteractionResult.skip();
        }
        return InteractionResult.feedback(FeedbackMessages.MARKER_INTERACT.text("type", tooltip, "name", marker.getName()));
    }

    @Override
    public Optional<? extends ISimpleMarker> getMarker(String key) {
        return getRepository().stream()
                .filter(m -> m.getKey().equals(key))
                .findFirst();
    }

    @Override
    public Optional<String> getClosestMarker(int x, int y, int z) {
        var closestMarker = getRepository().stream()
                .min((m1, m2) -> Comparator.comparingDouble(m ->
                                                                    ((ISimpleMarker) m).getPosition().distance(x, y, z)
                ).compare(m1, m2));
        return closestMarker.map(ISimpleMarker::getKey);
    }

    final protected boolean removeInternal(int x, int y, int z) {
        var removed = getRepository().remove(x, y, z);
        if (removed) {
            super.removeMarker(toMarkerKey(x, y, z));
        }
		return removed;
    }

    /**
     * Change the name of a marker
     *
     * @param x x coordinate of marker
     * @param y y coordinate of marker
     * @param z z coordinate of marker
     */
    public InteractionResult setName(int x, int y, int z, String newName) {
        var marker = getMarker(toMarkerKey(x, y, z));
        if (marker.isPresent() && marker.get().getName() != null && marker.get().getName().equals(newName)) {
            return interact(x, y, z);
        }
        boolean named = setNameInternal(x, y, z, newName);
        return named
                ? InteractionResult.added(FeedbackMessages.MARKER_RENAME.text("type", tooltip, "name", newName))
                : InteractionResult.failure(FeedbackMessages.MARKER_RENAME_FAILED.text("type", tooltip));
    }

    final protected boolean setNameInternal(int x, int y, int z, String newName) {
        var marker = getRepository().getOrCreate(x, y, z);
        if (marker == null) {
            return false;
        }
        marker.setName(newName);
        updateMarker(marker);
        return true;
    }

    /**
     * Change the color of a marker
     *
     * @param x x coordinate of marker
     * @param y y coordinate of marker
     * @param z z coordinate of marker
     */
    public InteractionResult setColor(int x, int y, int z, int newColor) {
        boolean colored = setColorInternal(x, y, z, newColor);
        return colored ? InteractionResult.added(FeedbackMessages.MARKER_COLOR.text("type", tooltip)) : InteractionResult.failure(
                FeedbackMessages.MARKER_COLOR_FAILED.text("type", tooltip));
    }

    final protected boolean setColorInternal(int x, int y, int z, int newColor) {
        var marker = getRepository().getOrCreate(x, y, z);
        if (marker == null) {
            return false;
        }
        marker.setColor(newColor);
        updateMarker(marker);
        return true;
    }

    @Override
    public MarkerBuilder<?> createBuilder(ISimpleMarker object) {
        var pos = object.getPosition();
        return IconMarkerBuilder.newIconMarker(
                        toMarkerKey(pos.x(), pos.y(), pos.z()), iconId, pos.x(), pos.z()
                )
                .centerIcon(16, 16);
    }

    @Override
    @Language("HTML")
    protected String createTooltip(ISimpleMarker markerEntity) {
        @Language("HTML") var name = markerEntity.getName();
        return HtmlHelper.sanitize(name != null ? name : tooltip);
    }

    /**
     * Remove a marker
     *
     * @param x x coordinate of marker
     * @param y y coordinate of marker
     * @param z z coordinate of marker
     */
    public InteractionResult remove(int x, int y, int z) {
        boolean removed = removeInternal(x, y, z);
        return removed ? InteractionResult.removed(FeedbackMessages.MARKER_REMOVE.text("type", tooltip)) : InteractionResult.skip();
    }

    @Override
    protected ISimpleMarkerRepository<? extends ISimpleMarker> getRepository() {
        return SquareMarkersCore.storage()
                .getWorldRepository(worldIdentifier)
                .getSimpleMarkerRepository(getKey());
    }

}
