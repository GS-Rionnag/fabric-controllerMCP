# Decisions

## Confirmed

| Topic | Decision | Date |
| --- | --- | --- |
| Project purpose | Fabric mod exposing an MCP server for LLM use | 2026-09-13 |
| Delivery strategy | Add capability areas step by step | 2026-09-13 |
| Continuity | Keep in-repository living documentation for fresh chats | 2026-09-13 |
| Compatibility direction | Prioritize the widest practical Minecraft version coverage; use an explicit compatibility matrix and version-family adapters rather than assuming a single binary can support all versions | 2026-09-13 |
| Game edition | Minecraft Java Edition only | 2026-09-13 |
| Compatibility floor | Aim to support every practical Fabric-supported Minecraft Java release from 1.14 through current, including patch releases where technically feasible; publish an explicit tested compatibility matrix and document exceptions. Minecraft 1.8–1.13 is out of scope. | 2026-09-13 |
| Distribution experience | Deliver one mod JAR that users place in their mods folder. The JAR must detect the running game and activate only the applicable compatibility code; do not require a separately downloaded mod binary per Minecraft version. | 2026-09-13 |
| Loader prerequisite | Users install the matching Fabric Loader for their Minecraft profile; the controller is a Fabric client mod, not a vanilla bootstrapper | 2026-09-13 |
| Fabric API | Optional runtime enhancement, not a required dependency. The core must run with Fabric Loader alone; Fabric API integrations activate only when its matching version is installed. | 2026-09-13 |
| Primary MCP client | Prioritize a ChatGPT MCP app/integration for first use, while retaining standards-compatible MCP behavior for other LLM clients | 2026-09-13 |
| Reachability and audience | Make the project publicly usable. Target ordinary cloud ChatGPT conversations, including mobile use. Do not operate a central backend: each user creates an OpenAI Secure MCP Tunnel and runs its `tunnel-client` beside their private mod-hosted MCP server. Provide a secure, simple setup guide. The tunnel is for private/developer-mode connections, not public plugin submission or directory distribution. | 2026-09-13 |
| Interactive MCP UI | Include MCP-UI/MCP Apps-compatible interactive tool UIs. The Java mod emits standards-compatible UI resources from its single JAR; do not require a Node.js sidecar. Use the ChatGPT Apps SDK compatibility path while retaining a useful non-UI response for other clients. | 2026-09-13 |
| Tunnel onboarding | Ship a clear manual OpenAI Secure MCP Tunnel setup guide first. An explicitly opt-in one-click helper that downloads/starts the official `tunnel-client` is a deferred optional enhancement, not a prerequisite for the first release. | 2026-09-13 |
| Multiplayer availability | Expose the same supported MCP capability surface in single-player, LAN, and multiplayer worlds; do not impose an owner-server-only product restriction. Actions must continue to respect normal Minecraft server authority, permissions, protection, and anti-cheat behavior; bypassing them is out of scope. | 2026-09-13 |
| In-game permission UX | Do not require an in-game master switch for MCP actions after the user intentionally connects their tunnel. Keep in-game UI cosmetic/status-oriented initially; retain technical safeguards such as validation, bounded execution, structured outcomes, and normal server authority. | 2026-09-13 |
| First vertical slice | Start with in-game client UI control before world inspection or gameplay actions. Cover title-screen and menu flows through a generic screen API plus high-level common commands: join server, create world, and inspect/change settings and controls. Minecraft Launcher/login UI is out of scope because the mod runs only after Minecraft starts. | 2026-09-13 |
| UI data and control scope | The connected MCP client may inspect and interact with every in-game UI field, including chat and other mods' text fields. Treat the tunnel connection as a high-trust boundary and clearly disclose that visible UI data can be returned to the connected ChatGPT account. | 2026-09-13 |
| Development anchor | Build and validate the first implementation against Minecraft Java 26.2, then add version-family adapters and compatibility testing for earlier Fabric-supported releases. Minecraft 26.2 ships unobfuscated code, so use Fabric Loom's non-remapping plugin and no mappings artifact; pin the exact Fabric Loader, optional Fabric API, Gradle wrapper, and Java toolchain during scaffolding. | 2026-09-13 |
| Operating systems | Support Windows, macOS, and Linux from the first public release. Keep the core mod JVM-portable; document platform-specific OpenAI `tunnel-client` setup and make any later helper choose the matching official binary. | 2026-09-13 |
| Screenshots and mod UI | Provide a screenshot tool as an early general observation capability for menus and gameplay. Complete reliable vanilla UI control first; defer richer custom-mod UI support and any vision/coordinate fallback to a later compatibility expansion. | 2026-09-13 |
| Project distribution | Develop publicly as an open-source project with publicly available source code and mod releases. No project-operated MCP hosting is implied; each user's OpenAI Secure MCP Tunnel remains private to their own account and machine. | 2026-09-13 |
| License | MIT; welcome outside contributions and downstream reuse | 2026-09-13 |
| Initial release channel | Publish source and release artifacts through GitHub Releases first; defer Modrinth and CurseForge distribution until the core path is stable | 2026-09-13 |
| Concurrent clients | Support one active Minecraft client per user/tunnel initially; defer multi-client routing and selection to a later expansion | 2026-09-13 |
| Raw client input | Add raw keyboard, mouse, and camera control later as a fallback for cases not covered by semantic/high-level tools. Prefer semantic tools by default because they are more reliable and token-efficient. | 2026-09-13 |
| Long-running work | Permit exactly one active autonomous job per Minecraft client. Support two modes: detached/background, which continues after its chat turn ends and reports through status; and attached/foreground, which is bound to the active MCP request and stops when that request is cancelled or its connection ends. Both stop on completion, explicit cancel, stuck/failure, game shutdown, or tunnel loss. Validate OpenAI tunnel cancellation propagation during integration testing. UI-action timing remains separate research. | 2026-09-13 |
| Background-job updates | Emit MCP progress only for an active request whose host supplied a progress token. Persist a bounded job event log for later status retrieval; do not assume an MCP server can independently inject a new ChatGPT message after a call has ended. | 2026-09-13 |
| Background-job duration | Do not impose a project-level maximum duration. A detached job may run while the Minecraft client and tunnel remain connected, ending only on completion, explicit cancel, or a detected unrecoverable/stuck failure. | 2026-09-13 |
| Conflicting jobs | Reject a request to start a second autonomous job while one is active. After explicit user approval, cancel the active job and replace it with the requested one. | 2026-09-13 |
| Job replacement approval | Require a follow-up confirmation in ChatGPT before replacing an active autonomous job; do not use an in-game confirmation prompt | 2026-09-13 |
| Screenshot retention | Return screenshots directly to the connected MCP client; do not save them to disk by default | 2026-09-13 |
| Persistent logging | Keep no persistent MCP action/audit logs by default. Offer explicitly enabled local diagnostic logging for debugging. | 2026-09-13 |

## Open decisions

| Topic | Why it matters |
| --- | --- |
| Target Minecraft/Fabric/Java versions | Determines project template, mappings, and supported APIs. |
| MCP transport | Defines how clients connect and how lifecycle/security work. |
| Initial vertical slice | Bounds the first implementation and verification work. |
| Permission model | Determines safeguards for state-changing and input-control tools. |
| Single-player vs. multiplayer support | Changes authority, compatibility, and safety constraints. |
