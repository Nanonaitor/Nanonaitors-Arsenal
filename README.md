# Nanonaitor's Arsenal

Nanonaitor's Arsenal is a Forge weapon mod built around specialized combat roles,
active mechanics, and deliberate tradeoffs instead of interchangeable damage tiers.

This branch contains release `1.0.0` for Minecraft 26.1.2 and Forge 64.0.12.
The original Minecraft 1.12.2 release remains preserved on the repository's
`main` branch.

## Included equipment

Every weapon is available in wood, stone, copper, gold, iron, diamond, and
netherite tiers. Copper uses vanilla Copper tool properties. Netherite weapons
upgrade from their matching Diamond weapon with a Netherite Upgrade Smithing
Template and Netherite Ingot, preserving enchantments and custom data.

### Morning Stars

Slow, heavy maces with exceptionally long spikes. Fully charged hits build Armor
Fracture, reducing armor by 20% per level. Tier caps range from 40% for wood to
100% for diamond. The effect lasts 30 seconds on mobs and 10 seconds on players.

### Scimitars

Fast, curved blades inspired by historical Middle Eastern swords and classic
RuneScape silhouettes. Fully charged hits have a 10% chance to inflict Weakness II
for 2 seconds.

### Paired Claws

Two wrist-mounted weapons controlled independently with left and right click.
Fully charged alternating strikes can pierce normal invulnerability frames. The
linked claw mirrors enchantments and durability from the main claw.

### Flails

Hold attack to swing an iron chain and tier-colored head in a four-block area around
the wielder every 25 ticks. Every visible non-allied enemy in range receives a full
melee hit, including off-hand Defender bonuses, Strength, and enchantment damage.

### Battering Rams

Two-handed charge weapons that rush forward and smash a tier-dependent three-by-
three path through terrain. Higher tiers break progressively harder materials.
Charging consumes hunger, requires an empty offhand, and damages the ram per block
or enemy struck.

### Balls and Chains

Hold attack to build up to three charges, then release a collision-limited line
attack that strikes on both the outward and returning passes. Each charge adds four
blocks of reach; Gold reaches full power at two charges. Fully charged throws gain
tier-scaled armor piercing and can permanently reduce mob armor.

### Sun-War Bulwark

A massive two-handed shield that can guard directed attacks from every side. Its
normal strike deals 1 damage plus the wielder's total armor points. Attacking while
guarding performs a four-block AOE bash, lowers the shield, and starts a 3-second
cooldown. Carrying it slows movement by 40%; guarding slows movement by 75%.

Its concept is inspired by the traditional account of El Pípila at the 1810
assault on the Alhóndiga de Granaditas in Guanajuato, Mexico: a miner remembered
for carrying a stone slab on his back as protection while reaching and burning
the fortified building's gate.

## Rendering and controls

- Flails and Balls and Chains use standalone animated 3D combat renders.
- Their held sprites change while attacking.
- Ball and Chain throws follow the exact aimed trajectory in first and third person.
- Paired Claws assign the main and linked claw to separate mouse buttons.
- No animation or compatibility mod is required.

## Enchanting

Arsenal weapons accept appropriate vanilla melee enchantments, including Sharpness,
Smite, Bane of Arthropods, Knockback, Fire Aspect, Looting, Unbreaking, and Mending.
Sweeping Edge is intentionally restricted to Scimitars. The Sun-War Bulwark accepts
compatible shield enchantments.

## Compatibility

This 26.1.2 release is fully standalone and has no required content-mod dependency.
RLCraft-specific material tiers from the 1.12.2 edition are intentionally not
registered because their source mods do not currently target Minecraft 26.1.2.

## Build

Use Java 25 and the supplied Gradle wrapper:

```powershell
.\gradlew.bat clean jar --rerun-tasks --no-build-cache --no-configuration-cache
```

Release JARs are created under `build/libs/`.

## License

Copyright 2026 Nanonaitor. See [LICENSE.md](LICENSE.md).
