package net.squaremarkers.core;

import net.squaremarkers.core.interfaces.ILogger;
import net.squaremarkers.core.interfaces.IStorage;
import net.squaremarkers.core.interfaces.api.IApi;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class SquareMarkersCore {
    private static final SquaremapHandler SQUAREMAP_HANDLER = new SquaremapHandler();
    private static final IApi API = new Api();
    private static IStorage storage;
    private static ILogger logger;
    private static Runnable reloadConfig = MarkersConfig::reload;
    private static MinecraftServer server;

    private SquareMarkersCore() {
    }

    public static void onInitialize(IStorage newStorage, ILogger newLogger, Runnable configReloader) {
        storage = newStorage;
        logger = newLogger;
        reloadConfig = configReloader;
        reloadConfig();
    }

    public static void onStarted() {
        SQUAREMAP_HANDLER.initialize();
        debug("Loaded config and markers");
    }

    public static void server(MinecraftServer minecraftServer) {
        server = minecraftServer;
    }

    public static MinecraftServer server() {
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not available");
        }
        return server;
    }

    public static void onDisable() {
        SQUAREMAP_HANDLER.close();
        if (storage != null) {
            storage.close();
        }
        storage = null;
        server = null;
        reloadConfig = MarkersConfig::reload;
    }

    public static void reloadMarkers() {
        SQUAREMAP_HANDLER.reload();
    }

    public static void reloadConfig() {
        reloadConfig.run();
    }

    public static IApi api() {
        return API;
    }

    public static SquaremapHandler squaremapHandler() {
        return SQUAREMAP_HANDLER;
    }

    public static IStorage storage() {
        if (storage == null) {
            throw new IllegalStateException("SquareMarkers storage is not initialized");
        }
        return storage;
    }

    public static Path getMainDir() {
        return Path.of("config/squaremarkers");
    }

    public static boolean isFeedbackDisabled() {
        return !MarkersConfig.FEEDBACK_MESSAGES_ENABLED && !MarkersConfig.FEEDBACK_SOUNDS_ENABLED;
    }

    public static boolean areFeedbackMessagesEnabled() {
        return MarkersConfig.FEEDBACK_MESSAGES_ENABLED;
    }

    public static boolean areFeedbackSoundsEnabled() {
        return MarkersConfig.FEEDBACK_SOUNDS_ENABLED;
    }

    public static void debug(String message) {
        if (logger != null) {
            logger.debug(message);
        }
    }

    public static void warn(String message, Throwable throwable) {
        if (logger != null) {
            logger.warn(message, throwable);
        }
    }

    public static void warn(String message) {
        if (logger != null) {
            logger.warn(message);
        }
    }
}
