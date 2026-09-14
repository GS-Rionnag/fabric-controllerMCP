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

The official Windows `tunnel-client` 0.0.14 binary remains installed outside
the repository at `%LOCALAPPDATA%\\fabric-controller-mcp\\tunnel-client`.
The Fabric tunnel is now operated by the automatic LocalSystem Windows Service
`FabricControllerMcpTunnel` (display name: `Fabric Controller MCP Tunnel`),
not the former Startup-folder process. Its native service wrapper and a
non-secret runtime copy of the tunnel client/profile live under
`%ProgramData%\\fabric-controller-mcp`; the profile retains tunnel ID
`tunnel_6aa75324261c81919453e546b1aafa67` and forwards to
`http://127.0.0.1:43125/mcp`. The wrapper reads `CONTROL_PLANE_API_KEY` from
the configured user's registry environment only at service startup; it never
copies the key into service settings, profiles, source, or logs. Service
recovery restarts a failed wrapper. The old `OpenAI Tunnel Clients.vbs` Startup
entry was renamed with a `.disabled` suffix to avoid duplicate clients. The
service and its `http://127.0.0.1:18080/readyz` check returned healthy on
2026-09-14.

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

The MCP server now advertises only one compact `mcp` tool. Its empty/root call
reports the client state and command-tree roots; `help` with a path recursively
lists exact state-aware commands. Shared categories are `settings` and `ui`.
The main-menu branch has single-player and multiplayer categories; the in-game
branch reserves player, world, interaction, and navigation for their later
phases. The settings branch mirrors vanilla's Options sections except Credits
& Attribution and supports compact per-section listings such as
`settings.music_and_sounds.list`, which exposes the direct `sound.music`
volume setting in every client state.

The repository's clickable command reference is `docs/COMMANDS.md`; it links
to independently maintained Settings, UI, Main Menu, and In-game pages under
`docs/commands/`.

`compat/1.20.1` is an isolated Java-17 remapping build profile for Minecraft
1.20.1. It resolves successfully through Loom but the shared 26.2 client code
does not compile there: `UiScreenService`, saved-server joining, keybinding
metadata, and world flows use newer APIs. Port those into version-specific
1.20.1 adapters before claiming or publishing a 1.20.1 JAR.

## Resume from here

1. The Save and Quit to Title retest now passes: `ui.click` returns success and
   the client reaches `mainmenu`. The bridge waits up to 30 seconds for vanilla
   save/disconnect work and returns `client_timeout`, rather than the false
   `client_unavailable`, only when that bounded wait is genuinely exceeded.
2. The shared raw `input` branch is manually verified for Escape pause-menu
   navigation; held W and Shift input; seed `12345` creation and local-world
   listing; saved-server removal; raw mouse movement/button/scroll; and raw
   text entry. `RawInputService` accepts both canonical dotted vanilla key
   names (`key.keyboard.left.shift`) and underscore aliases. Its captured-cursor
   baseline restoration prevents a physical mouse movement from reversing an
   injected camera turn; the user confirmed this works correctly in a live
   single-player world. The raw-input foundation is complete.
3. Continue Phase 2 only for uncovered vanilla screen-only flows using the
   generic UI tools. Direct services enumerate all registered OptionInstances
   and keybindings instead of maintaining a fragile hand-made allowlist. Sound
   categories are direct settings too: use `sound.music` for music volume.
4. Begin Phase 3 read-only player/world inspection only after the remaining
   Phase 2 verification issues are resolved. Maintain per-version compatibility
   notes for direct APIs.

## Update checklist

Before ending a work session, record:

- What changed and where.
- What was verified and the result.
- The exact active task and immediate next step.
- New decisions, assumptions, limitations, or blockers.
