package dev.fabriccontrollermcp;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;

/**
 * Compact, state-aware command tree behind the single MCP tool.  This keeps
 * the host's tool prompt small while retaining explicit, discoverable actions.
 */
public final class McpCommandService {
    private final UiScreenService ui = new UiScreenService();
    private final SavedServerService servers = new SavedServerService();
    private final VanillaClientService vanilla = new VanillaClientService();
    private final SingleplayerWorldService worlds = new SingleplayerWorldService();
    private final RawInputService input = new RawInputService();

    public String execute(Map<String, Object> request) {
        String command = string(request, "command");
        Map<String, Object> arguments = object(request.get("arguments"));
        if (command == null || command.isBlank() || command.equals("help")) return help(arguments.get("path") instanceof String path ? path : "");
        if (!command.equals("settings.list") && command.startsWith("settings.") && command.endsWith(".list")) {
            return vanilla.listSettingsJson(command.substring("settings.".length(), command.length() - ".list".length()));
        }
        return switch (command) {
            case "state" -> state();
            case "settings.list" -> vanilla.listSettingsJson();
            case "settings.get" -> requiredString(arguments, "settingId", vanilla::getSettingJson);
            case "settings.update" -> updateSetting(arguments);
            case "controls.list" -> vanilla.listKeybindingsJson();
            case "controls.update" -> updateKeybinding(arguments);
            case "mainmenu.multiplayer.list_saved" -> servers.listSavedJson();
            case "mainmenu.multiplayer.save" -> servers.saveJson(string(arguments, "serverId"), string(arguments, "name"), string(arguments, "address"));
            case "mainmenu.multiplayer.remove_saved" -> requiredString(arguments, "serverId", servers::removeJson);
            case "mainmenu.multiplayer.join" -> servers.joinJson(string(arguments, "serverId"), string(arguments, "address"), string(arguments, "name"));
            case "mainmenu.singleplayer.list" -> worlds.listJson();
            case "mainmenu.singleplayer.create" -> createWorld(arguments);
            case "mainmenu.singleplayer.load" -> requiredString(arguments, "worldId", worlds::loadJson);
            case "ui.current_screen" -> ui.currentScreenJson();
            case "ui.inspect" -> ui.inspectElementsJson();
            case "ui.click", "ui.select" -> ui.clickJson(string(arguments, "elementId"), longValue(arguments, "screenStateVersion"));
            case "ui.focus" -> ui.focusJson(string(arguments, "elementId"), longValue(arguments, "screenStateVersion"));
            case "ui.type" -> ui.typeTextJson(string(arguments, "elementId"), string(arguments, "text"), Boolean.TRUE.equals(arguments.get("append")), longValue(arguments, "screenStateVersion"));
            case "ui.set_slider" -> slider(arguments);
            case "ui.choose" -> ui.chooseJson(string(arguments, "elementId"), Boolean.TRUE.equals(arguments.get("previous")), longValue(arguments, "screenStateVersion"));
            case "ui.toggle" -> ui.toggleJson(string(arguments, "elementId"), longValue(arguments, "screenStateVersion"));
            case "ui.scroll" -> scroll(arguments);
            case "input.key_down", "input.key_up", "input.key_tap" -> input.keyJson(command.substring("input.".length()), string(arguments, "key"), intValue(arguments, "modifiers"));
            case "input.text" -> input.textJson(string(arguments, "text"));
            case "input.mouse_move" -> input.mouseMoveJson(doubleValue(arguments, "x"), doubleValue(arguments, "y"));
            case "input.mouse_down", "input.mouse_up", "input.mouse_click" -> input.mouseJson(command.substring("input.".length()), intValue(arguments, "button"));
            case "input.scroll" -> input.scrollJson(doubleValue(arguments, "horizontalAmount"), doubleValue(arguments, "verticalAmount"));
            case "input.state" -> input.stateJson();
            default -> error("unknown_command", "Run mcp with command help, then request the relevant category.");
        };
    }

