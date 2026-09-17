# 0.1.3 — Restless Horizons tooltips and cross-guard bash

## Tooltip reference and changes

Inspected the installed Spartan Weaponry 3.2.1 SwordBaseItem tooltip implementation
and English text, plus Restless Horizons' Spartan Weaponry and Tooltip Overhaul
configuration. Spartan uses a gold Traits heading, Shift expansion and separate
trait descriptions. The profile's tooltip frame/3D preview is owned by its
tooltip mods, not by Arsenal.

Arsenal weapons and shields now show a gold Traits heading, an aqua SHIFT hint,
and short yellow trait names. Shift expands indented gray descriptions; restrictions
use red. Long description lines wrap at word boundaries. Actual Shift state is
used rather than the sneak key binding. Normal item names, enchantment lines,
vanilla attack attributes, skill requirements and modpack frames remain intact.
This follows the installed weapons' information hierarchy, not their implementation.

## Dual-scimitar bash

Equip two Scimitars. Hold Use/right-click to cross-guard, then press Attack/left-click.
The bash combines both blades' attack damage and damage enchantments, applies
successful-hit effects and one durability wear to each blade, and knocks back the
target. It ends guard and applies a 30-tick (1.5-second) cooldown to both blades,
including on a miss. Release Attack before the next bash. It plays a sword sweep
sound, not a shield bash sound.

The existing interaction-event-only input can miss clicks while Minecraft is using
an item. A key-state polling fallback now shares a once-per-press latch with the
event path, preventing duplicate requests and unintended follow-up auto-attacks.
The server still requires a valid paired guard, skill eligibility and no shield
cooldown. Bash targeting now uses the scimitar's attribute-aware, wall-clipped
target finder, including supported Forge multipart entities.

## Validation and in-game checklist

22 GameTests passed, including combined bash damage with an enchanted offhand,
empty attack-bar behavior, wear to both blades, cooldown rejection, single-blade
rejection, tooltip indentation/line length and the previous regression suite.
These are clean Forge automated tests, not full Restless Horizons visual testing.

- Restart Minecraft; keep your existing world.
- Hover each weapon and both shields, then hold Shift. Compare with Spartan weapons.
- Equip two differently tiered Scimitars; guard and click Attack while looking at a mob.
- Holding Attack after the bash should not start repeated swings. Release before retrying.
- Both blades should receive cooldown and successful-hit durability wear.
- Try during a shield-break cooldown: neither guard nor bash should activate.
- Check a single Scimitar and a normal shield still use their existing controls.

No 1.12.2 balance or enchantment changes are included in this 1.20.1 update.
