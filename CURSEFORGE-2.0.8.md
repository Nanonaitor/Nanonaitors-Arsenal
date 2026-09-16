# CurseForge upload — Arsenal 1.12.2 v2.0.8

Upload `build/libs/nanonaitors-arsenal-1.12.2-2.0.8.jar` as a new file in the
existing Arsenal mod project. Do not upload a source archive or replace files
for the other Minecraft versions.

| Field | Value |
| --- | --- |
| Display name | Nanonaitor's Arsenal - 1.12.2 - 2.0.8 |
| Release type | Release |
| Game version | Minecraft 1.12.2 only |
| Mod loader | Forge |
| Java version, if asked | Java 8 |
| Environment, if asked | Client and server |

Built against Forge 14.23.5.2860. Optional integrations remain optional; do not
mark RLCraft, RLCombat, Ice & Fire, Better Survival or So Many Enchantments as
new hard dependencies. Keep any existing project dependency metadata accurate.

## Paste-ready changelog

### Claw balance

- Removed the guaranteed critical every fourth paired hit and critical banking.
- Both hands now share a four-tick minimum between fully charged i-frame-piercing
  hits against the same target, tracked separately for each player.
- Early offhand piercing attempts briefly wait for the next allowed window.
- Existing claws automatically lose obsolete saved critical counters.
- Auto-attacks, linked claws, shared item data and normal critical hits remain.
- Base damage and offhand damage multipliers are unchanged.

### Enchantment compatibility

- Includes the previous enchantment eligibility fix for modded weapon enchants.
- Preserves Morning Star Desolator, Blade Staff Combo and other special rules.

Restart Minecraft after updating. Use the same version on clients and servers.
Existing worlds and items do not need to be recreated.

## Validation

The build and automated claw/enchantment/configuration/guard/sound regressions
passed. This is not a claim of full live RLCraft playtesting. Before public
release, test both held attack buttons against one target, change targets,
and verify ordinary jump/skill criticals still work without a fourth-hit bonus.

Official upload reference:
https://support.curseforge.com/support/solutions/articles/9000197242

No GitHub push, CurseForge upload or test-profile installation was performed
as part of this request.
