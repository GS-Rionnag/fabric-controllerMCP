# Roadmap

Status legend: `planned` · `active` · `complete`

## Phase 0 — Foundation (`complete`)

- [x] Create continuity documentation.
- [x] Capture a decision-gated implementation plan (`docs/IMPLEMENTATION_PLAN.md`).
- [x] Choose target Minecraft, Fabric Loader, Fabric API, and Java versions.
- [x] Choose the MCP library and local transport.
- [x] Define the initial tool set and permission boundary.

## Phase 1 — Mod and MCP core (`active`)

- [x] Scaffold a Fabric client mod.
- [x] Start and stop a local MCP server with the mod lifecycle.
- [x] Register initial diagnostic tools with structured results.
- [x] Bridge MCP work safely onto the Minecraft client thread.
- [ ] Add diagnostics and an opt-in configuration surface.

## Phase 2 — In-game UI control (`active`)

- [x] Generic current-screen, widget, focus, click, typing, selection,
  scrolling, and keybinding API.
- [x] High-level title/menu tools: join server, create/load a normal-preset
  local world, inspect/update direct vanilla settings and controls, and manage
  saved servers without UI navigation.

High-level operations should use direct client APIs without opening menus where
those APIs exist. Generic screen interaction is reserved for UI-only flows and
as a compatibility fallback.

## Phase 3 — Read-only inspection (`planned`)

- [ ] Player state: position, inventory, equipment, health, effects.
- [ ] World state: blocks, entities, chunks, dimensions.
- [ ] Screenshot capture for menus and worlds.

## Phase 4 — Direct interaction (`planned`)

- [ ] Mine, place, attack, and use actions.
- [ ] Action preconditions, timeout behavior, and structured outcome reporting.

## Phase 5 — Navigation (`planned`)

- [ ] Travel to coordinates.
- [ ] A* pathfinding.
- [ ] Follow-player and mob-avoidance behaviors.

## Phase 6 — Client control and expansion (`planned`)

- [x] Shared raw keyboard and mouse event dispatch through vanilla client
  handlers (`input`); camera and rich custom-mod UI support remain planned.
- [ ] Arbitrary supported screen interaction and vision/coordinate fallback.
- [ ] Add capabilities one at a time with tests and documentation.

## Phase 7 — Full Fabric-version compatibility (`planned`)

- [ ] Support every practical Fabric-supported Minecraft Java release from
  1.14 through current, including patch releases where technically feasible.
- [ ] Build version-family adapters for all Minecraft client APIs that differ
  across releases; never relabel a newer JAR as an older-version build.
- [ ] Produce remapped, Java-compatible release artifacts for each supported
  release family while preserving the one-mod-JAR installation experience.
- [ ] Test every claimed version family in a real Fabric client and publish a
  compatibility matrix listing supported versions and documented exceptions.
- [ ] Keep the compact, state-aware MCP command tree consistent across version
  families, returning structured unsupported responses only where a vanilla
  capability genuinely does not exist.
