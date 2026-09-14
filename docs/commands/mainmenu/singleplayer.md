# Main menu / Single-player

| Command | Required arguments | Purpose |
| --- | --- | --- |
| `mainmenu.singleplayer.list` | none | Lists local worlds known to vanilla Minecraft. |
| `mainmenu.singleplayer.create` | `name` | Creates and loads a normal-preset world. Optional: `gameMode`, `difficulty`, `seed`, `structures`, `bonusChest`, `allowCommands`, `hardcore`. |
| `mainmenu.singleplayer.load` | `worldId` | Loads an existing local world. |

World creation and loading use vanilla backend flows rather than clicking the
Create World UI.

Return to [Main menu](README.md).
