# Nanonaitor's Arsenal — Forge 1.20.1

Version **0.1.5**. Independent port targeting **Java 17 / Forge 47.4.18**,
the Forge version installed in Restless Horizons. Core mechanics are based on
Arsenal 1.12.2, with fixes reviewed through 2.0.6. See [PARITY-0.1.1.md](PARITY-0.1.1.md) for changes, tests and compatibility limits.

This release does not claim complete 1.12.2 parity or full modpack certification.
Optional JustLevelingFork Strength requirements, tier defaults and the config switch
are documented in [LEVEL-REQUIREMENTS-0.1.2.md](LEVEL-REQUIREMENTS-0.1.2.md).
This branch is independent of the 1.12.2 and 26.1.2 source trees.

## Install for testing

1. Back up your test world's folder before adding a new mod.
2. Use a **Minecraft Java 1.20.1 Forge** profile with Forge 47.4.18 or later 47.x.
3. Copy `build/libs/nanonaitors-arsenal-1.20.1-0.1.0.jar` into that profile's `mods` folder.
4. Do not include a 1.12.2/26.1.2 Arsenal JAR or a second version of this port.
5. Start Minecraft and find the **Arsenal** creative tab. A new world is not required,
   but a disposable creative world is recommended for the first tests.

The mod belongs on both the client and server for multiplayer. Use the same version
and common configuration on both sides, especially for reach/charge visuals.
Other mods are optional; the base weapons work without Restless Horizons.

## Included weapons and controls

Controls below use the default attack/use bindings (left/right mouse).

| Weapon | Behavior / controls |
| --- | --- |
| Morning Star | Hold Attack to charge, release to sweep. Full charge applies stacking Armor Fracture and a 20% Stunned chance. |
| Scimitar | Weakness on hits. Paired blades alternate attacks; hold Use to cross-guard. A lone offhand Scimitar can attack with Use. |
| Claws | Empty offhand creates a linked matching claw. Attack/Use drive each hand; fully charged paired hits bypass normal hit immunity with a shared four-tick minimum per player/target. No guaranteed combo critical. |
| Flail | Hold Attack for timed area strikes and the rendered chain/spike-ball orbit. |
| Battering Ram | Hold Use with an empty offhand to charge forward and crush eligible blocks. Attack normally for extra knockback. Hunger must exceed three bars; charge direction is locked. |
| Ball & Chain | Hold Attack to wind, release to throw/return. Hold Use while winding for faster rotation. Use while idle with an empty offhand to guard. |
| Blade Staff | Normal attack controls; hits splash nearby enemies. Use for a one-second combat reflection window. Does not block environmental damage. |
| Tartsy Shield | Use to guard; Attack while guarding to dash. A confirmed dash hit primes a critical hit. |
| Sun-War Bulwark | Two-handed guard, armor-scaled bash, passive mitigation and movement penalties. |

### Ball & Chain

| Effective charge | Base reach | Throw damage |
| --- | --- | --- |
| 1 | 4 blocks | 1× |
| 2 | 8 blocks | 1.5× |
| 3 | 12 blocks | 2× |

Gold reaches full power in two rotations. Silver does so with its matching armor
set when the set-bonus option is enabled. Long Chain and live Forge reach attributes
add reach; external attack-speed modifiers affect rotation timing.

Full-charge throws compensate for armor according to tier: wood 25%; stone/copper/
gold/bronze 50%; iron/silver/steel/umbrium 75%; remaining tiers 100%.
This is **not** unrestricted true damage: other mitigation still applies.
Full-charge outgoing hits fracture armor; mob fracture changes the armor attribute,
not equipment durability. Winding respects ordinary hit immunity; thrown passes bypass it.

## Tiers and optional integrations

Seven base tiers: wood, stone, copper, gold, iron, diamond, netherite.

Fourteen original optional tiers: silver, bronze, steel, umbrium, dragonbone,
flamed/iced/electric dragonbone, desert/jungle Myrmex, desert/jungle venom,
living and sentient. The port bundles their Arsenal art and models. Optional tiers
appear in Arsenal's creative tab when their repair materials are available.

