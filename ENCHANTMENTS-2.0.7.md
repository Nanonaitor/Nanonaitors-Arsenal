# Enchantment repair and claw balance proposals

## Enchantments

The shared weapon item used a whitelist of vanilla WEAPON, Unbreaking and
Mending. This rejected custom categories and mod-specific eligibility when
queried through the item. It did not explain literal loss of every vanilla
enchantment: those were already accepted. If even Sharpness is unavailable
in-game, test the updated build and investigate the remaining modpack context.

The replacement asks each enchantment for eligibility. Forge's default method
calls back into the item, so a per-thread identity set detects that re-entry
and uses the enchantment type's normal item predicate, avoiding recursion.
Mod-specific overrides and disabled settings are respected. Special Arsenal
rules remain ahead of this fallback: Desolator for Morning Stars, Combo for
Blade Staffs, chain enchants for Flail/Ball & Chain, and existing sweep limits.
Linked claws remain non-enchantable directly and inherit the main claw's data.
Treasure-only rules and enchantment incompatibilities are not removed.

Validation: build succeeds, 1,824 new eligibility assertions pass, and 113
existing regression checks pass. The optional mods are represented by synthetic
eligibility overrides in these tests; this is not a full live RLCraft test.
The 1.20.1 implementation already uses normal eligibility. Its new all-tier,
all-family test passes along with the other GameTests (20 total). No production
enchantment change was needed there.

In Survival, test Sharpness/Unbreaking at a table, Mending from a book, several
SME weapon books, Desolator on a Morning Star, and Combo on a Blade Staff.
Check armor-only enchantments are rejected. Restart Minecraft after replacing
the JAR; no new world is needed. This build has not been installed into profiles.

## Claw balance proposals — not implemented

Claws already have reduced base damage, but two full enchantment-bearing attack
streams, fully charged i-frame piercing and a critical every fourth charged hit
can scale disproportionately. Preserve auto-attacks, linked equipment and
single-target combos; target scaling rather than making them slow swords.

1. First test: multiply final offhand damage by 0.75. With equal hits this is
   about a 12.5% reduction in combined damage, not a 25% overall nerf. Apply after
   additive weapon/enchantment bonuses so endgame enchantments cannot evade it.
2. Limit paired i-frame bypass to one accepted hit per player/target every four
   ticks. Do not consume cooldown/durability for a rejected scheduled hit; defer
   it. This caps that stream at five hits/second, mainly affecting boosted speed.
3. Reset the fourth-hit counter on target change or two seconds without a hit,
   preventing banking a guaranteed crit for a different target. Preserve the
   fourth-hit reward; do not stack its multiplier with another guaranteed crit.
4. If still too strong, test 1.25x instead of 1.5x for the fourth-hit bonus.
   Its isolated average multiplier changes from 1.125 to 1.0625 (about 5.6% less).

Do not apply every nerf at once. Compare naked and enchanted claws with similar
tier single-target weapons against low/high armor mobs and bosses, both with
and without attack-speed baubles. Record DPS, time-to-kill and damage taken.