    private String help(String path) {
        boolean inGame = Minecraft.getInstance().level != null;
        return switch (path) {
            case "settings" -> "{\"ok\":true,\"path\":\"settings\",\"sections\":[\"fov\",\"online\",\"skin_customization\",\"music_and_sounds\",\"video\",\"controls\",\"language\",\"chat\",\"resource_packs\",\"accessibility\",\"telemetry\"],\"usage\":\"Call help again with a section path, then use its compact .list command. settings.get and settings.update work in every client state. Credits and attribution are intentionally excluded.\"}";
            case "settings.fov", "settings.online", "settings.skin_customization", "settings.music_and_sounds", "settings.video", "settings.chat", "settings.accessibility", "settings.telemetry" -> settingSectionHelp(path.substring("settings.".length()));
            case "settings.controls" -> "{\"ok\":true,\"path\":\"settings.controls\",\"commands\":[\"controls.list\",\"controls.update\"]}";
            case "settings.language", "settings.resource_packs" -> "{\"ok\":true,\"path\":\"" + path + "\",\"commands\":[\"ui.current_screen\",\"ui.inspect\"],\"note\":\"This vanilla section is currently screen-only; use UI inspection and interaction.\"}";
            case "ui" -> "{\"ok\":true,\"path\":\"ui\",\"commands\":[\"ui.current_screen\",\"ui.inspect\",\"ui.click\",\"ui.select\",\"ui.focus\",\"ui.type\",\"ui.set_slider\",\"ui.choose\",\"ui.toggle\",\"ui.scroll\"],\"note\":\"Available in both states. Use only for screen-only or custom UI; prefer direct settings and semantic commands.\"}";
            case "input" -> "{\"ok\":true,\"path\":\"input\",\"commands\":[\"input.key_down\",\"input.key_up\",\"input.key_tap\",\"input.text\",\"input.mouse_move\",\"input.mouse_down\",\"input.mouse_up\",\"input.mouse_click\",\"input.scroll\",\"input.state\"],\"note\":\"Available in both states. This uses vanilla's raw input callback path; prefer direct semantic commands where available. Key commands accept vanilla key.keyboard.* names (for example key.keyboard.left.shift or key.keyboard.left_shift). Mouse buttons are GLFW numbers 0 through 7.\"}";
            case "mainmenu" -> "{\"ok\":true,\"path\":\"mainmenu\",\"sections\":[\"singleplayer\",\"multiplayer\"],\"usage\":\"Call help with mainmenu.singleplayer or mainmenu.multiplayer.\"}";
            case "mainmenu.singleplayer" -> "{\"ok\":true,\"path\":\"mainmenu.singleplayer\",\"commands\":[\"mainmenu.singleplayer.list\",\"mainmenu.singleplayer.create\",\"mainmenu.singleplayer.load\"]}";
            case "mainmenu.multiplayer" -> "{\"ok\":true,\"path\":\"mainmenu.multiplayer\",\"commands\":[\"mainmenu.multiplayer.list_saved\",\"mainmenu.multiplayer.save\",\"mainmenu.multiplayer.remove_saved\",\"mainmenu.multiplayer.join\"]}";
            case "ingame" -> "{\"ok\":true,\"path\":\"ingame\",\"sections\":[\"settings\",\"ui\",\"input\",\"player (planned)\",\"world (planned)\",\"interaction (planned)\",\"navigation (planned)\"],\"note\":\"settings, ui, and input are available now; gameplay sections will be added as their phases ship.\"}";
            default -> "{\"ok\":true,\"command\":\"mcp\",\"state\":\"" + (inGame ? "ingame" : "mainmenu") + "\",\"commands\":[\"state\",\"help\"],\"sharedSections\":[\"settings\",\"ui\",\"input\"],\"stateSection\":\"" + (inGame ? "ingame" : "mainmenu") + "\",\"usage\":\"Call mcp with command help and arguments.path set to settings, ui, input, mainmenu, mainmenu.singleplayer, mainmenu.multiplayer, or ingame. Then call one listed command with its arguments.\"}";
        };
    }

