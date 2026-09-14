# Handoff notes

Use this file when resuming work in a new chat.

## Snapshot — 2026-09-13

We are planning `fabric-controllerMCP`, a Fabric Minecraft mod that opens an
MCP server for LLMs. The desired eventual API areas are World, Player,
Navigation, Interaction, Client Control, and Mod UI APIs. The user explicitly
wants capability expansion to happen step by step.

The project now has a minimal Minecraft 26.2 Fabric client-mod skeleton:
`build.gradle`, `settings.gradle`, `gradle.properties`, the Gradle 9.5.0
wrapper, `src/main/resources/fabric.mod.json`, and a client bootstrap class.
It targets Fabric Loader 0.19.5 and Java 25, with Fabric API deliberately
optional. The decision-gated plan is in `docs/IMPLEMENTATION_PLAN.md`.

`./gradlew.bat --no-daemon clean build` was verified successfully on
2026-09-13. Minecraft 26.2 ships unobfuscated code, so the project uses
Fabric Loom's non-remapping plugin (`net.fabricmc.fabric-loom`) with standard
`implementation` dependencies and no Yarn/Mojang development mappings.

## Resume from here

1. Verify a 26.2 development-client launch.
2. Implement the local MCP diagnostic transport and Minecraft-thread bridge.
3. Continue Checkpoint 0 technical decisions only as they become necessary;
   product-level decisions are captured in `docs/DECISIONS.md`.

## Update checklist

Before ending a work session, record:

- What changed and where.
- What was verified and the result.
- The exact active task and immediate next step.
- New decisions, assumptions, limitations, or blockers.
