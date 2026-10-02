package net.squaremarkers.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.permissions.Permissions;
import net.squaremarkers.core.MarkerVisibility;
import net.squaremarkers.core.MarkersConfig;
import net.squaremarkers.core.SquareMarkersCore;
import net.squaremarkers.core.layers.primitive.MarkerLayer;
import net.squaremarkers.fabric.compat.warps.WarpHandler;
import net.squaremarkers.fabric.compat.waystones.WaystonesHandler;

import java.util.Set;
import java.util.TreeSet;

/** Admin diagnostics and global marker visibility; never modifies source objects. */
final class MarkerCommands {
    private MarkerCommands() {}

    static boolean allowed(CommandSourceStack source) {
        return source.getPlayer() == null || source.getPlayer().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    static LiteralArgumentBuilder<CommandSourceStack> statusCommand() {
        return Commands.literal("status").executes(context -> status(context.getSource(), false))
            .then(Commands.literal("details").executes(context -> status(context.getSource(), true)));
    }

    static LiteralArgumentBuilder<CommandSourceStack> markerCommand() {
        return Commands.literal("marker")
            .then(Commands.literal("list").executes(context -> status(context.getSource(), true))
                .then(worldArgument().then(layerArgument()
                    .executes(context -> list(context, 1))
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> list(context, IntegerArgumentType.getInteger(context, "page")))))))
            .then(visibilityCommand("hide", true))
            .then(visibilityCommand("show", false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> visibilityCommand(String name, boolean hidden) {
        return Commands.literal(name).then(worldArgument().then(layerArgument()
            .then(Commands.argument("id", StringArgumentType.string())
                .suggests((context, builder) -> {
                    identities(argument(context, "world"), argument(context, "layer")).stream()
                        .filter(id -> id.toLowerCase(java.util.Locale.ROOT).startsWith(
                            unquote(builder.getRemaining()).toLowerCase(java.util.Locale.ROOT)))
                        .limit(100).forEach(id -> builder.suggest(StringArgumentType.escapeIfRequired(id)));
                    return builder.buildFuture();
                }).executes(context -> visibility(context, hidden)))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> worldArgument() {
        return Commands.argument("world", StringArgumentType.string()).suggests((context, builder) -> {
            Set<String> worlds = new TreeSet<>();
            SquareMarkersCore.squaremapHandler().activeLayers().forEach(layer -> worlds.add(layer.worldIdentifier));
            SquareMarkersCore.visibility().exclusions().forEach(identity -> worlds.add(identity.world()));
            worlds.stream().filter(world -> world.startsWith(unquote(builder.getRemaining())))
                .limit(100).forEach(world -> builder.suggest(StringArgumentType.escapeIfRequired(world)));
            return builder.buildFuture();
        });
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> layerArgument() {
        return Commands.argument("layer", StringArgumentType.string()).suggests((context, builder) -> {
            String world = argument(context, "world");
            Set<String> keys = new TreeSet<>();
            SquareMarkersCore.squaremapHandler().activeLayers().stream().filter(layer -> layer.worldIdentifier.equals(world))
                .forEach(layer -> keys.add(layer.getKey()));
            SquareMarkersCore.visibility().exclusions().stream().filter(identity -> identity.world().equals(world))
                .forEach(identity -> keys.add(identity.layer()));
            keys.stream().filter(key -> key.startsWith(unquote(builder.getRemaining())))
                .limit(100).forEach(key -> builder.suggest(StringArgumentType.escapeIfRequired(key)));
            return builder.buildFuture();
        });
    }

    private static String unquote(String value) { return value.startsWith("\"") ? value.substring(1) : value; }
    private static String argument(CommandContext<CommandSourceStack> context, String name) {
        return StringArgumentType.getString(context, name);
    }
    private static Set<String> identities(String world, String key) {
        Set<String> ids = new TreeSet<>();
        MarkerLayer<?> layer = SquareMarkersCore.squaremapHandler().findLayer(world, key);
        if (layer != null) ids.addAll(layer.markerIdentities());
        SquareMarkersCore.visibility().exclusions().stream()
            .filter(identity -> identity.world().equals(world) && identity.layer().equals(key))
            .forEach(identity -> ids.add(identity.marker()));
        return ids;
    }

    private static int list(CommandContext<CommandSourceStack> context, int page) {
        String world = argument(context, "world"), key = argument(context, "layer");
        var ids = identities(world, key).stream().toList();
        int pages = Math.max(1, (ids.size() + 9) / 10);
        if (page > pages) return failure(context.getSource(), "Page must be between 1 and " + pages + ".");
        message(context.getSource(), world + " / " + key + ": " + ids.size() + " identities (page " + page + "/" + pages + ").");
        MarkerLayer<?> layer = SquareMarkersCore.squaremapHandler().findLayer(world, key);
        Set<String> present = layer == null ? Set.of() : layer.markerIdentities();
        ids.stream().skip((page - 1L) * 10).limit(10).forEach(id -> message(context.getSource(),
            ((layer != null ? layer.isHidden(id) : SquareMarkersCore.visibility().hidden(world, key, id)) ? "[hidden] " : "[visible] ")
                + StringArgumentType.escapeIfRequired(id)
                + (!present.contains(id) ? " (source currently absent)" : "")
                + (layer != null && present.contains(id) ? " | " + layer.markerSummary(id) : "")));
        return 1;
    }

    private static int visibility(CommandContext<CommandSourceStack> context, boolean hidden) {
        String world = argument(context, "world"), key = argument(context, "layer"), id = argument(context, "id");
        var layer = SquareMarkersCore.squaremapHandler().findLayer(world, key);
        if (!identities(world, key).contains(id)) return failure(context.getSource(), "Unknown marker. Use /squaremarkers marker list first.");
        if (id.isBlank() || id.length() > 4096) return failure(context.getSource(), "This marker identity is too long to persist.");
        if (!SquareMarkersCore.visibility().setHidden(new MarkerVisibility.Identity(world, key, id), hidden)) {
            return failure(context.getSource(), "Could not save visibility (storage error or exclusion limit reached).");
        }
        if (layer != null) layer.refreshVisibility(id);
        if (!hidden && layer != null && layer.isHidden(id)) {
            message(context.getSource(), "Individual exclusion cleared, but this marker remains hidden by an owner-level exclusion. Show that owner entry first.");
            return 1;
        }
        message(context.getSource(), "Marker " + (hidden ? "hidden from" : "shown on") + " squaremap: " + id
            + ". The web map updates on squaremap's next update.");
        return 1;
    }

    private static int status(CommandSourceStack source, boolean detailed) {
        String version = FabricLoader.getInstance().getModContainer("squaremarkers")
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("unknown");
        var layers = SquareMarkersCore.squaremapHandler().activeLayers();
        long worlds = layers.stream().map(layer -> layer.worldIdentifier).distinct().count();
        int total = layers.stream().mapToInt(MarkerLayer::logicalCount).sum();
        int visible = layers.stream().mapToInt(MarkerLayer::visibleCount).sum();
        statusMessage(source, Component.literal("Status ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
            .append(colored("v" + version, ChatFormatting.WHITE)));
        statusMessage(source, colored("Worlds: ", ChatFormatting.GRAY).append(colored(Long.toString(worlds), ChatFormatting.WHITE))
            .append(colored(" | Layers: ", ChatFormatting.GRAY)).append(colored(Integer.toString(layers.size()), ChatFormatting.WHITE)));
        statusMessage(source, colored("Markers: ", ChatFormatting.GRAY)
            .append(colored(visible + " visible", ChatFormatting.GREEN))
            .append(colored(" | ", ChatFormatting.DARK_GRAY)).append(colored((total - visible) + " hidden", ChatFormatting.YELLOW)));
        statusMessage(source, colored("Saved exclusions: ", ChatFormatting.GRAY)
            .append(colored(Integer.toString(SquareMarkersCore.visibility().exclusions().size()), ChatFormatting.YELLOW)));
        statusMessage(source, colored("Deaths: ", ChatFormatting.GRAY)
            .append(colored(MarkersConfig.DEATH_MARKERS_ENABLED ? "enabled" : "disabled",
                MarkersConfig.DEATH_MARKERS_ENABLED ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
            .append(colored(" | Lifetime: " + MarkersConfig.DEATH_MARKERS_LIFETIME + "s | Zone: "
                + MarkersConfig.DEATH_MARKERS_TIMEZONE.getId(), ChatFormatting.GRAY)));
        int installed = SquareMarkers.isOpacInstalled() ? 1 : 0;
        int enabled = SquareMarkers.isOpacEnabled() ? 1 : 0;
        for (var warp : WarpHandler.Source.values()) {
            if (warp.installed()) installed++;
            if (warp.enabled()) enabled++;
        }
        if (WaystonesHandler.installed()) installed++;
        if (WaystonesHandler.enabled()) enabled++;
        statusMessage(source, colored("Integrations: ", ChatFormatting.GRAY)
            .append(colored(enabled + " enabled", ChatFormatting.GREEN))
            .append(colored(" | ", ChatFormatting.DARK_GRAY)).append(colored((installed - enabled) + " disabled", ChatFormatting.YELLOW))
            .append(colored(" | " + (WarpHandler.Source.values().length + 2 - installed) + " not installed", ChatFormatting.DARK_GRAY)));
        if (!detailed) {
            statusMessage(source, colored("Details: /squaremarkers status details", ChatFormatting.AQUA));
            return 1;
        }
        statusMessage(source, colored("Integration settings", ChatFormatting.AQUA));
        integrationMessage(source, "Open Parties and Claims", SquareMarkers.isOpacInstalled(), SquareMarkers.isOpacEnabled());
        for (var warp : WarpHandler.Source.values()) integrationMessage(source, warp.label(), warp.installed(), warp.enabled());
        integrationMessage(source, "Waystones", WaystonesHandler.installed(), WaystonesHandler.enabled());
        statusMessage(source, colored("Layer entries (OPAC counts polygon fragments)", ChatFormatting.AQUA));
        layers.stream().limit(30).forEach(layer -> statusMessage(source,
            colored(layer.worldIdentifier + " / ", ChatFormatting.DARK_GRAY)
                .append(colored(layer.getKey() + ": ", ChatFormatting.WHITE))
                .append(colored(layer.visibleCount() + " visible", ChatFormatting.GREEN))
                .append(colored(" | " + (layer.logicalCount() - layer.visibleCount()) + " hidden", ChatFormatting.YELLOW))));
        if (layers.size() > 30) statusMessage(source, colored("Additional layers omitted; use marker list with world/layer arguments.", ChatFormatting.GRAY));
        return 1;
    }
    static MutableComponent integrationStatus(boolean installed, boolean enabled) {
        return colored(!installed ? "not installed" : enabled ? "enabled" : "disabled",
            !installed ? ChatFormatting.DARK_GRAY : enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
    }
    private static void integrationMessage(CommandSourceStack source, String label, boolean installed, boolean enabled) {
        statusMessage(source, colored(label + ": ", ChatFormatting.GRAY).append(integrationStatus(installed, enabled)));
    }
    private static MutableComponent colored(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }
    private static void statusMessage(CommandSourceStack source, Component text) {
        source.sendSuccess(() -> colored("[SquareMarkers] ", ChatFormatting.AQUA).append(text), false);
    }
    private static void message(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal("[SquareMarkers] " + text), false);
    }
    private static int failure(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal("[SquareMarkers] " + text));
        return 0;
    }
}
