package dev.fabriccontrollermcp;

import net.minecraft.client.Minecraft;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** The sole boundary for MCP work that reads or changes Minecraft client state. */
public final class MinecraftClientBridge {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    public <T> T call(Callable<T> action) throws Exception {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) {
            return action.call();
        }

        CompletableFuture<T> result = new CompletableFuture<>();
        client.execute(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }
}
