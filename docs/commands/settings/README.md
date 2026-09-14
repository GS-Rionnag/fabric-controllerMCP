# Settings

Settings are available in both `mainmenu` and `ingame`. They use direct
Minecraft client APIs and do not open or navigate the Options screen.

## Options-style sections

| Section | Discover commands | Notes |
| --- | --- | --- |
| FOV | `settings.fov.list` | Field of view and FOV effects. |
| Online | `settings.online.list` | Online/presence preferences. |
| Skin Customization | `settings.skin_customization.list` | Direct options available to the client. |
| Music & Sounds | `settings.music_and_sounds.list` | Includes `sound.music`. |
| Video | `settings.video.list` | Direct video/render preferences. |
| Controls | `controls.list` | Key bindings are a dedicated direct API. |
| Language | UI fallback | This vanilla flow is currently screen-only. |
| Chat | `settings.chat.list` | Chat display and behavior preferences. |
| Resource Packs | UI fallback | This vanilla flow is currently screen-only. |
| Accessibility | `settings.accessibility.list` | Direct accessibility preferences. |
| Telemetry | `settings.telemetry.list` | Telemetry preference. |

Credits & Attribution is intentionally not a controller category.

## Common commands

| Command | Required arguments | Purpose |
| --- | --- | --- |
| `settings.list` | none | Lists all direct settings. Prefer a section list to keep results small. |
| `settings.<section>.list` | none | Lists only one section from the table. |
| `settings.get` | `settingId` | Gets one setting. |
| `settings.update` | `settingId`, `value` | Changes and saves one setting live. |
| `controls.list` | none | Lists all vanilla key bindings. |
| `controls.update` | `keybindingId`, `key` | Updates and saves one binding. |

## Example: unmute music

```json
{
  "command": "settings.update",
  "arguments": { "settingId": "sound.music", "value": 1.0 }
}
```

Use `sound.music` directly; do not use the UI branch for normal music-volume
changes.

Return to the [command tree](../README.md).
