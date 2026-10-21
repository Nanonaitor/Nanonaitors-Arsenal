# CurseForge upload preparation — 1.12.2 v2.0.9

## File and settings

Upload `build/libs/nanonaitors-arsenal-1.12.2-2.0.9.jar` to the existing Arsenal
project as a new main file. Do not upload source ZIPs or overwrite other Minecraft
version files.

| Field | Value |
| --- | --- |
| Display name | Nanonaitor's Arsenal - 1.12.2 - 2.0.9 |
| Release type | Release |
| Minecraft version | 1.12.2 only |
| Mod loader | Forge |
| Java, if requested | Java 8 |
| Environment, if requested | Client and server |
| Changelog format | Markdown |

Built against Forge 14.23.5.2860. No new mandatory mod dependencies. Existing
optional integrations, including RLCombat, Better Survival, So Many Enchantments
and Ice & Fire, remain optional. Do not mark the RLCraft modpack as required.

Author Dashboard > Projects > Arsenal > Files > Upload file. Select the JAR,
enter the settings above and paste the release notes below. Submit only after
your final playtest. Release files are distributed by default after approval.
Official reference: https://support.curseforge.com/support/solutions/articles/9000197242

## Paste-ready release notes

### Nanonaitor's Arsenal 2.0.9 — Minecraft 1.12.2

#### Combat compatibility fixes

- Fixed dual-scimitar auto-attack crashing with the older RLCombat version used
  by RLCraft 2.9.3. Supports both legacy and newer RLCombat modifier APIs.
- Fixed a related dual-scimitar bash enchantment compatibility issue on older
  RLCombat versions.
- Blade Staff now gives an offhand shield priority over reflection, including
  remapped Use controls. Active reflection ends before the shield activates.
- Corrected outdated Blade Staff tooltip text.

#### Claw balance

- Removed the guaranteed fourth-hit critical and critical banking.
- Both hands share a four-tick minimum between fully charged i-frame-piercing
  hits against the same target, tracked separately for each attacking player.
- Early offhand piercing attempts can briefly wait for the shared hit window.
- Obsolete saved claw critical counters are cleared automatically.
- Auto-attacks, linked equipment, shared enchantments/durability and normal
  critical-hit mechanics remain. Base damage is unchanged.

#### Enchantment compatibility

- Restored normal mod-defined weapon enchantment eligibility.
- Preserved Morning Star Desolator, Blade Staff Combo, chain-only enchantments
  and existing weapon-specific restrictions.

**Updating:** Replace the old Arsenal JAR and restart Minecraft. Multiplayer
clients and servers should use the same version. No new world is required.

## Internal pre-release checklist — do not paste as release notes

- Build passed with 1,962 automated regression assertions.
- Verify dual-scimitar auto-attacks and bash in RLCraft 2.9.3 and the newer
  recording profile. Automated API tests do not replace live modpack testing.
- Verify Blade Staff + one-handed shield, including swapping during reflection.
- Verify claws against one target while holding both attack buttons.
- Check normal enchanting and the Desolator/Combo special cases.
- Preserve old public files for existing modpacks; no deletion is needed.
- The JAR is prepared locally. This preparation does not publish to CurseForge
  or push v2.0.9 to GitHub.
