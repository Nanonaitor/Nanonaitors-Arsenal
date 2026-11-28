# Arsenal 1.12.2 configuration (2.0.29)

File: `<Minecraft profile>/config/nanonaitors_arsenal.cfg`.
In game: **Mods → Nanonaitor's Arsenal → Config**. Settings are grouped by
weapon, with general sections and an Enchantments section. Hover over any
section or option for its full explanation.

Restart Minecraft/server after edits. Use matching settings on server and clients.
Weapon switches disable only crafting/upgrade recipes and loot. Existing weapons
remain registered, visible, and usable, including creative and JEI item listings.
Disabled enchantments stay registered, so saves are safe, but cannot be obtained,
applied, or take effect.

**Upgrading from 2.0.26 or older:** the old `weapons`, `reach` and `effects`
sections are moved into the new per-weapon sections automatically on the first
launch, keeping your values, and then removed from the file.

## Sections

| Section | Settings |
| --- | --- |
| worldEquipment | lootIntegration, lootReplacementChance, mobEquipment, mobReplacementChance, socketMobEquipment |
| animations (client) | weaponPoses, moBendsPoses, attackSpeedSwings, chainMaterialColors |
| sounds (client) | volume |
| stunned | entityBlacklist |
| materials | venomHit, icedDragonboneHit |
| morningStar | enabled, fullChargeSeconds, fractureEffects, stunEffects, hitEffects, livingEffects |
| scimitars | enabled, counterMultiplier, counterWindowSeconds, hitEffects |
| bladeStaff | enabled, reflectEffects, hitEffects |
| claws | enabled, hitEffects, livingEffects |
| flail | enabled, radius, hitEffects, livingEffects |
| batteringRam | enabled, hitEffects |
| ballAndChain | enabled, windupReach, throwReachPerCharge, playerFracture, hitEffects, livingEffects |
| tartsyShield | enabled, dashEffects |
| sunWarBulwark | enabled |
| shields | allowShieldEnchantments |
| enchantments | enableAll, enableLongChain, enableRotationForce, enableRecovery, enableBreeched, enableReverso, enableLuna, enableMagicLog, enableEndure, longChainMaxLevel, rotationForceMaxLevel |

Each weapon's `enabled` defaults to `true`. Turning a weapon off disables its
crafting and Dragon Forge upgrade recipes and its loot. Existing items, automatic
Linked Claw pairing, and furnace recycling still work.

## Effect lists

Each line is `modid:potion@level@seconds`. Levels are **one-based**: `2` means II.
Use `tier` in the level or seconds field to retain that proc's calculated default.
An empty list, or blank entry, applies no potion. Unknown/malformed entries are
skipped with a warning rather than crashing; optional SRP effects require SRP.

Example:

```text
scimitars {
    S:hitEffects <
        minecraft:slowness@1@3
    >
}
```

| Setting | Default and trigger |
| --- | --- |
| scimitars.hitEffects | Empty; optional effects on a confirmed scimitar melee hit. Before 2.0.26 scimitars applied Weakness (`minecraft:weakness@tier@10`); add that line back to restore it. |
| morningStar.fractureEffects | `nanonaitors_arsenal:armor_fracture@tier@tier`; fully-charged fracture, tier cap and player/mob duration |
| morningStar.stunEffects | `nanonaitors_arsenal:stunned@1@3`; 20% full-charge ability proc |
| ballAndChain.playerFracture | `nanonaitors_arsenal:armor_fracture@tier@10`; full-charge hit against players |
| bladeStaff.reflectEffects | `nanonaitors_arsenal:stunned@1@1`; successful non-melee reflection |
| tartsyShield.dashEffects | `nanonaitors_arsenal:stunned@1@1`; successful dash hit |
| materials.venomHit | Poison III, 10 seconds |
| materials.icedDragonboneHit | Slowness III and Mining Fatigue III, 5 seconds |
| morningStar / claws / flail .livingEffects | SRP Corrosive / Bleed / Antimall, I for Living or II for Sentient, 10 seconds |
| ballAndChain.livingEffects | SRP Debar, I/II for 10 seconds; parasites only |
| each weapon's hitEffects | Empty; optional additional confirmed-damage effects |

Changing the potion lists does **not** change direct damage, piercing, permanent
mob armor fracture, fire, Ice & Fire's separate freezing, lightning, or the
temporary SRP adaptation-bypass hook. Vanilla enchantment effects and reflected
enemy effects also remain separate. Fixed tooltip descriptions describe defaults.

## Scimitar counter

Blocking an attack while holding a scimitar (the dual-scimitar guard, a vanilla or
modded shield, or an Arsenal guard) charges the next scimitar hit or scimitar
shield bash. The bash multiplies its combined dual-wield damage.

