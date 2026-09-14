package dev.fabriccontrollermcp;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Direct vanilla single-player world creation without navigating a visible menu. */
public final class SingleplayerWorldService {
    public String createJson(String name, String gameMode, String difficulty, Long seed, boolean structures, boolean bonusChest, boolean allowCommands, boolean hardcore) {
        if (name == null || name.isBlank() || name.length() > 255) return invalid("World name must contain 1 through 255 characters");
        GameType mode;
        Difficulty selectedDifficulty;
        try {
            mode = GameType.valueOf(gameMode == null ? "SURVIVAL" : gameMode.toUpperCase(java.util.Locale.ROOT));
            selectedDifficulty = Difficulty.valueOf(difficulty == null ? "NORMAL" : difficulty.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) { return invalid("gameMode and difficulty must be vanilla enum names"); }
        if (hardcore && mode != GameType.SURVIVAL) return invalid("Hardcore worlds must use SURVIVAL game mode");

        Minecraft client = Minecraft.getInstance();
        LevelSettings settings = new LevelSettings(name, mode,
                new LevelSettings.DifficultySettings(selectedDifficulty, hardcore, false), allowCommands, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(seed == null ? WorldOptions.randomSeed() : seed, structures, bonusChest);
        client.createWorldOpenFlows().createFreshLevel(name, settings, options, WorldPresets::createNormalWorldDimensions, null);
        return "{\"ok\":true,\"status\":\"loading\",\"worldName\":\"" + VanillaClientService.escape(name) + "\",\"gameMode\":\""
                + mode.name() + "\",\"difficulty\":\"" + selectedDifficulty.name() + "\",\"seed\":" + options.seed() + "}";
    }

    public String listJson() {
        try {
            java.util.List<String> worlds = new java.util.ArrayList<>();
            var source = Minecraft.getInstance().getLevelSource();
            for (net.minecraft.world.level.storage.LevelSummary summary : source.loadLevelSummaries(source.findLevelCandidates()).get()) {
                worlds.add("{\"id\":\"" + VanillaClientService.escape(summary.getLevelId()) + "\",\"name\":\""
                        + VanillaClientService.escape(summary.getLevelName()) + "\",\"lastPlayed\":" + summary.getLastPlayed() + "}");
            }
            return "{\"ok\":true,\"worlds\":[" + String.join(",", worlds) + "]}";
        } catch (Exception error) {
            return "{\"ok\":false,\"error\":{\"code\":\"world_list_unavailable\",\"message\":\"Vanilla could not read local world metadata\"}}";
        }
    }

    public String loadJson(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\") || id.contains("..")) return invalid("Invalid local world ID");
        Minecraft.getInstance().createWorldOpenFlows().openWorld(id, () -> { });
        return "{\"ok\":true,\"status\":\"loading\",\"worldId\":\"" + VanillaClientService.escape(id) + "\"}";
    }

    private static String invalid(String message) { return "{\"ok\":false,\"error\":{\"code\":\"invalid_world\",\"message\":\"" + VanillaClientService.escape(message) + "\"}}"; }
}
