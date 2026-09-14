package dev.fabriccontrollermcp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.IdentityHashMap;

/**
 * Version-anchor implementation of the generic UI inspection primitives.
 * All methods must be invoked through {@link MinecraftClientBridge}.
 */
public final class UiScreenService {
    private static final int MAX_INSPECTED_ELEMENTS = 512;
    private long stateVersion;
    private String lastSignature = "";

    public String currentScreenJson() {
        Screen screen = Minecraft.getInstance().gui.screen();
        return screenJson(screen, currentStateVersion(screen));
    }

    public String inspectElementsJson() {
        Screen screen = Minecraft.getInstance().gui.screen();
        long version = currentStateVersion(screen);
        if (screen == null) {
            return "{\"ok\":true,\"screen\":{\"id\":\"minecraft:none\",\"stateVersion\":" + version
                    + "},\"elements\":[]}";
        }

        List<String> elements = new ArrayList<>();
        appendElements(elements, screen.children(), "element", new IdentityHashMap<>());
        return "{\"ok\":true,\"screen\":{\"id\":\"" + escape(screenId(screen))
                + "\",\"stateVersion\":" + version + "},\"elements\":["
                + String.join(",", elements) + "],\"truncated\":" + (elements.size() >= MAX_INSPECTED_ELEMENTS) + "}";
    }

    public String clickJson(String elementId, long expectedStateVersion) {
        Screen screen = Minecraft.getInstance().gui.screen();
        long actualStateVersion = currentStateVersion(screen);
        if (expectedStateVersion != actualStateVersion) {
            return "{\"ok\":false,\"error\":{\"code\":\"stale_screen\",\"message\":\"The screen changed before the action ran\",\"expectedStateVersion\":"
                    + expectedStateVersion + ",\"actualStateVersion\":" + actualStateVersion + "}}";
        }
        if (screen == null || !elementId.startsWith("element:")) {
            return invalidTargetJson(elementId);
        }
        GuiEventListener target = element(screen, elementId);
        if (target == null) return invalidTargetJson(elementId);
        if (target instanceof AbstractWidget widget && (!widget.visible || !widget.active)) {
            return "{\"ok\":false,\"error\":{\"code\":\"disabled_element\",\"message\":\"The target is not currently enabled and visible\",\"elementId\":\""
                    + escape(elementId) + "\"}}";
        }
        boolean consumed;
        if (target instanceof AbstractButton button) {
            button.onPress(input(0));
            consumed = true;
        } else {
            double x = target.getRectangle().left() + target.getRectangle().width() / 2.0;
            double y = target.getRectangle().top() + target.getRectangle().height() / 2.0;
            consumed = target.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        }
        return "{\"ok\":true,\"action\":\"click\",\"elementId\":\"" + escape(elementId)
                + "\",\"result\":\"" + (consumed ? "activated" : "not_consumed") + "\",\"screenStateVersion\":" + currentStateVersion(Minecraft.getInstance().gui.screen()) + "}";
    }

    public String focusJson(String elementId, long expectedStateVersion) {
        Screen screen = matchingScreen(expectedStateVersion);
        if (screen == null) return staleOrInvalidScreenJson(expectedStateVersion);
        GuiEventListener element = element(screen, elementId);
        if (element == null) return invalidTargetJson(elementId);
        screen.setFocused(element);
        element.setFocused(true);
        return actionJson("focus", elementId);
    }

    public String typeTextJson(String elementId, String text, boolean append, long expectedStateVersion) {
        Screen screen = matchingScreen(expectedStateVersion);
        if (screen == null) return staleOrInvalidScreenJson(expectedStateVersion);
        GuiEventListener element = element(screen, elementId);
        if (!(element instanceof EditBox editBox)) {
            return unsupportedJson(elementId, "Only standard Minecraft text fields support typing");
        }
        screen.setFocused(editBox);
        editBox.setFocused(true);
        editBox.setValue(append ? editBox.getValue() + text : text);
        return actionJson("type_text", elementId);
    }

    public String setSliderJson(String elementId, double value, long expectedStateVersion) {
        Screen screen = matchingScreen(expectedStateVersion);
        if (screen == null) return staleOrInvalidScreenJson(expectedStateVersion);
        GuiEventListener element = element(screen, elementId);
        if (!(element instanceof AbstractSliderButton slider)) {
            return unsupportedJson(elementId, "Only standard Minecraft sliders support set_slider");
        }
        if (value < 0.0 || value > 1.0 || Double.isNaN(value)) {
            return "{\"ok\":false,\"error\":{\"code\":\"invalid_value\",\"message\":\"Slider value must be between 0.0 and 1.0\"}}";
        }
        double x = slider.getX() + 4 + value * Math.max(1, slider.getWidth() - 8);
        double y = slider.getY() + slider.getHeight() / 2.0;
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        slider.mouseClicked(event, false);
        slider.mouseReleased(event);
        return actionJson("set_slider", elementId);
    }

