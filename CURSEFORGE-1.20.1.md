# First CurseForge upload — Forge 1.20.1

Use the existing Nanonaitor's Arsenal CurseForge project. Add a new main file
for this Minecraft version; do not replace or remove the 1.12.2 files.

## Upload settings

| Field | Value |
| --- | --- |
| File | nanonaitors-arsenal-1.20.1-0.1.6.jar, from build/libs |
| Display name | Nanonaitor's Arsenal - 1.20.1 - 0.1.6 |
| Release type | Release |
| Minecraft version | 1.20.1 only |
| Mod loader | Forge only |
| Java version, if offered | Java 17 |
| Environment, if offered | Client and server |
| Minimum Forge | 47.4.18; state this in the changelog/description |
| Required external mods | None beyond Forge |

1. Open https://authors.curseforge.com/ and select the existing Arsenal project.
2. Open Edit, then Files, then Upload file.
3. Select the built JAR itself, not a source ZIP, project folder or modpack export.
4. Fill in the settings above and paste the changelog below.
5. Review inherited project/file dependencies. Do not apply legacy 1.12.2-only
   required dependencies to this file. Optional integrations are described below.
6. Keep the project license consistent with LICENSE.md (All Rights Reserved).
7. Submit the file for moderation. Wait for approval before announcing availability.

The owner has selected Release status. This does not imply that every modpack
or multiplayer configuration has been tested.

Source branch: https://github.com/Nanonaitor/Nanonaitors-Arsenal/tree/mc-1.20.1

## Ready-to-paste changelog

### Forge 1.20.1 release — 0.1.6

Requires Minecraft Java 1.20.1, Forge 47.4.18 or newer 47.x, and Java 17.
Install Arsenal on both client and server for multiplayer.

- Initial 1.20.1 port based primarily on the mechanics of Arsenal 1.12.2.
- Includes Morning Stars, Scimitars, paired Claws, Flails, Battering Rams,
  Ball & Chains, Blade Staffs, Tartsy Shield, and Sun-War Bulwark.
- Includes seven base material tiers and fourteen optional integration tiers,
  available when the corresponding materials are installed.
- Includes weapon abilities, status effects, enchantments, crafting recipes,
  configurable reach, effect lists, recipe toggles and enchantment settings.
- Includes original-style shield models and active-ability animation overrides.
- Flail and Ball & Chain inventory icons animate during use while their held
  sprites remain hidden; world-space chain/ball visuals remain visible.
- Corrected the active inventory icons' lighting to match ordinary item sprites.
- Ball & Chain uses one world-space attack renderer for first and third person.
- Normal Ball & Chain charging and throwing work with an occupied offhand;
  only right-click charge acceleration requires an empty offhand.
- Shield abilities no longer also activate the main-hand weapon. Tartsy's
  dash uses a shield sound instead of a sword-sweep sound.
- Rebalanced paired Claws: removed guaranteed fourth-hit criticals and saved
  critical counters. Fully charged paired i-frame-piercing hits now share a
  four-tick minimum per player/target. Ordinary critical hits remain available.
- Fixed dual-Scimitar shield-bash input, combined weapon/enchantment damage,
  cooldown handling, and target detection. Improved paired attack timing,
  offhand weapon calculations, blocking durability, and shield-break handling.
- Improved real offhand-shield priority, including switching away from Blade
  Staff reflection without interrupting the newly raised shield.
- Blade Staff no longer auto-swings or reflects environmental damage. Improved
  combat-damage blocking and reflection handling.
- Flails now resume cleanly after reconnecting and correctly allow Defenders'
  compatible guard input instead of treating it as a conventional shield block.
- Living and Sentient weapon durability is read from the installed SRP Spartan
  source weapons, with safe fallback values when that integration is absent.
- Living and Sentient chain weapons now use their red parasite chain artwork.
- Dual-Scimitar shield-break checks now use the matching material shield rather
  than treating every pair as iron.
- Added configurable world equipment: eligible vanilla humanoids that naturally
  spawn with a vanilla sword or axe have a 25% default chance to receive a
  same-tier Arsenal weapon while preserving item data and durability ratio.
- Battering Ram charges with right click; ordinary attacks gain extra knockback.
- Ball & Chain attack speed is 0.5, with throw damage multipliers of 1x, 1.5x,
  and 2x. Improved blocking visuals and offhand interactions.
- Fixed Bulwark bash damage to use 1 + armor points. Added a yellow guard-strain
  bar: 25 blocked hits disable the shield briefly; strain decays over time.
- Added optional JustLevelingFork Strength requirements with configurable tier
  levels and an enable/disable setting. Requirement tooltips use a purple
  heading and green/red levels according to the player's skill level.
- Updated weapon tooltips with compact traits and expanded Shift descriptions.
- Build and all 26 automated gameplay tests passed. Full modpack and visual
  playtesting are still recommended.

Multiplayer, balance, and full modpack compatibility testing
are ongoing. Back up existing worlds before adding or updating mods.

Known differences: Dragonbone/venom upgrades use data-preserving crafting rather
than native Dragonforge processing. Chained secondary lightning damage and some
original SRP interactions are not reproduced. 1.12.2-only integrations such as
Reskillable requirements and race affinities are not included.

Ice & Fire, optional material providers, and SRP-related content are optional,
not mandatory dependencies. The base weapon set works without them.

## Official references

- Upload fields, file release types, loader tags and moderation:
  https://support.curseforge.com/support/solutions/articles/9000197242
- Project page, licensing and submission guidance:
  https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips
