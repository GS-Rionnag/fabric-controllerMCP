package dev.fabriccontrollermcp;

import com.mojang.blaze3d.platform.InputConstants;
import dev.fabriccontrollermcp.mixin.KeyboardHandlerInvoker;
import dev.fabriccontrollermcp.mixin.MouseHandlerAccessor;
import dev.fabriccontrollermcp.mixin.MouseHandlerInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Locale;

/**
 * Deliberately raw keyboard and mouse control. Calls are dispatched through
 * vanilla's input handlers, rather than mutating a UI element or an OS device.
 */
public final class RawInputService {
    private static final int ALLOWED_MODIFIERS = InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL
            | InputConstants.MOD_ALT | InputConstants.MOD_SUPER | InputConstants.MOD_CAPS_LOCK | InputConstants.MOD_NUM_LOCK;
    private final Set<String> heldKeys = new TreeSet<>();
    private final Set<Integer> heldButtons = new TreeSet<>();

    public String keyJson(String action, String keyName, Integer modifiers) {
        InputConstants.Key key = resolveKeyboardKey(keyName);
        if (key == null) {
            return error("invalid_key", "Use a keyboard key such as key.keyboard.escape, key.keyboard.w, or key.keyboard.left.shift");
        }
        int resolvedModifiers = modifiers == null ? 0 : modifiers;
        if ((resolvedModifiers & ~ALLOWED_MODIFIERS) != 0) return error("invalid_modifiers", "modifiers contains unsupported GLFW modifier bits");

        if (action.equals("key_down")) sendKey(key, InputConstants.PRESS, resolvedModifiers);
        else if (action.equals("key_up")) sendKey(key, InputConstants.RELEASE, resolvedModifiers);
        else {
            sendKey(key, InputConstants.PRESS, resolvedModifiers);
            sendKey(key, InputConstants.RELEASE, resolvedModifiers);
        }
        return "{\"ok\":true,\"action\":\"" + action + "\",\"key\":\"" + VanillaClientService.escape(key.getName())
                + "\",\"heldKeys\":" + stringArray(heldKeys) + "}";
    }

    public String mouseMoveJson(Double x, Double y) {
        if (x == null || y == null || !Double.isFinite(x) || !Double.isFinite(y)) {
            return error("invalid_coordinates", "input.mouse_move requires finite x and y window-pixel coordinates");
        }
        Minecraft client = Minecraft.getInstance();
        long windowHandle = client.getWindow().handle();
        ((MouseHandlerInvoker) client.mouseHandler).fabricControllerMcp$handleMove(windowHandle, x, y);
        preservePhysicalCursorBaseline(client, windowHandle);
        return "{\"ok\":true,\"action\":\"mouse_move\",\"x\":" + x + ",\"y\":" + y + "}";
    }

    public String textJson(String text) {
        if (text == null || text.isEmpty() || text.codePointCount(0, text.length()) > 1024) {
            return error("invalid_text", "input.text requires 1 through 1024 Unicode characters");
        }
        Minecraft client = Minecraft.getInstance();
        KeyboardHandlerInvoker keyboard = (KeyboardHandlerInvoker) client.keyboardHandler;
        text.codePoints().forEach(codepoint -> keyboard.fabricControllerMcp$handleCharacter(
                client.getWindow().handle(), new CharacterEvent(codepoint)));
        return "{\"ok\":true,\"action\":\"text\",\"characters\":" + text.codePointCount(0, text.length()) + "}";
    }

    public String mouseJson(String action, Integer button) {
        Integer resolvedButton = button == null ? null : button;
        if (resolvedButton == null || resolvedButton < 0 || resolvedButton > 7) {
            return error("invalid_button", "button must be a GLFW mouse button number from 0 through 7");
        }
        if (action.equals("mouse_down")) sendButton(resolvedButton, InputConstants.PRESS);
        else if (action.equals("mouse_up")) sendButton(resolvedButton, InputConstants.RELEASE);
        else {
            sendButton(resolvedButton, InputConstants.PRESS);
            sendButton(resolvedButton, InputConstants.RELEASE);
        }
        return "{\"ok\":true,\"action\":\"" + action + "\",\"button\":" + resolvedButton
                + ",\"heldButtons\":" + heldButtons + "}";
    }

