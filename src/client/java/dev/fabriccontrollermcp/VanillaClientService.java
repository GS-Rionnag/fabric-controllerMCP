package dev.fabriccontrollermcp;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.sounds.SoundSource;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Direct, screen-independent access to vanilla client preferences and controls. */
public final class VanillaClientService {
    public String listSettingsJson() {
        return listSettingsJson(null);
    }

    /** Lists only the requested vanilla Options-screen section when supplied. */
    public String listSettingsJson(String section) {
        Minecraft client = Minecraft.getInstance();
        List<String> settings = new ArrayList<>();
        for (Method method : client.options.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || !OptionInstance.class.isAssignableFrom(method.getReturnType())
                    || !Modifier.isPublic(method.getModifiers())) continue;
            try {
                OptionInstance<?> option = (OptionInstance<?>) method.invoke(client.options);
                if (belongsToSection(method.getName(), section)) settings.add(settingJson(method.getName(), option.get()));
            } catch (ReflectiveOperationException ignored) {
                // A future vanilla option may be unavailable in a particular client state.
            }
        }
        // Vanilla exposes per-category volume through a parameterized accessor,
        // so it is not covered by the zero-argument OptionInstance scan above.
        for (SoundSource source : SoundSource.values()) {
            OptionInstance<Double> option = client.options.getSoundSourceOptionInstance(source);
            if (belongsToSection(soundId(source), section)) settings.add(settingJson(soundId(source), option.get()));
        }
        settings.sort(Comparator.naturalOrder());
        return "{\"ok\":true," + (section == null ? "" : "\"section\":\"" + escape(section) + "\",") + "\"settings\":[" + String.join(",", settings) + "]}";
    }

    public String getSettingJson(String id) {
        OptionInstance<?> option = findOption(id);
        return option == null ? missing("setting", id) : "{\"ok\":true,\"setting\":" + settingJson(id, option.get()) + "}";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public String updateSettingJson(String id, Object requestedValue) {
        OptionInstance option = findOption(id);
        if (option == null) return missing("setting", id);
        Object current = option.get();
        Object parsed = coerce(requestedValue, current);
        if (parsed == null) return "{\"ok\":false,\"error\":{\"code\":\"invalid_value\",\"message\":\"Value does not match the vanilla setting type\"}}";
        try {
            option.set(parsed); // Runs vanilla's live update listener (sound, graphics, etc.).
            Minecraft.getInstance().options.save();
            return "{\"ok\":true,\"updated\":" + settingJson(id, option.get()) + "}";
        } catch (RuntimeException error) {
            return "{\"ok\":false,\"error\":{\"code\":\"rejected_by_vanilla\",\"message\":\"Vanilla rejected this setting value\"}}";
        }
    }

    public String listKeybindingsJson() {
        List<String> bindings = new ArrayList<>();
        for (KeyMapping binding : Minecraft.getInstance().options.keyMappings) bindings.add(keyJson(binding));
        return "{\"ok\":true,\"keybindings\":[" + String.join(",", bindings) + "]}";
    }

    public String updateKeybindingJson(String id, String key) {
        KeyMapping binding = findKeybinding(id);
        if (binding == null) return missing("keybinding", id);
        InputConstants.Key parsed = InputConstants.getKey(key);
        if (parsed == null || parsed == InputConstants.UNKNOWN) {
            return "{\"ok\":false,\"error\":{\"code\":\"invalid_key\",\"message\":\"Use a vanilla key name such as key.keyboard.g or key.mouse.left\"}}";
        }
        binding.setKey(parsed);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        return "{\"ok\":true,\"keybinding\":" + keyJson(binding) + "}";
    }

    private static OptionInstance<?> findOption(String id) {
        SoundSource soundSource = soundSource(id);
        if (soundSource != null) return Minecraft.getInstance().options.getSoundSourceOptionInstance(soundSource);
        try {
            Method method = Minecraft.getInstance().options.getClass().getMethod(id);
            if (method.getParameterCount() == 0 && OptionInstance.class.isAssignableFrom(method.getReturnType())) {
                return (OptionInstance<?>) method.invoke(Minecraft.getInstance().options);
            }
        } catch (ReflectiveOperationException ignored) { }
        return null;
    }

    private static String soundId(SoundSource source) { return "sound." + source.getName(); }

    private static SoundSource soundSource(String id) {
        if (id == null || !id.startsWith("sound.")) return null;
        String requested = id.substring("sound.".length());
        for (SoundSource source : SoundSource.values()) if (source.getName().equals(requested)) return source;
        return null;
    }

    private static boolean belongsToSection(String id, String section) {
        if (section == null || section.isBlank()) return true;
        return switch (section) {
            case "fov" -> id.equals("fov") || id.equals("fovEffectScale");
            case "online" -> id.equals("realmsNotifications") || id.equals("allowServerListing") || id.equals("sharePresence");
            case "skin_customization" -> id.equals("mainHand");
            case "music_and_sounds" -> id.startsWith("sound.") || id.equals("showSubtitles") || id.equals("directionalAudio");
            case "video" -> !(id.startsWith("chat") || id.startsWith("sound.") || id.equals("showSubtitles") || id.equals("directionalAudio")
                    || id.equals("mainHand") || id.equals("highContrast") || id.equals("highContrastBlockOutline") || id.equals("narrator")
                    || id.equals("narratorHotkey") || id.equals("screenEffectScale") || id.equals("darknessEffectScale") || id.equals("telemetryOptInExtra"));
            case "chat" -> id.startsWith("chat") || id.equals("backgroundForChatOnly") || id.equals("textBackgroundOpacity")
                    || id.equals("hideMatchedNames") || id.equals("onlyShowSecureChat") || id.equals("saveChatDrafts");
            case "accessibility" -> id.equals("highContrast") || id.equals("highContrastBlockOutline") || id.equals("narrator")
                    || id.equals("narratorHotkey") || id.equals("screenEffectScale") || id.equals("darknessEffectScale");
            case "telemetry" -> id.equals("telemetryOptInExtra");
            case "language", "resource_packs", "controls" -> false;
            default -> false;
        };
    }

    private static KeyMapping findKeybinding(String id) {
        for (KeyMapping binding : Minecraft.getInstance().options.keyMappings) if (binding.getName().equals(id)) return binding;
        return null;
    }

    private static Object coerce(Object requested, Object current) {
        if (current instanceof Boolean) return requested instanceof Boolean ? requested : null;
        if (current instanceof Integer) return requested instanceof Number number ? number.intValue() : null;
        if (current instanceof Double) return requested instanceof Number number ? number.doubleValue() : null;
        if (current instanceof String) return requested instanceof String ? requested : null;
        if (current instanceof Enum<?> value && requested instanceof String name) {
            try { return Enum.valueOf((Class) value.getDeclaringClass(), name.toUpperCase(java.util.Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

    private static String settingJson(String id, Object value) {
        return "{\"id\":\"" + escape(id) + "\",\"value\":" + valueJson(value)
                + ",\"valueType\":\"" + valueType(value) + "\"}";
    }

    private static String keyJson(KeyMapping binding) {
        return "{\"id\":\"" + escape(binding.getName()) + "\",\"key\":\""
                + escape(binding.saveString()) + "\",\"defaultKey\":\"" + escape(binding.getDefaultKey().getName())
                + "\",\"category\":\"" + escape(binding.getCategory().id().toString()) + "\"}";
    }

    private static String valueType(Object value) {
        if (value instanceof Boolean) return "boolean";
        if (value instanceof Integer) return "integer";
        if (value instanceof Number) return "number";
        if (value instanceof Enum<?>) return "enum";
        return "string";
    }

    private static String valueJson(Object value) {
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        return "\"" + escape(value instanceof Enum<?> e ? e.name() : String.valueOf(value)) + "\"";
    }

    static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private static String missing(String type, String id) { return "{\"ok\":false,\"error\":{\"code\":\"unknown_" + type + "\",\"message\":\"No vanilla " + type + " named " + escape(id) + "\"}}"; }
}
