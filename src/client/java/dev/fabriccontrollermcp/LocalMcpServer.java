package dev.fabriccontrollermcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Loopback-only MCP endpoint owned by the Fabric client-mod lifecycle. */
public final class LocalMcpServer implements AutoCloseable {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 43125;
    private static final String ENDPOINT = "/mcp";

    private final MinecraftClientBridge clientBridge;
    private final McpCommandService commands = new McpCommandService();
    private final AtomicBoolean running = new AtomicBoolean();
    private Tomcat tomcat;
    private McpSyncServer mcpServer;

    public LocalMcpServer(MinecraftClientBridge clientBridge) { this.clientBridge = clientBridge; }

    public synchronized void start() throws Exception {
        if (running.get()) return;
        HttpServletStreamableServerTransportProvider transport = HttpServletStreamableServerTransportProvider.builder().mcpEndpoint(ENDPOINT).build();
        mcpServer = McpServer.sync(transport)
                .serverInfo(FabricControllerMcpClient.MOD_ID, FabricControllerMcpClient.VERSION)
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .toolCall(McpSchema.Tool.builder("mcp", commandSchema())
                                .description("Minecraft's compact command tree. Call with no command or command help first; it returns state-aware categories and exact subcommands. Use direct settings commands rather than UI for vanilla settings.").build(),
                        (exchange, request) -> commandResult(request.arguments()))
                .build();

        tomcat = new Tomcat();
        String baseDirectory = Files.createTempDirectory("fabric-controller-mcp-").toString();
        tomcat.setBaseDir(baseDirectory);
        tomcat.setHostname(HOST);
        tomcat.setPort(PORT);
        tomcat.getConnector().setProperty("address", HOST);
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
        try { mcpServer.close(); } catch (Exception error) { FabricControllerMcpClient.LOGGER.warn("Unable to close MCP protocol server", error); }
        try { tomcat.stop(); tomcat.destroy(); } catch (Exception error) { FabricControllerMcpClient.LOGGER.warn("Unable to stop local MCP endpoint", error); }
    }

    private McpSchema.CallToolResult commandResult(Map<String, Object> arguments) {
        try {
            return textResult(clientBridge.call(() -> commands.execute(arguments)));
        } catch (Exception error) {
            FabricControllerMcpClient.LOGGER.warn("Unable to execute Minecraft command", error);
            return textResult("{\"ok\":false,\"error\":{\"code\":\"client_unavailable\",\"message\":\"Minecraft client thread is unavailable\"}}");
        }
    }

    private static McpSchema.CallToolResult textResult(String text) {
        return McpSchema.CallToolResult.builder().content(List.of(new McpSchema.TextContent(text))).build();
    }

    private static Map<String, Object> commandSchema() {
        return Map.of("type", "object", "additionalProperties", false,
                "properties", Map.of(
                        "command", Map.of("type", "string", "description", "A command returned by mcp help; omit it to list the root categories."),
                        "arguments", Map.of("type", "object", "description", "Arguments for that command. Start with help before invoking a new category.")));
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
