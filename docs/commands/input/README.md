# Raw input

`input` is shared between the main menu and in-game states. It sends keyboard
and mouse events through Minecraft's native client input handlers. It is for
cases where no direct semantic command or identifiable UI control exists.

Use direct commands such as `settings.update`, `controls.update`, and future
interaction/navigation commands whenever they cover the requested action.

## Commands

| Command | Required arguments | Purpose |
| --- | --- | --- |
| `input.key_down` | `key` | Hold a keyboard key. |
| `input.key_up` | `key` | Release a keyboard key. |
| `input.key_tap` | `key` | Press then release a keyboard key. |
| `input.text` | `text` | Send Unicode character input to the focused Minecraft field. |
| `input.mouse_move` | `x`, `y` | Move the Minecraft cursor to window-pixel coordinates. |
| `input.mouse_down` | `button` | Hold a GLFW mouse button (0 through 7). |
| `input.mouse_up` | `button` | Release a GLFW mouse button. |
| `input.mouse_click` | `button` | Press then release a GLFW mouse button. |
| `input.scroll` | none | Scroll; optional `horizontalAmount` and `verticalAmount`. |
| `input.state` | none | Reports controller-held keys/buttons and Minecraft mouse state. |

Keyboard values use vanilla names such as `key.keyboard.escape`,
`key.keyboard.w`, and `key.keyboard.left.shift`; underscore aliases such as
`key.keyboard.left_shift` are also accepted. Keyboard commands accept an optional numeric GLFW
`modifiers` bitmask. Raw input is state-changing and may affect chat, menus,
or gameplay, so calls should be deliberate and bounded.

`ui.key_press` has been removed. Keyboard actions belong to this raw-input
branch, while `controls.update` directly changes registered keybinding values
without navigating the Controls screen.

While Minecraft has captured the cursor in a world, `input.mouse_move` creates
a camera delta from the requested coordinate but preserves the real physical
cursor position as the next-event baseline. This prevents a later physical
mouse movement from cancelling or reversing the injected turn. On ordinary UI
screens, it remains an absolute window-coordinate move.

Return to the [command tree](../README.md).
