# Step-by-step implementation plan

Status: draft pending project-owner decisions.  
Last updated: 2026-09-13

## Working agreement

We complete one checkpoint at a time.  A checkpoint is complete only when its
acceptance criteria and verification are recorded.  New capabilities are added
behind explicit permission checks and are not assumed to be safe merely because
an MCP client can call them.

The source code and release artifacts will be public/open source under the MIT
license. This does not create a shared MCP service: every user's OpenAI Secure
MCP Tunnel remains private to that user's account and machine.

Initial releases are published through GitHub Releases; Modrinth and CurseForge
distribution are deferred until the core path is stable.

## Checkpoint 0 — Product and compatibility contract

**Goal:** Resolve the choices that determine every implementation dependency.

- Choose a supported Minecraft release, Java version, Fabric Loader/API, and
  mappings, plus the compatibility strategy and initial support matrix. The
  project goal is broad practical version coverage, implemented through
  version-family adapters and tested release lines where needed.
- Use Minecraft Java 26.2 as the first development anchor. It ships
  unobfuscated code, so use Fabric Loom's non-remapping plugin and no mappings
  artifact; pin its exact loader, optional Fabric API, Gradle wrapper, and Java
  toolchain before scaffolding, then extend compatibility through version-family
  adapters.
- Scope compatibility work to Minecraft Java Edition; Bedrock Edition is out
  of scope.
- Set the compatibility floor at Minecraft Java Edition 1.14, through current
  Fabric-supported releases. Aim to cover patch releases where feasible,
  publish every exception, and test every claimed release line. Minecraft
  1.8–1.13 is out of scope.
- Deliver one mod JAR for users to place in their mods folder. Build it around
  a Java-8-compatible bootstrap and shared core; after runtime game-version
  detection, load only the matching version-family adapter. Common code must
  never directly link to version-specific Minecraft classes. This constraint
  requires conditional mixins/entrypoints, strict dependency isolation, and a
  compatibility test matrix. Users supply the matching Fabric Loader for their
  Minecraft profile. Fabric API is optional: the core may not require it, and
  its enhancements are activated only when present.
- Choose the MCP SDK/library, ChatGPT app integration, and the OpenAI Secure
  MCP Tunnel pattern. Each user creates their own tunnel and runs the official
  `tunnel-client` beside their opted-in private MCP endpoint; do not operate a
  central relay/control plane or require inbound firewall configuration for the
  normal setup. This permits private/developer-mode use, not public plugin
  submission or directory distribution.
- Define the supported operating systems and intended MCP clients. The first
  client experience targets a ChatGPT MCP app/integration; interoperability
  with other standards-compatible MCP clients remains a core requirement.
  Support Windows, macOS, and Linux from the first public release.
- Support one active Minecraft client per user/tunnel initially; defer
  multi-client routing and selection.
- Support the same capability surface in single-player, LAN, and multiplayer
  worlds where technically available. Preserve normal server authority and
  report server-side rejections; do not attempt to bypass permissions,
  protection, or anti-cheat systems.
- Write a capability/permission matrix for read-only, direct game actions, and
  client-input actions. Tunnel connection is the user authorization boundary;
  do not require an in-game master switch initially, but retain validation,
  bounded execution, structured outcomes, and normal server authority.
- Select the first vertical slice and its acceptance tests.

**Exit criteria:** all decisions above are captured in `DECISIONS.md`; the
first milestone has a user-observable demonstration.

## Checkpoint 1 — Reproducible Fabric client-mod foundation

**Goal:** A minimal mod builds and launches reliably in the chosen environment.

- Create the Gradle/Fabric project and metadata.
- Add development run configuration and documented prerequisites.
- Add configuration loading, logging, and a diagnostic command/status surface.
- Establish package/module boundaries: MCP transport, domain services,
  Minecraft-thread adapter, and configuration/security.
- Add a basic automated build/check workflow.

**Exit criteria:** clean checkout builds; the development client starts with
the mod enabled; diagnostics prove the configured mod/version is loaded.

