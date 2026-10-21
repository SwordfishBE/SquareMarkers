package net.squaremarkers.core;

import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.core.markers.MarkerBuilder;
import net.squaremarkers.core.objects.LayerFactory;
import net.squaremarkers.core.registries.Layers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import xyz.jpenilla.squaremap.api.*;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LayerRegistrationTest {
    private static final String KEY = "registration_test";
    private final SquaremapHandler handler = new SquaremapHandler();
    private final List<MapWorld> worlds = new ArrayList<>();
    private final Map<Key, LayerProvider> registered = new HashMap<>();
    private final List<Layer> created = new ArrayList<>();
    private Map<String, LayerFactory> factories;
    private Map<String, LayerFactory> original;

    @BeforeEach @SuppressWarnings("unchecked")
    void setup() throws ReflectiveOperationException {
        var field = Layers.class.getDeclaredField("ALL");
        field.setAccessible(true);
        factories = (Map<String, LayerFactory>) field.get(null);
        original = new LinkedHashMap<>(factories);
        worlds.add(world("minecraft:overworld", registered));
        Squaremap api = (Squaremap) Proxy.newProxyInstance(Squaremap.class.getClassLoader(), new Class<?>[]{Squaremap.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "mapWorlds" -> worlds;
                case "getWorldIfEnabled" -> worlds.stream().filter(w -> w.identifier().equals(args[0])).findFirst();
                default -> throw new UnsupportedOperationException(method.getName());
            });
        var register = SquaremapProvider.class.getDeclaredMethod("register", Squaremap.class);
        register.setAccessible(true);
        register.invoke(null, api);
    }

    private MapWorld world(String id, Map<Key, LayerProvider> entries) {
        Registry<LayerProvider> registry = new Registry<>() {
            public void register(Key key, LayerProvider value) { assertNull(entries.putIfAbsent(key, value)); }
            public void unregister(Key key) { entries.remove(key); }
            public boolean hasEntry(Key key) { return entries.containsKey(key); }
            public LayerProvider get(Key key) { return entries.get(key); }
            public Iterable<Pair<Key, LayerProvider>> entries() { throw new UnsupportedOperationException(); }
        };
        return (MapWorld) Proxy.newProxyInstance(MapWorld.class.getClassLoader(), new Class<?>[]{MapWorld.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "identifier" -> WorldIdentifier.parse(id);
                case "layerRegistry" -> registry;
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    @AfterEach void cleanup() throws ReflectiveOperationException {
        handler.close();
        factories.clear(); factories.putAll(original);
        var unregister = SquaremapProvider.class.getDeclaredMethod("unregister");
        unregister.setAccessible(true);
        unregister.invoke(null);
    }

    private Layer newLayer(MapWorld world) {
        var layer = new Layer(world);
        created.add(layer);
        return layer;
    }

    private final class Layer extends MarkerLayer<Object> {
        int closed;
        boolean failLoad;
        boolean failClose;
        Layer(MapWorld world) { this(KEY, world); }
        Layer(String key, MapWorld world) { super(key, "Test", world, 1); }
        @Override public void load() {
            if (failLoad) throw new IllegalStateException("load failed");
        }
        @Override public MarkerBuilder<?> createBuilder(Object value) { return null; }
        @Override public void close() {
            closed++;
            if (failClose) throw new IllegalStateException("close failed");
            super.close();
        }
    }

    @Test void repeatedLegacyRegistrationsCloseReplacedLayersAndKeepOnlyLatestFactory() {
        int before = factories.size();
        LayerFactory latest = null;
        for (int i = 0; i < 100; i++) {
            latest = new LayerFactory(this::newLayer, world -> true);
            handler.registerMarkerLayer(latest);
        }
        assertEquals(before + 1, factories.size());
        assertSame(latest, factories.get(KEY));
        assertEquals(100, created.size());
        for (int i = 0; i < 99; i++) assertEquals(1, created.get(i).closed);
        assertEquals(0, created.getLast().closed);
        assertEquals(1, registered.size());
        assertSame(created.getLast(), handler.findLayer("minecraft:overworld", KEY));
        handler.close();
        assertEquals(1, created.getLast().closed);
        assertTrue(registered.isEmpty());
    }

    @Test void replacementWithDisabledPredicateRetiresOldLayerToo() {
        handler.registerMarkerLayer(new LayerFactory(KEY, this::newLayer, world -> true));
        handler.registerMarkerLayer(new LayerFactory(KEY, this::newLayer, world -> false));
        assertEquals(1, created.getFirst().closed);
        assertTrue(registered.isEmpty());
        assertNull(handler.findLayer("minecraft:overworld", KEY));
    }

    @Test void explicitKeysAllowReplacingFactoriesBeforeWorldsExist() {
        worlds.clear();
        int before = factories.size();
        for (int i = 0; i < 100; i++) {
            handler.registerMarkerLayer(new LayerFactory(KEY, this::newLayer, world -> true));
        }
        assertEquals(before + 1, factories.size());
        assertTrue(created.isEmpty());
        assertThrows(IllegalArgumentException.class,
            () -> handler.registerMarkerLayer(new LayerFactory(this::newLayer, world -> true)));
        assertEquals(before + 1, factories.size());
    }

    @Test void failedLoadAndMismatchedKeysReleaseNewLayers() {
        assertThrows(IllegalStateException.class, () -> handler.registerMarkerLayer(new LayerFactory(KEY, world -> {
            Layer layer = newLayer(world); layer.failLoad = true; return layer;
        }, world -> true)));
        assertEquals(1, created.getFirst().closed);
        assertTrue(registered.isEmpty());
        assertNull(handler.findLayer("minecraft:overworld", KEY));
        assertThrows(IllegalArgumentException.class,
            () -> handler.registerMarkerLayer(new LayerFactory("wrong_key", this::newLayer, world -> true)));
        assertEquals(1, created.getLast().closed);
    }

    @Test void failedPreviousCleanupAlsoClosesTheReplacementAndUnregistersTheProvider() {
        handler.registerMarkerLayer(new LayerFactory(KEY, this::newLayer, world -> true));
        created.getFirst().failClose = true;
        assertThrows(IllegalStateException.class,
            () -> handler.registerMarkerLayer(new LayerFactory(KEY, this::newLayer, world -> true)));
        assertEquals(1, created.getFirst().closed);
        assertEquals(1, created.getLast().closed);
        assertTrue(registered.isEmpty());
        assertNull(handler.findLayer("minecraft:overworld", KEY));
    }

    @Test void bulkCloseContinuesAcrossLayersAndWorldsAndClearsFailedProviders() {
        Map<Key, LayerProvider> otherRegistry = new HashMap<>();
        worlds.add(world("minecraft:the_nether", otherRegistry));
        handler.registerMarkerLayer(new LayerFactory(KEY, this::newLayer, world -> true));
        handler.registerMarkerLayer(new LayerFactory("another_layer", world -> {
            Layer layer = new Layer("another_layer", world); created.add(layer); return layer;
        }, world -> true));
        for (Layer layer : created) {
            layer.addMarker("marker", xyz.jpenilla.squaremap.api.marker.Marker.circle(Point.of(0, 0), 1));
            if (layer.getKey().equals(KEY)) layer.failClose = true;
        }
        assertDoesNotThrow(handler::close);
        assertEquals(4, created.size());
        created.forEach(layer -> {
            assertEquals(1, layer.closed);
            assertEquals(0, layer.logicalCount());
            assertEquals(0, layer.visibleCount());
        });
        assertTrue(registered.isEmpty());
        assertTrue(otherRegistry.isEmpty());
        assertTrue(handler.activeLayers().isEmpty());
    }

    @Test void shutdownReleasesCoreStorageReferenceEvenWhenStorageCloseThrows() {
        int[] closes = {0};
        SquareMarkersCore.onInitialize(new net.squaremarkers.core.interfaces.IStorage() {
            public net.squaremarkers.core.interfaces.IWorldRepository getWorldRepository(String world) {
                throw new UnsupportedOperationException();
            }
            public void close() { closes[0]++; throw new IllegalStateException("storage close failed"); }
        }, null, () -> {});
        assertDoesNotThrow(SquareMarkersCore::onDisable);
        assertEquals(1, closes[0]);
        assertThrows(IllegalStateException.class, SquareMarkersCore::storage);
        assertThrows(IllegalStateException.class, SquareMarkersCore::server);
    }
}
