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
- The Windows user environment provides `CONTROL_PLANE_API_KEY`. The Fabric
  tunnel now runs as the automatic LocalSystem Windows Service
  `FabricControllerMcpTunnel` (display name: `Fabric Controller MCP Tunnel`),
  with Service Control Manager recovery configured. Its native wrapper and a
  non-secret copy of `tunnel-client.exe` plus the Fabric profile live under
  `%ProgramData%\fabric-controller-mcp`; the wrapper reads the existing key
  from the configured user's registry environment only at runtime. The key is
  not copied into the service configuration, profile, repository, or logs.
  The former Startup-folder VBS launcher was renamed with a `.disabled` suffix
  to prevent duplicate tunnel clients. The Fabric profile health endpoint is
  `http://127.0.0.1:18080/ui`.
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

The clickable, expandable command reference begins at `docs/COMMANDS.md`.
It mirrors the runtime tree with separate pages for Settings, UI, Main Menu
(including Single-player and Multiplayer), and In-game; future branches can be
documented without growing one monolithic file.

An isolated `compat/1.20.1` Fabric remapping build profile now targets Minecraft
1.20.1, Fabric Loader 0.14.22, official Mojang mappings, and Java 17 without
altering the 26.2 build. It correctly resolves the older target, but no 1.20.1
JAR has been produced yet: the 26.2 UI, server, and world adapters use 31 APIs
that changed after 1.20.1 and require dedicated compatibility implementations.

The first Phase 2 low-level UI foundation is implemented: `ui.get_current_screen`,
`ui.inspect_elements`, `ui.click`, `ui.select`, `ui.set_focus`, `ui.type_text`,
`ui.set_slider`, `ui.choose`, `ui.toggle`, and `ui.scroll`. Raw keyboard and
mouse events now live in the separate shared `input` branch instead of UI.
Actions are guarded by a screen-state version, which changes when the inspected
screen, standard widget state, or supported text/cycle values change. The
implementation recursively walks standard nested UI containers (up to 512
elements), including supported widget metadata and text-field values. This
exposes individual option rows and sliders inside vanilla scrolling lists with
hierarchical IDs such as `element:0/child:2`; unsupported custom widgets return
structured errors rather than pretending to succeed. A clean production build
passed on 2026-09-14. The updated client launched, MCP discovery advertised all
new tools, and a live `settings.get` request succeeded. The connector binds
only to `127.0.0.1`.

Manual tunnel testing on 2026-09-14 confirmed `state`, `settings.music_and_sounds.list`,
`settings.get`, live `sound.music` update and restoration, `controls.list`, and
an idempotent `controls.update` for `key.screenshot`. Saved-server create and
replace also passed. Two defects remain before Phase 2 can be considered
verified: `mainmenu.singleplayer.create` initially ignored an explicit seed of
`12345` when it arrived as text. It now accepts both JSON numeric and signed
64-bit string seed values, rejecting malformed seeds rather than silently
generating one. `ui.key_press` was intentionally removed and replaced by the
shared raw `input` branch, which dispatches keyboard and mouse events through
vanilla handlers in both states. Saved-server removal was not tested because
the connected ChatGPT host blocked that state-changing call for user approval;
the temporary `MCP Test Server Updated` entry remains. No multiplayer server
was joined. A clean production build passed after these changes; runtime raw
input and corrected string-seed behavior still need manual verification after a
client restart. Retesting then passed raw Escape input, seed `12345` world
creation, return to the title screen, final local-world listing, and temporary
saved-server removal. One false-negative remains: activating Save and Quit to
Title succeeds, but `ui.click` can return `client_unavailable` while the
integrated world shutdown exceeds the bridge's current five-second wait. The
subsequent state and screen calls correctly report the title screen and
`mainmenu`. The bridge now waits up to 30 seconds for vanilla client-thread
work (including integrated-world save/disconnect) and distinguishes a genuine
overrun as `client_timeout`; it never mislabels a timeout as
`client_unavailable`. A clean production build passed, and a live retest of
Save and Quit to Title returned `{"ok":true,"action":"click","elementId":"element:9","result":"activated","screenStateVersion":2}`
followed by `mainmenu` state.

The raw-input retest found that `input.key_down` and `input.key_up` rejected
the documented `key.keyboard.left_shift` spelling: this Minecraft version's
`InputConstants.getKey(String)` expects numeric keyboard suffixes and threw a
`NumberFormatException`, which the transport incorrectly surfaced as
`client_unavailable`. `RawInputService` now resolves vanilla key names by
their `KeyEvent` representation, accepting canonical dotted names such as
`key.keyboard.left.shift` and underscore aliases such as
`key.keyboard.left_shift`; malformed names return `invalid_key` instead of
throwing. A clean production build passed. The next manual retest must take
place in a single-player world so held W/A/S/D movement and mouse camera
movement can be visibly confirmed; release every held input before exiting.

The single-player camera retest exposed a follow-up issue: `input.mouse_move`
fed an absolute coordinate into Minecraft while the game had captured the OS
cursor. The next real mouse callback could then arrive at the physical
cursor's centered position and undo the synthetic turn. `RawInputService` now
restores the real GLFW cursor position as `MouseHandler`'s next-event baseline
after a captured-cursor injected move, preserving the injected camera delta
while allowing the user's following physical mouse motion to continue normally.
A clean production build passed, and the user confirmed the live single-player
retest works correctly: injected camera motion remains in place and subsequent
physical mouse movement continues naturally rather than reversing it. The raw
input foundation is now manually verified.
