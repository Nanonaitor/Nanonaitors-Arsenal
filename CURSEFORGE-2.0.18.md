# Nanonaitor's Arsenal 2.0.18 — Minecraft 1.12.2

## Blade Staffs

- Renamed Blade Staff registry IDs from `double_bladed_scimitar_*` to `blade_staff_*`.
- Added legacy registry remapping so Blade Staffs in existing worlds migrate to the new IDs instead of disappearing.
- Reflection strength now depends on material tier, with special bonuses for Silver against undead and Gold with its matching armor set.
- Successful reflections consume durability equal to the blocked incoming damage, rounded down, with Unbreaking support.
- Updated Blade Staff tooltips to show each tier's reflection strength.
- Added distinct successful-reflection sounds.

## New weapon enchantments

- **Reverso** — Blade Staff reflections heal the wielder instead of damaging the attacker and repair durability based on the healing delivered.
- **Luna** — increases Morning Star area by 25% and reduces charge time by 25% per level (maximum level II).
- **Magic Log** — halves Battering Ram hunger use and converts its attack damage to magic damage.
- **Endure** — gives each enchanted Scimitar a 25% chance to resist a guard break and reduces failed guard recovery by 25%; dual-wielded copies stack.
- Added configuration switches for each new enchantment and one master switch for all Arsenal enchantments.

## Dual Scimitars

- Reworked automatic dual-wield attacks so each hand takes its own turn and uses its proper attack speed, damage, enchantments, reach, cooldown and RLCombat offhand modifier.
- Fixed scheduled main-hand attacks becoming extremely weak or dealing no damage in obfuscated RLCraft builds.
- Fixed right-click interaction priority for blocks, chests, levers and villagers while dual wielding.
- Prevented extra offhand strikes during cross-guard, shield bash and shield cooldown.
- Shield bash now combines one correctly calculated main-hand hit and one correctly calculated offhand hit.
- Added separate first-person hand animations and third-person crossed-arm support, including Mo' Bends compatibility.
- Added a custom Scimitar bash sound.

## Compatibility and balance

- Flails, Ball and Chains, and Claws can no longer accept So Many Enchantments' Swifter Slashes through either the enchanting table or enchanted books.
- Raised Rotation Force's default maximum level from II to III.
- Arsenal shields now support Thorns when shield enchantments are enabled.
- Added the new weapon effects to configuration and recipe/content registration.

## Notes

- Replace the previous Arsenal JAR; do not keep multiple Arsenal versions installed.
- Restart Minecraft after updating.
- Back up important worlds before changing mod versions.
- Install the same Arsenal version on the client and server for multiplayer.
