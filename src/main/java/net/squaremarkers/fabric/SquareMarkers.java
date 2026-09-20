package net.squaremarkers.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.interfaces.ILogger;
import net.squaremarkers.core.json.JsonStorage;
import net.squaremarkers.core.registries.Layers;
import net.squaremarkers.fabric.compat.layers.OPACAreaMarkerLayer;
import net.squaremarkers.fabric.compat.OpacHandler;
import net.squaremarkers.fabric.listeners.UseItemOnListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused") // Called by fabric
public class SquareMarkers implements DedicatedServerModInitializer {

	private static final String LOG_PREFIX = "[SquareMarkers]";
	public static final Logger LOGGER = LoggerFactory.getLogger(SquareMarkers.class);
	private JsonStorage storage;
	private int ticks;

	public static boolean isOpacInstalled() {
		return FabricLoader.getInstance().isModLoaded("openpartiesandclaims");
	}

	public static boolean isOpacEnabled() {
		return isOpacInstalled() && FabricMarkersConfig.OPAC_MARKERS_ENABLED;
	}

    @Override
    public void onInitializeServer() {
		// register layers
		if (isOpacInstalled()) {
			Layers.register(OPACAreaMarkerLayer::new, unused -> isOpacEnabled());
		}
		// initialize core
	    storage = new JsonStorage("config/squaremarkers");
	    SquareMarkersCore.onInitialize(storage, new ILogger() {
		    @Override
		    public void debug(String message) {
			    LOGGER.debug("{} {}", LOG_PREFIX, message);
		    }

		    @Override
		    public void warn(String message) {
			    LOGGER.warn("{} {}", LOG_PREFIX, message);
		    }

		    @Override
		    public void warn(String message, Throwable throwable) {
			    LOGGER.warn("{} {}", LOG_PREFIX, message, throwable);
		    }
	    }, FabricMarkersConfig::reload);
        // register events
		ServerLifecycleEvents.AFTER_SAVE.register(
			(server, flush, force) -> storage.write()
		);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			ticks = 0;
			SquareMarkersCore.server(server);
			SquareMarkersCore.onStarted();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(unused -> {
			if (isOpacInstalled()) {
				OpacHandler.reset();
			}
			SquareMarkersCore.onDisable();
		});
		ServerLevelEvents.LOAD.register((server, level) ->
			xyz.jpenilla.squaremap.api.SquaremapProvider.get()
				.getWorldIfEnabled(xyz.jpenilla.squaremap.api.WorldIdentifier.parse(level.dimension().identifier().toString()))
				.ifPresent(SquareMarkersCore.squaremapHandler()::registerWorld)
		);
		ServerLevelEvents.UNLOAD.register((server, level) ->
			SquareMarkersCore.squaremapHandler().unregisterWorld(level.dimension().identifier().toString())
		);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++ticks % 20 == 0) {
				SquareMarkersCore.squaremapHandler().updateDynamicLayers();
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("squaremarkers")
				.then(Commands.literal("reload").executes(context -> reload(context.getSource())))
		));
	    BlockEvents.USE_ITEM_ON.register(new UseItemOnListener());
		String version = FabricLoader.getInstance().getModContainer("squaremarkers")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
		LOGGER.info("{} Mod initialized. Version: {}", LOG_PREFIX, version);
    }

	private static int reload(CommandSourceStack source) {
		if (source.getPlayer() != null
			&& !source.getPlayer().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
			source.sendFailure(Component.literal("You do not have permission to reload SquareMarkers."));
			return 0;
		}
		SquareMarkersCore.reloadMarkers();
		LOGGER.info("{} Config reloaded.", LOG_PREFIX);
		if (source.getPlayer() != null) {
			source.getPlayer().sendSystemMessage(Component.literal("[SquareMarkers] Config and markers reloaded."));
		}
		return 1;
	}

}
