package net.squaremarkers.fabric.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.squaremarkers.fabric.SquareMarkers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Checks once per server process without doing network work on the server thread. */
public final class ModrinthUpdateChecker {
    private static final String PROJECT_ID = "7SYL5SOe";
    private static final String PROJECT_URL = "https://modrinth.com/mod/" + PROJECT_ID;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(TIMEOUT)
        .build();
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private ModrinthUpdateChecker() {
    }

    public static void checkOnceAsync(String currentVersion, String minecraftVersion) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(() -> check(currentVersion, minecraftVersion),
            "squaremarkers-modrinth-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private static void check(String currentVersion, String minecraftVersion) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID
                + "/version?include_changelog=false"))
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .header("User-Agent", "SwordfishBE/SquareMarkers/" + currentVersion
                + " (https://github.com/SwordfishBE/SquareMarkers)")
            .GET()
            .build();
        try {
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                SquareMarkers.LOGGER.debug("{} Modrinth update check returned HTTP {}.",
                    SquareMarkers.LOG_PREFIX, response.statusCode());
                return;
            }
            findNewestCompatible(response.body(), currentVersion, minecraftVersion)
                .ifPresent(newVersion -> SquareMarkers.LOGGER.info(
                    "{} Update available: {} (current: {}). {}",
                    SquareMarkers.LOG_PREFIX, newVersion, currentVersion, PROJECT_URL));
        } catch (IOException | InterruptedException | JsonParseException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            SquareMarkers.LOGGER.debug("{} Modrinth update check failed.",
                SquareMarkers.LOG_PREFIX, exception);
        }
    }

    static Optional<String> findNewestCompatible(String body, String currentVersion,
        String minecraftVersion) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonArray()) {
            return Optional.empty();
        }
        Version current;
        try {
            current = Version.parse(currentVersion);
        } catch (VersionParsingException exception) {
            return Optional.empty();
        }
        Version newest = current;
        String newestNumber = null;
        JsonArray versions = root.getAsJsonArray();
        for (JsonElement element : versions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject candidate = element.getAsJsonObject();
            String number = string(candidate, "version_number");
            if (number == null || !"release".equals(string(candidate, "version_type"))
                || !contains(candidate, "loaders", "fabric")
                || !contains(candidate, "game_versions", minecraftVersion)) {
                continue;
            }
            String status = string(candidate, "status");
            if (status != null && !"listed".equals(status)) {
                continue;
            }
            try {
                Version parsed = Version.parse(number);
                if (parsed.compareTo(newest) > 0) {
                    newest = parsed;
                    newestNumber = number;
                }
            } catch (VersionParsingException ignored) {
                // A non-comparable version must not trigger a false update notice.
            }
        }
        return Optional.ofNullable(newestNumber);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString() : null;
    }

    private static boolean contains(JsonObject object, String key, String wanted) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            return false;
        }
        for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive() && wanted.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }
}
