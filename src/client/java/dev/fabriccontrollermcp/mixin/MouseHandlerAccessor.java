package dev.fabriccontrollermcp.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets injected camera movement retain the physical cursor baseline. */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void fabricControllerMcp$setXpos(double xpos);

    @Accessor("ypos")
    void fabricControllerMcp$setYpos(double ypos);
}
