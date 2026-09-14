# MCP command reference

The server advertises one MCP tool named `mcp`. Its `command` field behaves
like a command-line path; its `arguments` object supplies the arguments for
that path. Start with an empty call or `help`, then follow the returned branch.

## Command tree

```text
mcp
├── state
├── help
├── settings
├── ui
├── mainmenu
│   ├── singleplayer
│   └── multiplayer
└── ingame
```

- [Shared settings](commands/settings/README.md) — direct vanilla preferences
  and controls in either client state.
- [Shared UI](commands/ui/README.md) — inspect and interact with visible
  screens in either client state.
- [Raw input](commands/input/README.md) — keyboard and mouse events in either
  client state.
- [Main menu](commands/mainmenu/README.md) — single-player worlds and
  multiplayer servers while no world is loaded.
- [In-game](commands/ingame/README.md) — the future home for player, world,
  interaction, and navigation commands.

## Root commands

| Command | Arguments | Result |
| --- | --- | --- |
| `state` | none | Reports `mainmenu` or `ingame`. |
| `help` | optional `path` | Lists the next command level. |

Example root request:

```json
{ "command": "help", "arguments": { "path": "settings" } }
```

The returned command tree is authoritative. This reference documents the
stable design and is updated whenever a branch gains a new capability.
