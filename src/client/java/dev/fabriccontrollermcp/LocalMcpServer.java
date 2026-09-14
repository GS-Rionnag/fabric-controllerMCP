package dev.fabriccontrollermcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import net.minecraft.client.Minecraft;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.nio.file.Files;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loopback-only MCP endpoint owned by the Fabric client-mod lifecycle. */
public final class LocalMcpServer implements AutoCloseable {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 43125;
    private static final String ENDPOINT = "/mcp";

    private final MinecraftClientBridge clientBridge;
    private final Instant startedAt = Instant.now();
    private final AtomicBoolean running = new AtomicBoolean();

    private Tomcat tomcat;
    private McpSyncServer mcpServer;

    public LocalMcpServer(MinecraftClientBridge clientBridge) {
        this.clientBridge = clientBridge;
    }

    public synchronized void start() throws Exception {
        if (running.get()) return;

        HttpServletStreamableServerTransportProvider transport = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(ENDPOINT)
                .build();
        mcpServer = McpServer.sync(transport)
                .serverInfo(FabricControllerMcpClient.MOD_ID, FabricControllerMcpClient.VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .toolCall(McpSchema.Tool.builder("ping", emptyObjectSchema())
                                .description("Returns the local controller's liveness and protocol status.").build(),
                        (exchange, request) -> textResult("pong"))
                .toolCall(McpSchema.Tool.builder("server_status", emptyObjectSchema())
                                .description("Returns safe diagnostic status for the local Minecraft controller.").build(),
                        (exchange, request) -> textResult(statusJson()))
                .build();

        tomcat = new Tomcat();
        String baseDirectory = Files.createTempDirectory("fabric-controller-mcp-").toString();
        tomcat.setBaseDir(baseDirectory);
        tomcat.setHostname(HOST);
        tomcat.setPort(PORT);
        tomcat.getConnector();
        Context context = tomcat.addContext("", baseDirectory);
        addLoopbackOriginFilter(context);
        Tomcat.addServlet(context, "fabric-controller-mcp", transport);
        context.addServletMappingDecoded(ENDPOINT, "fabric-controller-mcp");
        tomcat.start();

        running.set(true);
        FabricControllerMcpClient.LOGGER.info("Local MCP server listening at http://{}:{}{}", HOST, PORT, ENDPOINT);
    }

    public boolean isRunning() { return running.get(); }

    public String endpoint() { return "http://" + HOST + ":" + PORT + ENDPOINT; }

    @Override
    public synchronized void close() {
        if (!running.compareAndSet(true, false)) return;
        try {
            mcpServer.close();
        } catch (Exception error) {
            FabricControllerMcpClient.LOGGER.warn("Unable to close MCP protocol server", error);
        }
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (Exception error) {
            FabricControllerMcpClient.LOGGER.warn("Unable to stop local MCP endpoint", error);
        }
    }

    private String statusJson() {
        try {
            boolean onClientThread = clientBridge.call(() -> Minecraft.getInstance().isSameThread());
            return "{\"status\":\"ok\",\"endpoint\":\"" + endpoint()
                    + "\",\"minecraftClientThread\":" + onClientThread + ",\"startedAt\":\"" + startedAt + "\"}";
        } catch (Exception error) {
            return "{\"status\":\"degraded\",\"endpoint\":\"" + endpoint()
                    + "\",\"reason\":\"Minecraft client thread unavailable\"}";
        }
    }

    private static McpSchema.CallToolResult textResult(String text) {
        return McpSchema.CallToolResult.builder().content(List.of(new McpSchema.TextContent(text))).build();
    }

    private static Map<String, Object> emptyObjectSchema() {
        return Map.of("type", "object", "additionalProperties", false);
    }

    private static void addLoopbackOriginFilter(Context context) {
        String filterName = "fabric-controller-mcp-loopback-origin";
        FilterDef filterDefinition = new FilterDef();
        filterDefinition.setFilterName(filterName);
        filterDefinition.setFilterClass(LoopbackOriginFilter.class.getName());
        filterDefinition.setFilter(new LoopbackOriginFilter());
        context.addFilterDef(filterDefinition);

        FilterMap filterMapping = new FilterMap();
        filterMapping.setFilterName(filterName);
        filterMapping.addURLPatternDecoded(ENDPOINT);
        filterMapping.setDispatcher("REQUEST");
        context.addFilterMapBefore(filterMapping);
    }
}