## Checkpoint 2 — MCP lifecycle and safe request bridge

**Goal:** The mod can accept a harmless diagnostic request through its approved
transport without unsafe game-thread access.

- Start/stop the local MCP server with the client-mod lifecycle.
- Keep any direct local endpoint bound to loopback; expose the chosen
  authentication/consent mechanism and design it to work behind the OpenAI
  Secure MCP Tunnel selected in Checkpoint 2.5.
- Implement tool registration, schemas, structured success/errors, timeouts,
  cancellation, and audit-safe logs.
- Keep persistent MCP action logs disabled by default; provide explicitly
  enabled local diagnostic logging for troubleshooting.
- Implement a single, tested bridge for work that must run on the Minecraft
  client thread; MCP worker threads never access game state directly.
- Provide `server_status` and `ping`/`capabilities` diagnostic tools.

**Exit criteria:** an approved test client can discover and call the diagnostic
tools; rejected/invalid requests are safe and intelligible; mod shutdown closes
the server.

## Checkpoint 2.5 — User-managed tunnel and ChatGPT connection guide

**Goal:** Let a user safely reach their opted-in game client from cloud
ChatGPT, without a project-operated backend.

- Use OpenAI Secure MCP Tunnel as the supported private transport; document
  secure defaults and the required official `tunnel-client` lifecycle.
- Generate a local connection status, pairing material, and clear setup
  instructions from the mod.
- Ship the manual guide first. Defer an optional, consent-based helper that
  downloads and launches the official `tunnel-client` until the core path is
  proven; it must never be required for use.
- Document how to create/select the tunnel in ChatGPT developer mode, revoke
  access, rotate credentials, and shut the endpoint down.
- Publish the limitations: the client is reachable only while the game and
  tunnel run; the project cannot provide cross-device presence or recovery
  when a user-managed endpoint is offline.

**Exit criteria:** a user can follow the guide, connect their own ChatGPT
configuration to their own protected tunnel, call a permitted diagnostic tool
remotely, and revoke access.

## Checkpoint 2.6 — Interactive MCP UI

**Goal:** Provide rich, optional interactive UI for MCP Apps-capable hosts.

- Emit MCP Apps/MCP-UI-compatible UI resources directly from the Java mod;
  bundle static, sandbox-safe UI assets in the controller JAR.
- Link eligible tools to UI resources and retain structured text/JSON results
  for hosts that do not render UI.
- Use the ChatGPT Apps SDK compatibility path and test graceful fallback.
- Start with one read-only panel (recommended: player status and inventory)
  before adding action controls.

**Exit criteria:** a supported host renders the selected panel; unsupported
hosts receive an equally complete non-UI result; UI-originated actions remain
subject to normal authorization and validation.

## Checkpoint 3 — In-game UI control vertical slice

**Goal:** Let an MCP client control Minecraft's in-game UI from the title
screen before a world is loaded.

- Implement a generic, typed screen API: current-screen metadata; widget/tree
  inspection; focus; click; toggle; choose; type; scroll; and keybind.
- Permit inspection and interaction of all in-game UI text fields, including
  chat and other mods' fields. Treat this as high-trust remote access; disclose
  the resulting data-sharing boundary in setup and in the connection status.
- Define stable element IDs/roles, screen-state freshness, response limits,
  and structured stale/unsupported-element failures.
- Add high-level version-adapted commands for common flows: `server.join`,
  `world.create`, `settings.get`, `settings.update`, and controls/keybinding
  inspection and updates.
- Prefer direct client-side APIs for high-level operations wherever Minecraft
  exposes them. For example, changing a sound option must update the live
  option on the Minecraft client thread while gameplay continues; it must not
  first navigate to Music & Sound Options or visibly alter the current screen.
  Use the generic UI layer only where a flow is genuinely screen-only or no
  stable direct API exists.
- Support title screen, multiplayer/server-entry screens, create-world flow,
  settings, and controls. The Minecraft launcher and login screens are
  explicitly out of scope; rich custom-mod UI support is deferred.
