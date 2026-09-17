# Optional JustLevelingFork requirements — Arsenal 1.20.1 v0.1.2

Detected in Restless Horizons: `justlevelingfork-1.2.2.jar`, mod ID
`justlevelingfork`. Its attack-related aptitude is **Strength**, not Attack.
The Reskillable configuration folder also exists, but no Reskillable JAR was
found; its leftover configuration was not used as the active progression system.

Arsenal reads the player's actual JustLevelingFork aptitude capability. It does
not create a separate skill, modify player XP, or rewrite JustLevelingFork files.
Integration activates only when that mod is loaded and the Arsenal toggle is on.

## Defaults based on this profile

| Arsenal tier | Strength | Comparison |
| --- | ---: | --- |
| Wood, stone | 1 | Vanilla wooden/stone swords |
| Copper | 3 | Spartan Weaponry copper weapons; Ice & Fire copper sword is instead 4 |
| Gold | 3 | Vanilla golden sword |
| Bronze | 4 | Spartan bronze longsword |
| Umbrium | 4 | Defiled Lands Preborn umbrium sword |
| Iron | 8 | Vanilla iron sword |
| Silver | 12 | Spartan silver weapons; Ice & Fire silver sword is instead 8 |
| Steel | 12 | Estimate between iron 8 and diamond 16; no exact steel weapon lock found |
| Desert/jungle Myrmex | 12 | Configured chitin weapons |
| Desert/jungle venom | 14 | Configured stinger weapons; older venom-named entries also exist at 12 |
| Diamond | 16 | Vanilla diamond sword |
| Netherite | 19 | Vanilla netherite sword |
| All Dragonbone variants | 25 | Ice & Fire dragonbone/fire/ice/lightning swords |
| Living | 25 | Editable endgame estimate; no exact parasite weapon lock found |
| Sentient | 30 | Editable endgame estimate; no exact parasite weapon lock found |

Source compared: the profile's
`config/JLFork/justleveling-fork.lockItems.json5`. These are initial Arsenal
defaults, not a continuously synchronized copy of the other mod's item list.
Later changes to that list do not automatically change Arsenal's tier values.

## Coverage

- All tiered weapon families, including generated Linked Claws.
- Ordinary mainhand attacks, offhand Scimitar/Claw controls, custom attack/charge
  abilities and weapon-based guarding are checked. Dual Scimitar abilities require
  both blades to qualify, so a locked blade cannot supply free attack-speed bonuses.
- A qualified lone offhand Scimitar remains usable even if the unrelated mainhand
  weapon is locked. Each selected hand is checked independently in that loadout.
- Server-side validation remains authoritative even if a client sends an ability
  packet manually. Active abilities stop if their requirement is no longer met.
- Tooltips show the Strength requirement; denied use shows a rate-limited message.
- Items are not deleted or forcibly dropped. Creative mode bypasses these locks.
- Untiered Tartsy/Bulwark shields were not assigned arbitrary attack-tier values.
  Existing shield requirements imposed by the profile remain separate.

## Configuration

Active world/server file:

`saves/<world>/serverconfig/nanonaitors_arsenal-level-requirements.toml`

Dedicated server: `<world>/serverconfig/nanonaitors_arsenal-level-requirements.toml`.
Forge synchronizes this server config to joining clients.

```toml
[justLevelingFork]
    enabled = true

[justLevelingFork.strengthByTier]
    iron = 8
    diamond = 16
    netherite = 19
```

Set `enabled = false` to disable Arsenal's added locks. Set any tier to `0` to
disable only that tier. All 21 tier entries are generated with explanatory comments.
This does not disable independently configured JustLevelingFork locks on an item.

The profile's `defaultconfigs/nanonaitors_arsenal-level-requirements.toml` is a
template for worlds that do not yet have this server config. Changing the template
does not overwrite an existing world's file. Exit the world before editing the
active file; restart Minecraft after installing a new JAR. No new world is needed.

## Testing

1. In Survival below Strength 8, attempt iron Scimitar attacks and an iron Ram
   charge. Both should be denied, with the requirement shown in the tooltip/message.
2. At Strength 8, retry. At Strength 7 it must remain blocked.
3. Repeat with Flail, Morning Star, Ball & Chain, Blade Staff and Claws. Try their
   abilities as well as normal hits so packet-driven actions cannot bypass the lock.
4. Use a low-tier mainhand Scimitar and high-tier offhand Scimitar: paired attack,
   guard and bash must require both. Remove the high-tier blade to use the low one.
5. Set `enabled = false` in the active world's config, rejoin and confirm Arsenal
   no longer adds these requirements. Re-enable afterward if desired.
6. With JustLevelingFork absent, Arsenal must load and operate normally.

The installed 1.2.2 API was inspected to verify `AptitudeCapability.get(Player)`
and `getAptitudeLevel("strength")`. Automated tests cover defaults, thresholds,
all family/tier mappings and the absent/disabled gates. Live skill-capability
integration still needs the above Restless Horizons play-test; the clean Forge
GameTest environment does not include JustLevelingFork.

If the installed mod changes its API, the integration logs an error and fails
closed rather than silently letting locked weapons attack. The config switch can
disable it while investigating. No progression data is changed.
