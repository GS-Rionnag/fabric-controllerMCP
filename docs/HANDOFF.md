# Handoff notes

Use this file when resuming work in a new chat.

## Snapshot — 2026-09-14

We are planning `fabric-controllerMCP`, a Fabric Minecraft mod that opens an
MCP server for LLMs. The desired eventual API areas are World, Player,
Navigation, Interaction, Client Control, and Mod UI APIs. The user explicitly
wants capability expansion to happen step by step.

The project now has a minimal Minecraft 26.2 Fabric client-mod skeleton:
`build.gradle`, `settings.gradle`, `gradle.properties`, the Gradle 9.5.0
wrapper, `src/main/resources/fabric.mod.json`, and a client bootstrap class in
`src/client/java`.
It targets Fabric Loader 0.19.5 and Java 25, with Fabric API deliberately
optional. The decision-gated plan is in `docs/IMPLEMENTATION_PLAN.md`.

`./gradlew.bat --no-daemon clean build` and `./gradlew.bat --no-daemon
runClient` were verified successfully on 2026-09-13. The client reached the
title screen and logged `Fabric Controller MCP initialized`; Windows
performance-counter warnings were non-fatal environment warnings. Minecraft
26.2 ships unobfuscated code, so the project uses
Fabric Loom's non-remapping plugin (`net.fabricmc.fabric-loom`) with standard
`implementation` dependencies and no Yarn/Mojang development mappings.

The local MCP diagnostic transport is now implemented with the official MCP
Java SDK 2.0.1, an embedded Tomcat 11.0.11 Servlet host, and Streamable HTTP
at `http://127.0.0.1:43125/mcp`. It is loopback-only and rejects non-local
browser origins. The initial `ping` and `server_status` tools are registered;
Minecraft work must cross `MinecraftClientBridge`.

The official Windows `tunnel-client` 0.0.14 binary is installed outside the
repository at `%LOCALAPPDATA%\\fabric-controller-mcp\\tunnel-client`. Its
`fabric-controller-mcp` profile is configured with tunnel ID
`tunnel_6aa75324261c81919453e546b1aafa67` and forwards to
`http://127.0.0.1:43125/mcp`. The remaining prerequisite is a runtime API key
in the `CONTROL_PLANE_API_KEY` environment variable; it has been set in the
Windows user environment without writing the value into the repository or
profile. The `OpenAI Tunnel Clients.vbs` user Startup-folder launcher starts
and supervises every local tunnel-client `.yaml`/`.yml` profile found in the
official default profile directory (`~/.config/tunnel-client`) and the existing
Fabric profile directory. It re-scans for additions every 15 seconds and
restarts exited profile clients. Profiles need distinct health/admin ports (or
`127.0.0.1:0`); the Fabric profile uses `http://127.0.0.1:18080/ui`.

The embedded Tomcat server must call `tomcat.getConnector()` before starting;
without that call it logs a misleading listening message but binds no port.
The loopback filter also uses Tomcat filter definitions rather than the
post-initialization Servlet API. With those fixes, a local MCP `initialize`
request and `tunnel-client doctor` both succeed while the development client is
running.

The production mod JAR now uses Fabric Loader jar-in-jar packaging for the MCP
Java SDK, embedded Tomcat, and every transitive runtime library required by
those dependencies. Users only need the one mod JAR (plus their normal Fabric
Loader installation); no standalone MCP or Tomcat libraries need to be copied
into the mods folder. `./gradlew.bat --no-daemon clean build` passed on
2026-09-14 and its output declared all 15 nested runtime JAR entries.

## Resume from here

1. Begin Phase 2 with a structured, read-only current-screen inspection tool.
3. Continue Checkpoint 0 technical decisions only as they become necessary;
   product-level decisions are captured in `docs/DECISIONS.md`.

## Update checklist

Before ending a work session, record:

- What changed and where.
- What was verified and the result.
- The exact active task and immediate next step.
- New decisions, assumptions, limitations, or blockers.