- Verify every action in a development profile and record per-version UI
  differences in the compatibility matrix.

**Exit criteria:** a user can inspect and complete the chosen common flow from
the title screen through a remote MCP request; each generic action returns a
typed result or an explicit unsupported/stale/invalid-target failure.

## Checkpoint 4 — Read-only player and world inspection

**Goal:** Return authoritative, typed state after a world is available.

- Implement the selected player and/or world inspection tools.
- Define stable response schemas, null/unknown behavior, and data limits.
- Verify behavior in title screen, loaded single-player world, and disconnected
or invalid-target cases.
- Add examples and a small compatibility-test matrix.
- Add a standalone screenshot tool for visual observation of menus and loaded
  worlds, with clear image-size and privacy limits. Return screenshots directly
  to the MCP client and do not save them to disk by default.

**Suggested minimum slice:** `player.get_state`, `world.get_context`, and
`world.get_block` with explicit unavailable-world errors.

**Exit criteria:** each tool returns validated JSON results and fails
predictably in all supported client states.

## Checkpoint 5 — One direct interaction at a time

**Goal:** Introduce a narrow, observable state-changing capability.

- Select exactly one of use, attack, place, or mine.
- Require capability enablement, confirmation policy, precondition checks, and
  a target/range/permission validation path.
- Return a structured outcome: accepted, started, completed, timed out,
  rejected, or unknown.
- Test in the agreed single-player and multiplayer modes.

**Exit criteria:** the action can neither execute outside policy nor report a
false success; audit records contain useful but non-sensitive detail.

## Checkpoint 6 — Navigation

**Goal:** Deliver one bounded navigation behavior before general automation.

- Choose travel-to-coordinate or follow-player first.
- Build world observation and path-planning separately from execution.
- Add cancellation, duration/distance limits, hazards, stuck detection, and
  status reporting.
- Gate long-running behavior with an explicit session/permission policy.
- Implement exactly one active autonomous job per Minecraft client, with status
  and cancel tools. Support detached/background jobs that continue after their
  chat turn ends and foreground/attached jobs that are bound to the active MCP
  request. Stop an attached job if that request is cancelled or its connection
  ends; stop either mode on completion, explicit cancel, stuck/failure, game
  shutdown, or actual tunnel loss. Validate OpenAI tunnel cancellation
  propagation during integration testing. Emit MCP progress notifications only
  when the active request's host supplies a progress token; persist a bounded
  job event log for later `job.status` retrieval and never assume the server
  can initiate a new chat message. Do not impose a project-level maximum
  duration on detached jobs while the client and tunnel stay connected.
  Reject conflicting job starts by default; after explicit user approval,
  cancel and replace the active job. Replacement approval occurs through a
  ChatGPT follow-up confirmation, not an in-game prompt.
  UI-action timing remains a separate research question.

**Exit criteria:** navigation can be started, observed, cancelled, and safely
terminates at boundaries or failures.

## Checkpoint 7 — Client input, camera, screenshots, and wider UI control

**Goal:** Add the highest-risk client-control capabilities deliberately.

- Implement keyboard/mouse/camera independently from server-authoritative
  actions. Treat raw input as a fallback; prefer semantic/high-level tools for
  reliability and token efficiency.
- Establish screenshot storage, retention, and privacy behavior.
- Support only whitelisted UI interactions before considering general element
  targeting.
- Revisit security, multiplayer compatibility, and abuse cases for every new
  capability.

**Exit criteria:** each control is individually gated, bounded, observable,
and documented with its limitations.

## Cross-cutting definition of done

For every checkpoint and tool: schema documentation, validation, deterministic
error handling, Minecraft-thread safety, logging appropriate to the privacy
policy, focused tests, manual in-game verification, and updates to the
continuity documents are required.

## Immediate next action

Manually validate Phase 2's completed direct vanilla-client services in a
development client: every settings/keybinding entry, saved-server management
and joining, and local-world list/create/load. Retain the generic UI API for
screen-only flows and unsupported custom controls, then begin Phase 3
read-only player/world inspection.
