package dev.fabriccontrollermcp.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's own GLFW keyboard callback path to the raw-input service. */
@Mixin(KeyboardHandler.class)
public interface KeyboardHandlerInvoker {
    @Invoker("keyPress")
    void fabricControllerMcp$handleKey(long windowHandle, int action, KeyEvent event);

    @Invoker("charTyped")
    void fabricControllerMcp$handleCharacter(long windowHandle, CharacterEvent event);
}
