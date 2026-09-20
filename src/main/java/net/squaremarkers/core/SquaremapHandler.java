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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        SquaremapProvider.get().getWorldIfEnabled(xyz.jpenilla.squaremap.api.WorldIdentifier.parse(worldIdentifier))
            .ifPresent(world -> removed.values().forEach(layer -> {
                Key key = layerKey(layer.getKey());
                if (world.layerRegistry().hasEntry(key)) {
                    world.layerRegistry().unregister(key);
                }
            }));
    }

    public void registerMarkerLayer(LayerFactory factory) {
        Layers.register(factory);
        SquaremapProvider.get().mapWorlds().forEach(world -> registerLayer(world, factory));
    }

    private void registerLayer(MapWorld world, LayerFactory factory) {
        if (factory.disabledFor(world)) {
            return;
        }
        MarkerLayer<?> layer = factory.create(world);
        Key registryKey = layerKey(layer.getKey());
        if (world.layerRegistry().hasEntry(registryKey)) {
            world.layerRegistry().unregister(registryKey);
        }
        world.layerRegistry().register(registryKey, layer.provider());
        layers.computeIfAbsent(world.identifier().asString(), ignored -> new ConcurrentHashMap<>())
            .put(layer.getKey(), layer);
        layer.load();
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
            .filter(CrossDimensionPlayerMarkerLayer.class::isInstance)
            .map(CrossDimensionPlayerMarkerLayer.class::cast)
            .forEach(CrossDimensionPlayerMarkerLayer::update);
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