    public String scrollJson(Double horizontalAmount, Double verticalAmount) {
        double horizontal = horizontalAmount == null ? 0.0 : horizontalAmount;
        double vertical = verticalAmount == null ? 0.0 : verticalAmount;
        if (!Double.isFinite(horizontal) || !Double.isFinite(vertical)) return error("invalid_scroll", "Scroll amounts must be finite numbers");
        Minecraft client = Minecraft.getInstance();
        ((MouseHandlerInvoker) client.mouseHandler).fabricControllerMcp$handleScroll(client.getWindow().handle(), horizontal, vertical);
        return "{\"ok\":true,\"action\":\"scroll\",\"horizontalAmount\":" + horizontal + ",\"verticalAmount\":" + vertical + "}";
    }

    public String stateJson() {
        Minecraft client = Minecraft.getInstance();
        return "{\"ok\":true,\"heldKeys\":" + stringArray(heldKeys) + ",\"heldButtons\":" + heldButtons
                + ",\"mouse\":{\"x\":" + client.mouseHandler.xpos() + ",\"y\":" + client.mouseHandler.ypos()
                + ",\"grabbed\":" + client.mouseHandler.isMouseGrabbed() + "}}";
    }

    private void sendKey(InputConstants.Key key, int glfwAction, int modifiers) {
        Minecraft client = Minecraft.getInstance();
        ((KeyboardHandlerInvoker) client.keyboardHandler).fabricControllerMcp$handleKey(
                client.getWindow().handle(), glfwAction, new KeyEvent(key.getValue(), 0, modifiers));
        if (glfwAction == InputConstants.RELEASE) heldKeys.remove(key.getName()); else heldKeys.add(key.getName());
    }

    private void sendButton(int button, int glfwAction) {
        Minecraft client = Minecraft.getInstance();
        ((MouseHandlerInvoker) client.mouseHandler).fabricControllerMcp$handleButton(
                client.getWindow().handle(), new MouseButtonInfo(button, 0), glfwAction);
        if (glfwAction == InputConstants.RELEASE) heldButtons.remove(button); else heldButtons.add(button);
    }

    /**
     * In a world, Minecraft captures the operating-system cursor and interprets
     * movement as a delta. A synthetic absolute callback must not become the
     * baseline for the next physical callback, or the user's next mouse move
     * applies an equal-and-opposite camera turn. Keep only the generated turn
     * delta and restore the actual GLFW cursor position as the baseline.
     */
    private static void preservePhysicalCursorBaseline(Minecraft client, long windowHandle) {
        if (!client.mouseHandler.isMouseGrabbed()) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer physicalX = stack.mallocDouble(1);
            DoubleBuffer physicalY = stack.mallocDouble(1);
            GLFW.glfwGetCursorPos(windowHandle, physicalX, physicalY);
            MouseHandlerAccessor mouse = (MouseHandlerAccessor) client.mouseHandler;
            mouse.fabricControllerMcp$setXpos(physicalX.get(0));
            mouse.fabricControllerMcp$setYpos(physicalY.get(0));
        }
    }

    /**
     * InputConstants.getKey(String) only accepts a numeric suffix for keyboard
     * keys in this Minecraft version (for example, key.keyboard.340). MCP
     * callers should be able to use the vanilla-facing names returned by
     * keybindings, including underscore variants such as left_shift.
     */
    private static InputConstants.Key resolveKeyboardKey(String requestedName) {
        if (requestedName == null || requestedName.isBlank()) return null;
        try {
            InputConstants.Key direct = InputConstants.getKey(requestedName);
            if (direct != null && direct != InputConstants.UNKNOWN && direct.getType() == InputConstants.Type.KEYSYM) return direct;
        } catch (RuntimeException ignored) {
            // Named GLFW keys are resolved below instead of leaking a parser error.
        }

        String requested = normalizeKeyName(requestedName);
        for (int keyCode = 0; keyCode <= 348; keyCode++) {
            InputConstants.Key candidate = InputConstants.getKey(new KeyEvent(keyCode, 0, 0));
            if (candidate.getType() == InputConstants.Type.KEYSYM && normalizeKeyName(candidate.getName()).equals(requested)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalizeKeyName(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace(".", "").replace("-", "");
    }

    private static String stringArray(Set<String> values) {
        return "[" + values.stream().map(value -> "\"" + VanillaClientService.escape(value) + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]";
    }

    private static String error(String code, String message) {
        return "{\"ok\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + VanillaClientService.escape(message) + "\"}}";
    }
}