    private static String state() { return "{\"ok\":true,\"state\":\"" + (Minecraft.getInstance().level == null ? "mainmenu" : "ingame") + "\"}"; }
    private static String settingSectionHelp(String section) { return "{\"ok\":true,\"path\":\"settings." + section + "\",\"commands\":[\"settings." + section + ".list\",\"settings.get\",\"settings.update\"],\"note\":\"Use the list result's setting IDs with settings.get or settings.update. sound.music is the direct music-volume setting.\"}"; }
    private String updateSetting(Map<String, Object> arguments) { String id = string(arguments, "settingId"); return id == null || !arguments.containsKey("value") ? error("invalid_arguments", "settings.update requires settingId and value") : vanilla.updateSettingJson(id, arguments.get("value")); }
    private String updateKeybinding(Map<String, Object> arguments) { return requiredString(arguments, "keybindingId", id -> { String key = string(arguments, "key"); return key == null ? error("invalid_arguments", "controls.update requires keybindingId and key") : vanilla.updateKeybindingJson(id, key); }); }
    private String createWorld(Map<String, Object> arguments) {
        SeedArgument seed = seed(arguments.get("seed"));
        if (!seed.valid()) return error("invalid_seed", "seed must be a signed 64-bit integer when supplied");
        return worlds.createJson(string(arguments, "name"), string(arguments, "gameMode"), string(arguments, "difficulty"), seed.value(), !Boolean.FALSE.equals(arguments.get("structures")), Boolean.TRUE.equals(arguments.get("bonusChest")), Boolean.TRUE.equals(arguments.get("allowCommands")), Boolean.TRUE.equals(arguments.get("hardcore")));
    }
    private String slider(Map<String, Object> arguments) { return arguments.get("value") instanceof Number value ? ui.setSliderJson(string(arguments, "elementId"), value.doubleValue(), longValue(arguments, "screenStateVersion")) : error("invalid_arguments", "ui.set_slider requires value"); }
    private String scroll(Map<String, Object> arguments) { return arguments.get("amount") instanceof Number amount ? ui.scrollJson(string(arguments, "elementId"), amount.doubleValue(), longValue(arguments, "screenStateVersion")) : error("invalid_arguments", "ui.scroll requires amount"); }
    private static String requiredString(Map<String, Object> arguments, String name, java.util.function.Function<String, String> action) { String value = string(arguments, name); return value == null ? error("invalid_arguments", name + " is required") : action.apply(value); }
    @SuppressWarnings("unchecked") private static Map<String, Object> object(Object value) { return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of(); }
    private static String string(Map<String, Object> arguments, String name) { return arguments.get(name) instanceof String value ? value : null; }
    private static Long longValue(Map<String, Object> arguments, String name) { return arguments.get(name) instanceof Number value ? value.longValue() : null; }
    private static Integer intValue(Map<String, Object> arguments, String name) { return arguments.get(name) instanceof Number value ? value.intValue() : null; }
    private static Double doubleValue(Map<String, Object> arguments, String name) { return arguments.get(name) instanceof Number value ? value.doubleValue() : null; }
    private static SeedArgument seed(Object value) {
        if (value == null) return new SeedArgument(true, null);
        if (value instanceof Number number) {
            try { return new SeedArgument(true, Long.parseLong(number.toString())); }
            catch (NumberFormatException ignored) { }
        }
        if (value instanceof String text) {
            try { return new SeedArgument(true, Long.parseLong(text)); }
            catch (NumberFormatException ignored) { }
        }
        return new SeedArgument(false, null);
    }
    private record SeedArgument(boolean valid, Long value) { }
    private static String error(String code, String message) { return "{\"ok\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + VanillaClientService.escape(message) + "\"}}"; }
}