    public String chooseJson(String elementId, boolean previous, long expectedStateVersion) {
        Screen screen = matchingScreen(expectedStateVersion);
        if (screen == null) return staleOrInvalidScreenJson(expectedStateVersion);
        GuiEventListener element = element(screen, elementId);
        if (!(element instanceof CycleButton<?> cycleButton)) {
            return unsupportedJson(elementId, "Only standard Minecraft cycle controls support choose");
        }
        cycleButton.onPress(input(previous ? 1 : 0));
        return actionJson("choose", elementId);
    }

    public String toggleJson(String elementId, long expectedStateVersion) {
        return chooseJson(elementId, false, expectedStateVersion);
    }

    public String scrollJson(String elementId, double amount, long expectedStateVersion) {
        Screen screen = matchingScreen(expectedStateVersion);
        if (screen == null) return staleOrInvalidScreenJson(expectedStateVersion);
        GuiEventListener element = element(screen, elementId);
        if (element == null) return invalidTargetJson(elementId);
        boolean consumed = element.mouseScrolled(0, 0, 0, amount);
        return "{\"ok\":true,\"action\":\"scroll\",\"elementId\":\"" + escape(elementId)
                + "\",\"result\":\"" + (consumed ? "consumed" : "not_consumed") + "\",\"screenStateVersion\":"
                + currentStateVersion(Minecraft.getInstance().gui.screen()) + "}";
    }

    public String keyPressJson(String keyName, long expectedStateVersion) {
        Screen screen = matchingScreen(expectedStateVersion);
        if (screen == null) return staleOrInvalidScreenJson(expectedStateVersion);
        Integer key = switch (keyName) {
            case "ESCAPE" -> 256; case "ENTER" -> 257; case "TAB" -> 258; case "BACKSPACE" -> 259;
            case "LEFT" -> 263; case "RIGHT" -> 262; case "UP" -> 265; case "DOWN" -> 264;
            default -> null;
        };
        if (key == null) return "{\"ok\":false,\"error\":{\"code\":\"unsupported_key\",\"message\":\"Only Escape, Enter, Tab, Backspace, and arrow keys are supported\"}}";
        boolean consumed = screen.keyPressed(new KeyEvent(key, 0, 0));
        return "{\"ok\":true,\"action\":\"key_press\",\"key\":\"" + keyName + "\",\"result\":\""
                + (consumed ? "consumed" : "not_consumed") + "\",\"screenStateVersion\":" + currentStateVersion(Minecraft.getInstance().gui.screen()) + "}";
    }

    private long currentStateVersion(Screen screen) {
        String signature = signature(screen);
        if (!signature.equals(lastSignature)) {
            lastSignature = signature;
            stateVersion++;
        }
        return stateVersion;
    }

    private static String screenJson(Screen screen, long version) {
        if (screen == null) {
            return "{\"ok\":true,\"screen\":{\"id\":\"minecraft:none\",\"className\":null,\"title\":null,\"stateVersion\":" + version
                    + ",\"capabilities\":[\"inspect_elements\"]}}";
        }
        return "{\"ok\":true,\"screen\":{\"id\":\"" + escape(screenId(screen)) + "\",\"className\":\""
                + escape(screen.getClass().getName()) + "\",\"title\":\"" + escape(screen.getTitle().getString())
                + "\",\"stateVersion\":" + version + ",\"capabilities\":[\"inspect_elements\",\"click\",\"focus\",\"type_text\",\"set_slider\",\"choose\",\"scroll\",\"key_press\"]}}";
    }

    private static String elementJson(String id, GuiEventListener element) {
        String role = "widget";
        String label = "";
        boolean visible = true;
        boolean enabled = true;
        boolean focused = element.isFocused();
        String value = null;
        String bounds = "";
        if (element instanceof AbstractWidget widget) {
            label = widget.getMessage().getString();
            visible = widget.visible;
            enabled = widget.active;
            bounds = ",\"bounds\":{\"x\":" + widget.getX() + ",\"y\":" + widget.getY()
                    + ",\"width\":" + widget.getWidth() + ",\"height\":" + widget.getHeight() + "}";
            if (widget instanceof EditBox editBox) {
                role = "text_field";
                value = editBox.getValue();
            } else if (widget instanceof AbstractSliderButton) {
                role = "slider";
            } else if (widget instanceof CycleButton<?> cycleButton) {
                role = "cycle_button";
                value = String.valueOf(cycleButton.getValue());
            } else if (widget instanceof AbstractButton) {
                role = "button";
            }
        }
        String json = "{\"id\":\"" + id + "\",\"role\":\"" + role + "\",\"className\":\""
                + escape(element.getClass().getName()) + "\",\"label\":\"" + escape(label) + "\",\"enabled\":" + enabled
                + ",\"visible\":" + visible + ",\"focused\":" + focused + bounds;
        if (value != null) json += ",\"value\":\"" + escape(value) + "\"";
        return json + "}";
    }

