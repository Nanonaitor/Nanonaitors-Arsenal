# CurseForge upload — Forge 1.20.1 v0.1.7

## Upload settings

| Field | Value |
| --- | --- |
| File | `nanonaitors-arsenal-1.20.1-0.1.7.jar` from `build/libs` |
| Display name | Nanonaitor's Arsenal - 1.20.1 - 0.1.7 |
| Release type | Release |
| Minecraft version | 1.20.1 only |
| Mod loader | Forge only |
| Java version | Java 17 |
| Environment | Client and server |
| Minimum Forge | 47.4.18 |
| Required external mods | None beyond Forge |
| Changelog format | Markdown |

## Ready-to-paste changelog

### Nanonaitor's Arsenal 0.1.7 — Minecraft 1.20.1

Requires Minecraft Java 1.20.1, Forge 47.4.18 or newer 47.x, and Java 17.
Install Arsenal on both client and server for multiplayer.

#### Blade Staffs

- Reflection strength now scales with the Blade Staff's material tier.
- Wood reflects a flat 1 damage. Stone reflects 10%; Copper and Bronze 25%; Iron, Gold, Silver, Steel and Umbrium 50%; Diamond and ordinary Dragonbone 75%; top elemental and end-game materials 100%.
- Silver reflects 200% damage against undead enemies.
- Gold reflects 100% damage while its matching gold armor bonus is active.
- Successful reflection consumes durability equal to incoming damage rounded down, with Unbreaking support.
- Added tier-specific reflection values to Blade Staff descriptions.
- Added a louder custom sound for successful reflection.

#### New weapon enchantments

- **Reverso:** Blade Staff reflection heals the user instead of damaging the attacker, removes the projectile-stun effect and repairs durability based on reflected damage.
- **Luna:** increases Morning Star area by 25% and reduces charge time by 25% per level. Maximum level II.
- **Magic Log:** reduces Battering Ram hunger consumption by 50% and converts its damage to magic damage.
- **Endure:** gives each enchanted Scimitar a 25% chance to resist a guard break and reduces failed guard recovery by 25%. Enchanting both Scimitars stacks the effect.
- Added individual configuration switches and one master switch for Arsenal enchantments.

#### Dual Scimitars

- Reworked automatic dual-wield attacks so each hand takes its own turn and preserves its proper damage, enchantments and attack-speed timing.
- Added separate per-hand hit windows so a legitimate alternating attack can land without allowing repeated attacks from the same hand to bypass immunity.
- Fixed paired target selection and offhand attribute calculations.
- Shield bash now combines the correctly calculated damage and enchantment bonuses from both weapons.
- Shield-break checks now use the appropriate material shield tier.
- Added a custom Scimitar bash sound.
- Reduced the Scimitar inventory rendering size by 15% without changing its held rendering scale.

#### Compatibility and balance

- Flails, Ball and Chains, Claws and Linked Claws can no longer receive So Many Enchantments' Swifter Slashes.
- Raised Rotation Force's default maximum level from II to III.
- Arsenal shields now support Thorns when shield enchantments are enabled.
- Living and Sentient weapon durability can follow the installed parasite weapon values.
- Added configurable Arsenal weapon equipment for eligible naturally spawned vanilla humanoids.
- Added custom sounds for Blade Staff reflection, Reverso healing and Scimitar shield bash.

#### Updating

Replace the previous Arsenal JAR and restart Minecraft. Do not keep multiple Arsenal versions installed. Multiplayer clients and servers should use the same version.

Known compatibility note: First Aid intercepts and distributes ordinary healing across body parts. Reverso activates, but its visible healing can be much smaller in packs using First Aid. Dedicated First Aid healing compatibility is planned separately and is not included in this release.
