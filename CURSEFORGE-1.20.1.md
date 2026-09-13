# First CurseForge upload — Forge 1.20.1

Use the existing Nanonaitor's Arsenal CurseForge project. Add a new main file
for this Minecraft version; do not replace or remove the 1.12.2 files.

## Upload settings

| Field | Value |
| --- | --- |
| File | nanonaitors-arsenal-1.20.1-0.1.0-beta.7.jar, from build/libs |
| Display name | Nanonaitor's Arsenal - 1.20.1 - 0.1.0-beta.7 |
| Release type | Beta |
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

Beta files require users to allow beta updates in the CurseForge app. Do not
mark this as Release solely to make it the automatic download.

Source branch: https://github.com/Nanonaitor/Nanonaitors-Arsenal/tree/mc-1.20.1

## Ready-to-paste changelog

### First public Forge 1.20.1 beta — 0.1.0-beta.7

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
- Build and nine automated gameplay tests passed. Client model checks passed
  for all 255 registered item models before the final input/audio-only patch.

This is a beta: multiplayer, balance, and full modpack compatibility testing
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
