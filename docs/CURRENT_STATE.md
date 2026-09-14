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

Phase 2 is active. The low-level generic UI layer and high-level
vanilla-client services are implemented and build.
High-level actions must use direct Minecraft client APIs on the client thread
where available, without opening or navigating a visible screen. This includes
live settings such as music volume, which should change while gameplay
continues. UI navigation remains the fallback for UI-only flows.

The detailed implementation plan is in `docs/IMPLEMENTATION_PLAN.md`; the
anchor choices are captured in `docs/DECISIONS.md`.

## Next milestone

Direct services now expose every registered vanilla `OptionInstance` through
`settings.list`, `settings.get`, and `settings.update`, so all live client
settings (including sound) can change without a menu. Every registered vanilla
key mapping is available through `controls.list` and `controls.update`.
Per-category volume settings use stable direct IDs such as `sound.music`; this
corrects the earlier omission where parameterized vanilla sound options were
not included in the general OptionInstance scan.
Saved-server listing, save/replace, removal, and join plus local-world listing,
direct normal-preset creation, and loading are also available. All operations
run via `MinecraftClientBridge` and use vanilla persistence/connection APIs.

The MCP surface is now deliberately compact: the host sees one `mcp` tool rather
than a flat list of every action. Calling it with no command returns the
state-aware root (`mainmenu` or `ingame`) plus shared `settings` and `ui`;
`help` with a path drills into categories. Main-menu commands are grouped into
single-player and multiplayer, while in-game future capabilities have their
own reserved branch. Settings mirrors vanilla Options sections (FOV, Online,
Skin Customization, Music & Sounds, Video, Controls, Language, Chat, Resource
Packs, Accessibility, and Telemetry), excluding Credits & Attribution. Each
direct settings section has a compact `.list` command; `settings.music_and_sounds.list`
was verified live and includes `sound.music`.

The first Phase 2 low-level UI foundation is implemented: `ui.get_current_screen`,
`ui.inspect_elements`, `ui.click`, `ui.select`, `ui.set_focus`, `ui.type_text`,
`ui.set_slider`, `ui.choose`, `ui.toggle`, `ui.scroll`, and `ui.key_press`.
Actions are guarded by a screen-state version, which changes when the inspected
screen, standard widget state, or supported text/cycle values change. The
implementation recursively walks standard nested UI containers (up to 512
elements), including supported widget metadata and text-field values. This
exposes individual option rows and sliders inside vanilla scrolling lists with
hierarchical IDs such as `element:0/child:2`; unsupported custom widgets return
structured errors rather than pretending to succeed. A clean production build
passed on 2026-09-14. The updated client launched, MCP discovery advertised all
new tools, and a live `settings.get` request succeeded. Stateful server/world
flows and live setting/keybinding changes still need interactive verification.
The connector binds only to `127.0.0.1`.