| Setting | Default |
| --- | ---: |
| scimitars.counterMultiplier | 1.3 |
| scimitars.counterWindowSeconds | 5 |

## Animations and sounds (client)

These only change what a player sees and hears; each player can set their own.

| Setting | Default | Effect |
| --- | --- | --- |
| animations.weaponPoses | true | Third-person weapon poses |
| animations.moBendsPoses | true | The same poses through Mo' Bends when it is installed |
| animations.attackSpeedSwings | true | Swings timed to each weapon's attack speed |
| animations.chainMaterialColors | false | Chains in each tier's material instead of iron (red tendon for Living/Sentient) |
| sounds.volume | 1.0 | Multiplier for Arsenal's own sounds (0 to 2, 0 = silent) |

## Reach

| Setting | Default (blocks) |
| --- | ---: |
| flail.radius | 4 |
| ballAndChain.windupReach | 3 |
| ballAndChain.throwReachPerCharge | 4 |

Attribute/quality reach bonuses and Long Chain still add to these values.
Ball throws use base-per-charge × effective charge, then add bonuses.

## Custom materials

Add your own weapon materials in **customMaterials → materials** (in game: Mods →
Nanonaitor's Arsenal → Config → Custom Materials), then restart. Each line adds
that material's version of every enabled weapon, crafted like the other material
tiers with your ingredient (the Battering Ram and Ball and Chain use its block when
one exists), with the metal tinted your colour. Up to 8 lines.

```text
custommaterials {
    S:materials <
        copper, Copper, #D9824B, ore:ingotCopper, 2, 300, 1.5, 14
     >
}
```

| Field | Meaning |
| --- | --- |
| id | Lowercase letters, digits and `_`. Becomes part of item IDs (`morning_star_copper`), so keep it once a world uses it. |
| Display Name | Replaces "Iron" in item names ("Copper Morning Star"). |
| #RRGGBB | Tint for the metal parts; handles, logs and tendons keep their colours. |
| repair ingredient | `ore:oreDictName` or `modid:item`. The material only appears when it exists. |
| harvest level | 0-3 like wood to diamond (4+ allowed): sets armour piercing, Blade Staff reflection and which blocks the ram breaks. |
| durability, attack damage, enchantability | Like a vanilla tool material (iron: 250, 2.0, 14). |

Invalid lines are skipped with a log warning naming the problem. Removing a line
later leaves that material's items unknown in worlds that have them.

## Reskillable requirements

With Reskillable installed, Arsenal adds level requirements for its weapons itself.
They are in **reskillable → requirements** (in game: Config → Reskillable
Requirements), one per line in Reskillable's own format, and can be switched off
with **reskillable → enabled**.

```text
nanonaitors_arsenal:scimitar_dragonbone:*=reskillable:attack|24
```

- Defaults: 122 lines covering every material from gold and iron up to the
  dragon-blooded tiers (attack 2 to 24; Myrmex and Venom also need magic), plus
  the Tartsy Shield (defense 8, attack 4) and Sun-War Bulwark (defense 16). Wood,
  stone, Living and Sentient have none.
- If the pack's own `reskillable.cfg` already locks an item, the pack's lock is
  kept and Arsenal's line for that item is skipped.
- Lines for items that don't exist (a disabled weapon, a custom material that is
  not configured) are skipped. Custom materials can be added here too.
- Without Reskillable, this section does nothing.

## Enchantments

Hover over each switch in game for what the enchantment does. The same
explanation shows under the enchantment's name when you hover an item or book.

| Enchantment | Effect |
| --- | --- |
| Long Chain | Flail, Ball and Chain: +1 block of reach per level |
| Rotation Force | Flail, Ball and Chain: +0.2 attack speed per level |
| Recovery | Arsenal shields: halves the Tartsy disable and Bulwark bash cooldowns |
| Breeched (curse) | Arsenal shields: doubles those cooldowns |
| Reverso | Blade Staff: reflecting heals you and repairs the staff |
| Luna | Morning Star: +25% smash area, -25% charge time per level |
| Magic Log | Battering Ram: half charge hunger, magic damage |
| Endure | Scimitars: resist and shorten guard breaks |

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

- Silver/Gold Scimitar + matching active set: Weakness II, only if Weakness is
  added back to `scimitars.hitEffects` with the `tier` level.
- Silver Ball & Chain + matching active set: charges `1 → 3` (full) in two rotations.
- Gold retains its pre-existing `1 → full` two-rotation behavior with or without
  its set. The matching set also receives full reach/damage on rotation two.

The full-charge bonus is recorded when a rotation completes, and remains stored
for that throw even if the armor is subsequently changed.
