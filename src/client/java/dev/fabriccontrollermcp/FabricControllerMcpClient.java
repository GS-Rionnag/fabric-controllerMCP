package dev.fabriccontrollermcp;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only bootstrap and owner of the local MCP server lifecycle. */
public final class FabricControllerMcpClient implements ClientModInitializer {
    public static final String MOD_ID = "fabric_controller_mcp";
    public static final String VERSION = "0.1.0-SNAPSHOT";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private LocalMcpServer localMcpServer;

    @Override
    public void onInitializeClient() {
        try {
            localMcpServer = new LocalMcpServer(new MinecraftClientBridge());
            localMcpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(localMcpServer::close, "fabric-controller-mcp-shutdown"));
            LOGGER.info("Fabric Controller MCP initialized");
        } catch (Exception error) {
            LOGGER.error("Fabric Controller MCP failed to start its local MCP endpoint", error);
        }
    }
}
