# UI

The UI branch works in both client states. It is for screen-only vanilla flows
and supported custom-mod screens. Prefer semantic branches such as Settings,
Main Menu, and future In-game commands whenever one exists.

## Workflow

1. Call `ui.current_screen`.
2. Call `ui.inspect` and retain its `screenStateVersion`.
3. Use an action below with the element ID and that version.
4. Inspect again after an action changes the screen or its controls.

## Commands

| Command | Purpose |
| --- | --- |
| `ui.current_screen` | Current screen metadata and state version. |
| `ui.inspect` | Supported screen elements, including text-field values. |
| `ui.click`, `ui.select` | Activate an inspected element. |
| `ui.focus` | Move keyboard focus. |
| `ui.type` | Replace or append text in a text field. |
| `ui.set_slider` | Set a normalized slider value. |
| `ui.choose`, `ui.toggle` | Change cycling controls. |
| `ui.scroll` | Scroll a supported container. |

Actions requiring an element use `elementId` and `screenStateVersion`; stale
versions return a structured failure rather than acting on the wrong screen.

For raw keyboard and mouse events, use the shared [Raw input](../input/README.md)
branch. For changing registered keybind values, use `controls.update` directly.

Return to the [command tree](../README.md).
