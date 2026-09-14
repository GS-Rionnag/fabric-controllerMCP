# Current state

Last updated: 2026-09-14

## Repository

- A minimal Fabric client-mod skeleton now exists for Minecraft 26.2.
- The core targets Fabric Loader 0.19.5 and Java 25; Fabric API remains
  optional and is not a core dependency.
- The initial source entrypoint is
  `dev.fabriccontrollermcp.FabricControllerMcpClient`.
- Gradle is not installed globally, but the project-local Gradle 9.5.0 wrapper
  has been generated (`gradlew`, `gradlew.bat`, and `gradle/wrapper/*`).
- `./gradlew.bat --no-daemon clean build` was run on 2026-09-13. It reaches
  Fabric Loom 1.17.20 and completes successfully.
- `./gradlew.bat --no-daemon runClient` was verified on 2026-09-13. Minecraft
  26.2 reached the title screen and logged `Fabric Controller MCP initialized`.
  Windows performance-counter warnings were non-fatal Minecraft environment
  warnings.
- Minecraft 26.2 ships unobfuscated code. The build therefore uses Fabric
  Loom's non-remapping plugin (`net.fabricmc.fabric-loom`), standard
  `implementation` dependencies, and no development-mappings artifact.
- The client now starts a local, loopback-only MCP Streamable HTTP endpoint at
  `http://127.0.0.1:43125/mcp`, using the official MCP Java SDK 2.0.1 and
  embedded Tomcat 11.0.11. It exposes `ping` and `server_status`; the latter
  reaches Minecraft state only through `MinecraftClientBridge`.
- OpenAI Secure MCP Tunnel `tunnel_6aa75324261c81919453e546b1aafa67` has a
  local `tunnel-client` 0.0.14 profile at
  `%LOCALAPPDATA%\\fabric-controller-mcp\\tunnel-client\\profiles\\fabric-controller-mcp.yaml`.
  It forwards to the local `/mcp` endpoint and deliberately reads its runtime
  key from `CONTROL_PLANE_API_KEY`; no secret is stored in the profile.
- The Windows user environment now provides `CONTROL_PLANE_API_KEY`. The
  generic launcher is started at user logon by `OpenAI Tunnel Clients.vbs` in
  the Windows Startup folder. It scans both the official default profile
  directory (`~/.config/tunnel-client`) and the existing Fabric profile
  directory, then supervises every `.yaml`/`.yml` profile it finds. It
  re-scans every 15 seconds and restarts a profile if its client exits. Each
  profile must use a distinct health/admin port (or an ephemeral
  `127.0.0.1:0` port). The Fabric profile uses `http://127.0.0.1:18080/ui`.
- The embedded Tomcat endpoint now creates its connector explicitly and
  registers the loopback filter before context initialization. A real MCP
  `initialize` request succeeds at `http://127.0.0.1:43125/mcp`; the official
  tunnel-client `doctor` check reports `RESULT ok` while Minecraft is running.
- The distributable mod JAR now packages the MCP Java SDK, embedded Tomcat,
  and their transitive runtime libraries as Fabric Loader jar-in-jar entries.
  Users can install the one mod JAR without separately providing these
  libraries. A clean `./gradlew.bat --no-daemon clean build` on 2026-09-14
  produced the JAR and verified all 15 nested runtime JAR entries.
- The tunnel/ChatGPT connection was exercised successfully on 2026-09-13.

## Active work

An implementation plan is documented in `docs/IMPLEMENTATION_PLAN.md`.
The current anchor choices are captured in `docs/DECISIONS.md`.

## Next milestone

Begin the first Phase 2 UI tool: structured current-screen inspection.