- Silver/bronze/steel use Forge ingot tags.
- Umbrium recognizes Defiled Lands Preborn's `defiled_lands_preborn:umbrium_ingot`.
- Dragonbone/chitin/blood/stingers use Ice & Fire item IDs.
- Living materials accept `srparasites:infectious_blade_fragment` or the installed
  SRP Spartans `srp_spartans:infectious_long_blade_fragment`.
- Silver gains damage against undead; Myrmex and blooded Dragonbone keep their
  appropriate creature-family bonuses. Fire burns; Ice calls the installed Ice &
  Fire freeze capability when available. See the electric limitation below.
- Matching gold/silver armor bonuses are rebuilt directly, without requiring the
  1.12.2 SetBonus mod. Scimitars apply Weakness II with the matching full set.
- Living-to-sentient evolution tracks slain `srparasites` entities' maximum health
  and preserves names, enchantments, data and proportional durability.

## Configuration

Generated on first launch: `config/nanonaitors_arsenal-common.toml`.

- `weapons`: crafting switches per family/shield. Disabled recipes do **not** delete items.
- `enchantments`: enable switches and effective Long Chain/Rotation Force caps.
- `reach`: flail radius, Ball & Chain winding reach and throw reach per charge.
- `morningStar`: full-charge duration.
- `compatibility`: shield enchanting, matching armor bonuses, Stunned blacklist,
  and living evolution health threshold.
- `effects`: lists such as `"minecraft:weakness@2@10"` (level II, ten seconds).
  `tier` uses the weapon's computed default; an empty list disables that proc.
  Missing optional effects are skipped instead of preventing startup.

Restart after changing these options. Recipe toggles can also take effect on a
datapack reload; clients must use matching combat settings for accurate prediction.
Enchantment toggles suppress new availability and Arsenal's effect without deleting
the registry entry or existing saved enchantment data.

## Important differences / still needs playtesting

- Blooded Dragonbone and venom upgrades use data-preserving crafting recipes.
  Native Dragonforge processing is **not** integrated in this release: the inspected
  1.20.1 API lacks the original preserve-NBT option. This avoids silently losing
  names, enchantments, quality data and wear during an upgrade.
- Electric Dragonbone has its damage matchup bonus, knockback and a visual-only
  lightning strike. The original Ice & Fire chain-lightning API was not present;
  chained secondary lightning damage is **not** reproduced.
- SRP Spartans alone does not provide the original SRP creature/effect systems.
  Corrosive, Bleed, Antimall, Debar, Rage, Call of the Hive cleansing and evolution
  only operate where their expected registry IDs/entities actually exist. No fake
  vanilla substitutes are presented as those effects. The original temporary
  pre-hit Antimall/adaptation interaction and scent behavior are not ported.
- 1.12.2-only Reskillable defense requirements, race affinities, Distinct Damage and
  the specific Better Combat sound patch are not included. They need matching
  1.20.1 mod APIs, not copies of the old patches.
- No hard dependency on JEI, EMI, Quality Forked, Spartan Weaponry or Defenders.
  Standard recipes, tags and Forge attributes are used. Restless Horizons user
  testing is ongoing; this is not a certification of every modpack interaction.
- Both first/third-person rendering paths compile and all item models bake.
  Active ability poses now override the normal model-animation pass. Further
  hand-placement, multiplayer, balance and shader/animation compatibility testing
  is still needed.

## Build and validation

With Java 17 selected:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient -PclientSmokeTest
python audit_resources.py
```

The opt-in client smoke test checks all registered item models and closes the
development client automatically. Normal Minecraft launches do not activate it.

Verified during development:

- Forge build and reobfuscated JAR packaging.
- Seven isolated GameTests: registry/stats/recipes/tags, Stunned AI restoration,
  fracture without equipment wear, armor-piercing curve, linked claw lifecycle,
  upgrade data preservation, and mixed-tier offhand effect selection.
- 255 registered item models baked in a real development client.
- 671 model JSONs checked for missing Arsenal model/texture references.

Source/config entry points: `CombatEvents.java`, `TierEffects.java`,
`ArsenalConfig.java`, `ClientWeaponRenderer.java`, and the JSON display transforms
under `src/main/resources/assets/nanonaitors_arsenal/models/item`.
