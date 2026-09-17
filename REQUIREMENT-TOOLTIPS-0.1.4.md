# 0.1.4 — Skill requirement tooltip formatting

- Removed the old aqua "Requires Strength ... (JustLevelingFork)" line.
- Added a separate section after the standard item details: a dark-purple
  "Requirements:" heading and a white " - Strength: " label.
- The required level is green when the player's current Strength meets the
  configured value, red when it does not. Unavailable player data is red,
  not a false success. Creative mode does not falsify the displayed skill level.
- Only appears with active JustLevelingFork compatibility. Disabled integration
  or tier requirement 0 adds no Arsenal requirement section.
- Reads current player data whenever the tooltip is generated; no cached color.
- No changes to actual requirements, progression, combat or existing traits.

Test by hovering an iron Arsenal weapon below Strength 8, then at/above 8.
The number stays 8 and changes red to green. Configured tier overrides also
change the number. The section is visible without holding Shift.

Clean Forge GameTests cover below/equal/above thresholds, unknown level, purple
heading and disabled tier. Full Restless Horizons tooltip rendering still needs
an in-game check because the profile's tooltip mods control the final frame.
