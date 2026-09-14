package dev.fabriccontrollermcp.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's own GLFW mouse callback path to the raw-input service. */
@Mixin(MouseHandler.class)
public interface MouseHandlerInvoker {
    @Invoker("onButton")
    void fabricControllerMcp$handleButton(long windowHandle, MouseButtonInfo button, int action);

    @Invoker("onMove")
    void fabricControllerMcp$handleMove(long windowHandle, double x, double y);

    @Invoker("onScroll")
    void fabricControllerMcp$handleScroll(long windowHandle, double horizontalAmount, double verticalAmount);
}