    private static String signature(Screen screen) {
        if (screen == null) return "none";
        StringBuilder signature = new StringBuilder(screen.getClass().getName()).append('|').append(screen.getTitle().getString());
        appendSignature(signature, screen.children(), new IdentityHashMap<>());
        return signature.toString();
    }

    private static String screenId(Screen screen) {
        return "minecraft:" + screen.getClass().getSimpleName().replaceAll("Screen$", "").toLowerCase(Locale.ROOT);
    }

    private static String invalidTargetJson(String elementId) {
        return "{\"ok\":false,\"error\":{\"code\":\"invalid_target\",\"message\":\"The element ID does not identify the current screen\",\"elementId\":\""
                + escape(elementId) + "\"}}";
    }

    private Screen matchingScreen(long expectedStateVersion) {
        Screen screen = Minecraft.getInstance().gui.screen();
        return currentStateVersion(screen) == expectedStateVersion ? screen : null;
    }

    private String staleOrInvalidScreenJson(long expectedStateVersion) {
        long actual = currentStateVersion(Minecraft.getInstance().gui.screen());
        return "{\"ok\":false,\"error\":{\"code\":\"" + (actual == expectedStateVersion ? "no_screen" : "stale_screen")
                + "\",\"expectedStateVersion\":" + expectedStateVersion + ",\"actualStateVersion\":" + actual + "}}";
    }

    private static GuiEventListener element(Screen screen, String elementId) {
        return findElement(screen.children(), "element", elementId, new IdentityHashMap<>());
    }

    private static void appendElements(List<String> output, List<? extends GuiEventListener> children, String prefix,
                                       IdentityHashMap<GuiEventListener, Boolean> visited) {
        for (int index = 0; index < children.size() && output.size() < MAX_INSPECTED_ELEMENTS; index++) {
            GuiEventListener child = children.get(index);
            if (visited.put(child, Boolean.TRUE) != null) continue;
            String id = prefix + ":" + index;
            output.add(elementJson(id, child));
            if (child instanceof ContainerEventHandler container) {
                appendElements(output, container.children(), id + "/child", visited);
            }
        }
    }

    private static GuiEventListener findElement(List<? extends GuiEventListener> children, String prefix, String targetId,
                                                IdentityHashMap<GuiEventListener, Boolean> visited) {
        for (int index = 0; index < children.size(); index++) {
            GuiEventListener child = children.get(index);
            if (visited.put(child, Boolean.TRUE) != null) continue;
            String id = prefix + ":" + index;
            if (id.equals(targetId)) return child;
            if (child instanceof ContainerEventHandler container) {
                GuiEventListener found = findElement(container.children(), id + "/child", targetId, visited);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void appendSignature(StringBuilder signature, List<? extends GuiEventListener> children,
                                        IdentityHashMap<GuiEventListener, Boolean> visited) {
        for (GuiEventListener child : children) {
            if (visited.put(child, Boolean.TRUE) != null) continue;
            signature.append('|').append(child.getClass().getName()).append(':').append(child.isFocused());
            if (child instanceof AbstractWidget widget) {
                signature.append(':').append(widget.getMessage().getString()).append(':').append(widget.active).append(':').append(widget.visible);
            }
            if (child instanceof EditBox editBox) signature.append(':').append(editBox.getValue());
            if (child instanceof CycleButton<?> cycleButton) signature.append(':').append(cycleButton.getValue());
            if (child instanceof ContainerEventHandler container) appendSignature(signature, container.children(), visited);
        }
    }

    private String actionJson(String action, String elementId) {
        return "{\"ok\":true,\"action\":\"" + action + "\",\"elementId\":\"" + escape(elementId)
                + "\",\"result\":\"applied\",\"screenStateVersion\":" + currentStateVersion(Minecraft.getInstance().gui.screen()) + "}";
    }

    private static String unsupportedJson(String elementId, String message) {
        return "{\"ok\":false,\"error\":{\"code\":\"unsupported_element\",\"message\":\"" + escape(message)
                + "\",\"elementId\":\"" + escape(elementId) + "\"}}";
    }

    private static InputWithModifiers input(int modifiers) {
        return new InputWithModifiers() {
            @Override public int input() { return 257; }
            @Override public int modifiers() { return modifiers; }
        };
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
