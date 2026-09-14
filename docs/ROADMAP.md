# Roadmap

Status legend: `planned` · `active` · `complete`

## Phase 0 — Foundation (`active`)

- [x] Create continuity documentation.
- [x] Capture a decision-gated implementation plan (`docs/IMPLEMENTATION_PLAN.md`).
- [ ] Choose target Minecraft, Fabric Loader, Fabric API, Java, and MCP
  library/transport versions.
- [ ] Define the initial tool set and permission boundary.

## Phase 1 — Mod and MCP core (`planned`)

- [ ] Scaffold a Fabric client mod.
- [ ] Start and stop a local MCP server with the mod lifecycle.
- [ ] Register tools, validate requests, and return structured results.
- [ ] Bridge MCP work safely onto the Minecraft client thread.
- [ ] Add diagnostics and an opt-in configuration surface.

## Phase 2 — In-game UI control (`planned`)

- [ ] Generic current-screen, widget, focus, click, typing, selection,
  scrolling, and keybinding API.
- [ ] High-level title/menu tools: join server, create world, inspect/update
  settings and controls.

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

- [ ] Keyboard, mouse, camera, and rich custom-mod UI support.
- [ ] Arbitrary supported screen interaction and vision/coordinate fallback.
- [ ] Add capabilities one at a time with tests and documentation.
