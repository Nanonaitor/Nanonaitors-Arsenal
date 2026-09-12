# Arsenal 1.12.2 configuration (2.0.4)

File: `<Minecraft profile>/config/nanonaitors_arsenal.cfg`.
In game: **Mods → Nanonaitor's Arsenal → Config**. The category pages contain
true/false buttons, number fields, and editable effect/entity lists.

Restart Minecraft/server after edits. Use matching settings on server and clients.
Weapon switches disable only crafting/upgrade recipes. Existing weapons remain
registered, visible, and usable, including creative and JEI item listings.
Back up worlds before disabling enchantments: those are **not registered** when
disabled, so Forge can report missing enchantment entries and remove them from saves.

## Weapons and enchantments

All switches default to `true`.

| Category | Switches |
| --- | --- |
| weapons | morningStar, scimitar, bladeStaff, claws, flail, batteringRam, ballAndChain, tartsyShield, sunWarBulwark |
| enchantments | enableLongChain, enableRotationForce, enableRecovery, enableBreeched |

Turning a weapon off disables its crafting and Dragon Forge upgrade recipes.
Existing items, automatic Linked Claw pairing, and furnace recycling still work.
JEI will show the item but not the disabled crafting recipe. Long Chain and
Rotation Force maximum-level settings remain available.

## Effect lists

Each line is `modid:potion@level@seconds`. Levels are **one-based**: `2` means II.
Use `tier` in the level or seconds field to retain that proc's calculated default.
An empty list, or blank entry, applies no potion. Unknown/malformed entries are
skipped with a warning rather than crashing; optional SRP effects require SRP.

Example inside the generated `effects` category:

```text
S:scimitarHit <
    minecraft:weakness@tier@10
    minecraft:slowness@1@3
>
S:flailHit <
>
```

| Setting | Default and trigger |
| --- | --- |
| scimitarHit | Weakness, tier/set-dependent level, 10 seconds; confirmed scimitar melee hit |
| morningStarFracture | `nanonaitors_arsenal:armor_fracture@tier@tier`; original fully-charged fracture trigger, tier cap and player/mob duration |
| morningStarStun | `nanonaitors_arsenal:stunned@1@3`; original 20% full-charge ability proc |
| ballPlayerFracture | `nanonaitors_arsenal:armor_fracture@tier@10`; full-charge outgoing hit against players |
| bladeStaffReflect | `nanonaitors_arsenal:stunned@1@1`; successful non-melee reflection |
| tartsyDash | `nanonaitors_arsenal:stunned@1@1`; successful dash hit |
| venomHit | Poison III, 10 seconds |
| icedDragonboneHit | Slowness III and Mining Fatigue III, 5 seconds |
| livingMorningStar / livingClaws / livingFlail | SRP Corrosive / Bleed / Antimall, I for Living or II for Sentient, 10 seconds |
| livingBallAndChain | SRP Debar, I/II for 10 seconds; parasites only |
| clawsHit / flailHit / ballAndChainHit / batteringRamHit / morningStarHit / bladeStaffHit | Empty; optional additional confirmed-damage effects |

Changing the potion lists does **not** change direct damage, piercing, permanent
mob armor fracture, fire, Ice & Fire's separate freezing, lightning, or the
temporary SRP adaptation-bypass hook. Vanilla enchantment effects and reflected
enemy effects also remain separate. Fixed tooltip descriptions describe defaults.

## Reach

| Setting | Default (blocks) |
| --- | ---: |
| reach.flailRadius | 4 |
| reach.ballWindupReach | 3 |
| reach.ballThrowReachPerCharge | 4 |

Attribute/quality reach bonuses and Long Chain still add to these values.
Ball throws use base-per-charge × effective charge, then add bonuses.

## Shields and Stunned immunity

`shields.allowShieldEnchantments=true` permits Arsenal's shield-specific
enchantments and compatible shield enchantment types from other mods. `false`
blocks their normal enchanting/book application; Unbreaking and Mending remain.
It does not erase existing enchantments or prevent administrator NBT commands.

```text
stunned {
    S:entityBlacklist <
        minecraft:wither
        minecraft:ender_dragon
    >
}
```

The blacklist means **cannot be stunned**, not an allowlist. Use exact entity
registry IDs. Default is empty. It also rejects externally applied Arsenal Stunned
and restores AI if an already-stunned entity becomes exempt.

## RLCraft armor sets

These bonuses require SetBonus **and detection of RLCraft's configured sets and
bonuses**, not just wearing visually similar armor in standalone Minecraft.

- Silver/Gold Scimitar + matching active set: default Weakness II.
- Silver Ball & Chain + matching active set: charges `1 → 3` (full) in two rotations.
- Gold retains its pre-existing `1 → full` two-rotation behavior with or without
  its set. The matching set also receives full reach/damage on rotation two.

The full-charge bonus is recorded when a rotation completes, and remains stored
for that throw even if the armor is subsequently changed.
