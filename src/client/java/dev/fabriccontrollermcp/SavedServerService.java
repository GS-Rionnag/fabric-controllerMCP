package dev.fabriccontrollermcp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.gui.screens.ConnectScreen;

import java.util.ArrayList;
import java.util.List;

/** High-level read-only access to the local multiplayer server list. */
public final class SavedServerService {
    public String listSavedJson() {
        ServerList servers = new ServerList(Minecraft.getInstance());
        servers.load();
        List<String> entries = new ArrayList<>();
        for (int index = 0; index < servers.size(); index++) {
            ServerData server = servers.get(index);
            entries.add("{\"id\":\"saved-server:" + index + "\",\"name\":\"" + escape(server.name)
                    + "\",\"address\":\"" + escape(server.ip) + "\"}");
        }
        return "{\"ok\":true,\"servers\":[" + String.join(",", entries) + "]}";
    }

    public String saveJson(String id, String name, String address) {
        if (name.isBlank() || !ServerAddress.isValidAddress(address)) return invalidServer();
        ServerList servers = servers();
        int index = parseId(id, servers.size());
        ServerData data = new ServerData(name, address, ServerData.Type.OTHER);
        if (index < 0) servers.add(data, false); else servers.replace(index, data);
        servers.save();
        return "{\"ok\":true,\"server\":{\"id\":\"saved-server:" + (index < 0 ? servers.size() - 1 : index)
                + "\",\"name\":\"" + escape(name) + "\",\"address\":\"" + escape(address) + "\"}}";
    }

    public String removeJson(String id) {
        ServerList servers = servers();
        int index = parseId(id, servers.size());
        if (index < 0) return unknown(id);
        servers.remove(servers.get(index));
        servers.save();
        return "{\"ok\":true,\"removed\":\"" + escape(id) + "\"}";
    }

    public String joinJson(String id, String address, String name) {
        ServerData data;
        if (id != null) {
            ServerList servers = servers();
            int index = parseId(id, servers.size());
            if (index < 0) return unknown(id);
            data = servers.get(index);
        } else {
            if (address == null || !ServerAddress.isValidAddress(address)) return invalidServer();
            data = new ServerData(name == null || name.isBlank() ? address : name, address, ServerData.Type.OTHER);
        }
        Minecraft client = Minecraft.getInstance();
        ConnectScreen.startConnecting(null, client, ServerAddress.parseString(data.ip), data, false, null);
        return "{\"ok\":true,\"status\":\"connecting\",\"name\":\"" + escape(data.name) + "\",\"address\":\"" + escape(data.ip) + "\"}";
    }

    private static ServerList servers() { ServerList result = new ServerList(Minecraft.getInstance()); result.load(); return result; }
    private static int parseId(String id, int size) {
        if (id == null || !id.startsWith("saved-server:")) return -1;
        try { int value = Integer.parseInt(id.substring("saved-server:".length())); return value >= 0 && value < size ? value : -1; }
        catch (NumberFormatException ignored) { return -1; }
    }
    private static String unknown(String id) { return "{\"ok\":false,\"error\":{\"code\":\"unknown_server\",\"message\":\"No saved server named " + escape(String.valueOf(id)) + "\"}}"; }
    private static String invalidServer() { return "{\"ok\":false,\"error\":{\"code\":\"invalid_server\",\"message\":\"Provide a non-empty name and a valid server address\"}}"; }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
