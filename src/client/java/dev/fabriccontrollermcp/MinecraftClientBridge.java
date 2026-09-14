package dev.fabriccontrollermcp;

import net.minecraft.client.Minecraft;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** The sole boundary for MCP work that reads or changes Minecraft client state. */
public final class MinecraftClientBridge {
    /**
     * World save/disconnect work is performed synchronously by vanilla on the
     * client thread. Five seconds was short enough to report a completed
     * Save-and-Quit click as unavailable; keep a bounded but realistic wait.
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

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
        try {
            return result.get(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException error) {
            // Do not cancel the queued action: it may already be executing a
            // vanilla operation. Callers must receive a timeout, not a false
            // claim that the client is unavailable.
            throw new ClientThreadTimeoutException(DEFAULT_TIMEOUT, error);
        }
    }

    public static final class ClientThreadTimeoutException extends Exception {
        private final Duration timeout;

        private ClientThreadTimeoutException(Duration timeout, TimeoutException cause) {
            super("Minecraft client action exceeded the bounded wait", cause);
            this.timeout = timeout;
        }

        public Duration timeout() { return timeout; }
    }
}
