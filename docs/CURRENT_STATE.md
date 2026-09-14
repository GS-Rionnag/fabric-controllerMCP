# Current state

Last updated: 2026-09-13

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
- Minecraft 26.2 ships unobfuscated code. The build therefore uses Fabric
  Loom's non-remapping plugin (`net.fabricmc.fabric-loom`), standard
  `implementation` dependencies, and no development-mappings artifact.

## Active work

An implementation plan is documented in `docs/IMPLEMENTATION_PLAN.md`.
The current anchor choices are captured in `docs/DECISIONS.md`.

## Next milestone

Verify a 26.2 development-client launch. Then implement the local MCP
diagnostic transport and Minecraft-thread bridge.
