package dev.fabriccontrollermcp;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/** Rejects non-loopback requests and hostile browser origins before MCP parsing. */
public final class LoopbackOriginFilter implements Filter {
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!isLoopbackAddress(httpRequest.getRemoteAddr()) || !hasAllowedOrigin(httpRequest.getHeader("Origin"))) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Local MCP endpoint rejects this origin.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isLoopbackAddress(String address) {
        return "127.0.0.1".equals(address) || "::1".equals(address) || "0:0:0:0:0:0:0:1".equals(address);
    }

    private static boolean hasAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) return true;
        try {
            return LOCAL_HOSTS.contains(URI.create(origin).getHost());
        } catch (IllegalArgumentException error) {
            return false;
        }
    }
}
