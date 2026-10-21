package net.squaremarkers.core;

import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.layers.CrossDimensionPlayerMarkerLayer;
import net.squaremarkers.core.objects.IconImageAddress;
import net.squaremarkers.core.objects.LayerFactory;
import net.squaremarkers.core.registries.Icons;
import net.squaremarkers.core.registries.Layers;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.squaremarkers.core.helpers.Cleanup;
import java.util.function.Consumer;

public final class SquaremapHandler {
    private final Map<String, Map<String, MarkerLayer<?>>> layers = new ConcurrentHashMap<>();

    public void initialize() {
        Squaremap api = SquaremapProvider.get();
        Icons.ALL.forEach(this::registerIconImage);
        registerCrossDimensionPlayerIcon(api);
        api.mapWorlds().forEach(this::registerWorld);
    }

    public void reload() {
        SquareMarkersCore.reloadConfig();
        SquaremapProvider.get().mapWorlds().forEach(this::registerWorld);
        SquareMarkersCore.debug("Reloaded config and markers");
    }

    public void registerWorld(MapWorld world) {
        unregisterWorld(world.identifier().asString());
        Layers.getAll().forEach(factory -> registerLayer(world, factory));
    }

    public void unregisterWorld(String worldIdentifier) {
        Map<String, MarkerLayer<?>> removed = layers.remove(worldIdentifier);
        if (removed == null) {
            return;
        }
        removed.values().forEach(layer -> {
            String description = layer.getKey() + " in " + worldIdentifier;
            Cleanup.run("close marker layer " + description, layer::close);
            Cleanup.run("clear marker layer " + description, layer::clearMarkers);
            Cleanup.run("unregister marker layer " + description, () -> {
                MapWorld world = layer.getWorld();
                Key key = layerKey(layer.getKey());
                if (world.layerRegistry().hasEntry(key)) {
                    world.layerRegistry().unregister(key);
                }
            });
        });
    }

    public void registerMarkerLayer(LayerFactory factory) {
        List<MapWorld> worlds = List.copyOf(SquaremapProvider.get().mapWorlds());
        // Preserve the old constructor for active-world registrations; infer its key only once.
        MapWorld first = null;
        MarkerLayer<?> prepared = null;
        if (factory.key() == null) {
            first = worlds.stream().filter(world -> !factory.disabledFor(world)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provide a LayerFactory key when no matching map world is enabled"));
            prepared = factory.create(first);
        }
        Layers.register(factory);
        if (prepared != null) registerLayer(first, prepared);
        for (MapWorld world : worlds) {
            if (world == first) continue;
            else if (factory.disabledFor(world)) unregisterLayer(world, factory.key());
            else registerLayer(world, factory);
        }
    }

    private void registerLayer(MapWorld world, LayerFactory factory) {
        if (factory.disabledFor(world)) {
            return;
        }
        MarkerLayer<?> layer = factory.create(world);
        registerLayer(world, layer);
    }

    private void unregisterLayer(MapWorld world, String key) {
        Map<String, MarkerLayer<?>> worldLayers = layers.get(world.identifier().asString());
        MarkerLayer<?> previous = worldLayers == null ? null : worldLayers.remove(key);
        try {
            if (previous != null) previous.close();
        } finally {
            if (world.layerRegistry().hasEntry(layerKey(key))) world.layerRegistry().unregister(layerKey(key));
        }
    }

    private void registerLayer(MapWorld world, MarkerLayer<?> layer) {
        Key registryKey = layerKey(layer.getKey());
        boolean registered = false;
        try {
            unregisterLayer(world, layer.getKey());
            world.layerRegistry().register(registryKey, layer.provider());
            registered = true;
            layers.computeIfAbsent(world.identifier().asString(), ignored -> new ConcurrentHashMap<>())
                .put(layer.getKey(), layer);
            layer.load();
        } catch (RuntimeException | Error failure) {
            var worldLayers = layers.get(world.identifier().asString());
            if (worldLayers != null) worldLayers.remove(layer.getKey(), layer);
            if (registered) {
                try { world.layerRegistry().unregister(registryKey); }
                catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            }
            try { layer.close(); }
            catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    public <T extends MarkerLayer<?>> T getLayer(String worldIdentifier, Class<T> layerClass, String layerKey) {
        MarkerLayer<?> layer = layers.getOrDefault(worldIdentifier, Map.of()).get(layerKey);
        return layerClass.isInstance(layer) ? layerClass.cast(layer) : null;
    }

    public void registerIconImage(String path, String filename, String filetype) {
        registerIconImage(new IconImageAddress(path, filename, filetype));
    }

    public void updateDynamicLayers() {
        layers.values().stream()
            .flatMap(worldLayers -> worldLayers.values().stream())
            .forEach(MarkerLayer::tick);
        layers.values().stream()
            .flatMap(worldLayers -> worldLayers.values().stream())
            .filter(CrossDimensionPlayerMarkerLayer.class::isInstance)
            .map(CrossDimensionPlayerMarkerLayer.class::cast)
            .forEach(CrossDimensionPlayerMarkerLayer::update);
    }

    public List<MarkerLayer<?>> activeLayers() {
        return layers.values().stream().flatMap(value -> value.values().stream())
            .sorted(java.util.Comparator.comparing((MarkerLayer<?> layer) -> layer.worldIdentifier)
                .thenComparing(MarkerLayer::getKey)).toList();
    }

    public MarkerLayer<?> findLayer(String world, String key) {
        return layers.getOrDefault(world, Map.of()).get(key);
    }

    public <T extends MarkerLayer<?>> void forEachLayer(Class<T> type, Consumer<T> action) {
        layers.values().forEach(world -> world.values().forEach(layer -> {
            if (type.isInstance(layer)) action.accept(type.cast(layer));
        }));
    }

    public void close() {
        List.copyOf(layers.keySet()).forEach(world -> Cleanup.run("unregister map world " + world, () -> unregisterWorld(world)));
        layers.clear();
    }

    private void registerIconImage(IconImageAddress address) {
        String resourceName = address.fileName().replaceFirst("^squaremarkers_", "");
        String resource = address.path() + resourceName + "." + address.fileType();
        try (InputStream input = SquaremapHandler.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing icon resource " + resource);
            }
            registerImage(Key.of(address.fileName()), ImageIO.read(input));
        } catch (IOException exception) {
            SquareMarkersCore.warn("Failed to register icon " + address.fileName(), exception);
        }
    }

    private void registerCrossDimensionPlayerIcon(Squaremap api) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(123, 75, 196, 220));
        graphics.fill(new Polygon(new int[]{8, 14, 10, 8, 6, 2}, new int[]{1, 13, 11, 15, 11, 13}, 6));
        graphics.setColor(new Color(255, 255, 255, 210));
        graphics.drawPolygon(new int[]{8, 14, 10, 8, 6, 2}, new int[]{1, 13, 11, 15, 11, 13}, 6);
        graphics.dispose();
        registerImage(Key.of(Icons.Keys.CROSS_DIMENSION_PLAYER), image);
    }

    private void registerImage(Key key, BufferedImage image) {
        var registry = SquaremapProvider.get().iconRegistry();
        if (registry.hasEntry(key)) {
            registry.unregister(key);
        }
        registry.register(key, image);
    }

    private static Key layerKey(String key) {
        return Key.of("squaremarkers_" + key);
    }
}
