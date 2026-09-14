# fabric-controllerMCP

## Purpose

`fabric-controllerMCP` is a Fabric Minecraft mod that exposes a local Model
Context Protocol (MCP) server. Its tools let an authorized LLM inspect and,
where permitted, control the running Minecraft client and game world.

The project will grow incrementally. We will implement a small, reliable
vertical slice before expanding the API surface.

## Planned API areas

- **World API:** blocks, entities, chunks, dimensions.
- **Player API:** inventory, equipment, health, effects.
- **Navigation:** A* pathfinding, follow player, avoid mobs, travel to XYZ.
- **Interaction:** mine, place, attack, use.
- **Client Control:** keyboard, mouse, camera, screenshots.
- **Mod UI API:** current screen, widgets, text fields, buttons, slots, and
  arbitrary screen interaction.

## Architectural principles

- Keep Minecraft-thread work separate from MCP transport and request handling.
- Favor structured, typed responses with explicit failures over text-only
  results.
- Offer dependable high-level actions alongside carefully scoped low-level
  primitives.
- Treat any state-changing tool as sensitive: validate input, report its
  effects, and make its permission model explicit.
- Prefer server-authoritative game APIs for game actions. Client-input control
  is a separate layer for cases where direct APIs are not suitable.

## Out of scope for the initial slice

- Every API area listed above.
- Remote/network-exposed MCP access.
- Autonomous long-running gameplay without explicit tool calls and safeguards.
