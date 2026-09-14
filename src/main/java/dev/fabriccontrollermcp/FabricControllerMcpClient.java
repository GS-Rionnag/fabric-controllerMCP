package dev.fabriccontrollermcp;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only bootstrap for the controller. Game-affecting work must later be
 * dispatched through an explicit Minecraft-thread bridge rather than executed
 * from MCP transport threads.
 */
public final class FabricControllerMcpClient implements ClientModInitializer {
    public static final String MOD_ID = "fabric_controller_mcp";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Fabric Controller MCP initialized");
    }
}
