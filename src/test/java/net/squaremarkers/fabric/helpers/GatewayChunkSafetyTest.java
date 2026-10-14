package net.squaremarkers.fabric.helpers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Source-level guard against reintroducing the watchdog's blocking world lookup. */
class GatewayChunkSafetyTest {
    private String source(String relative) throws IOException {
        return Files.readString(Path.of("src/main/java/net/squaremarkers/fabric", relative));
    }

    @Test void gatewayCaptureNeverRequestsOrInstantiatesChunksOrBlockEntities() throws IOException {
        String helper = source("helpers/GatewayHelper.java");
        assertFalse(helper.contains(".getBlockEntity("));
        assertFalse(helper.contains(".getChunk("));
        assertFalse(helper.contains(".getChunkAt("));
        assertTrue(helper.contains(".getChunkNow("));
        assertTrue(helper.contains("chunk.getBlockEntities().get(pos)"));
    }

    @Test void chunkLoadAndMixinHooksUseTheirExistingGatewayInstance() throws IOException {
        String helper = source("helpers/GatewayHelper.java");
        String callback = helper.substring(helper.indexOf("public static void onChunkLoad"),
            helper.indexOf("public static void refreshLoaded"));
        assertTrue(callback.contains("capture(layer, gateway)"));
        assertFalse(callback.contains("getChunkSource"));
        assertFalse(callback.contains("refresh(level, entity.getBlockPos())"));
        String mixin = source("mixin/EndGatewayBlockEntityMixin.java");
        assertTrue(mixin.contains("GatewayHelper.refresh(level, gateway)"));
        assertTrue(mixin.contains("GatewayHelper.refresh(level, (TheEndGatewayBlockEntity) (Object) this)"));
        assertFalse(mixin.contains("getBlockPos()"));
    }
}
