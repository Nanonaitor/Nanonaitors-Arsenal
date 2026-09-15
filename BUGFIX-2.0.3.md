# Arsenal 1.12.2 — 2.0.3 test update

## Changes

- **Blade Staff / Combo:** a Combo enchanted book can now be applied in a survival anvil. Better Survival hardcodes its enchanting-table eligibility to nunchaku, so this is an explicit anvil bridge, not an assertion that it will roll naturally at the enchanting table. It respects the configured maximum level, enchantment incompatibilities, prior-work cost, book consumption and renaming. Existing staff enchantments are preserved. **This request changes application eligibility only: Better Survival's native nunchaku combo-power mechanic is still nunchaku-specific; no new stacking-damage mechanic was invented for the staff.**
- **Blade Staff position:** both third-person translations moved from `[0, 4, 0.5]` to `[0, 2.5, 0]` in `double_bladed_scimitar_base.json`. This brings the held art closer to the hand. First-person transforms and the separate reflect animation are unchanged. Visual alignment still needs an in-game check.
- **Scimitar disabled guard:** client input, first-person rendering, third-person arm posing and server item-use state now check both blades' cooldowns. Holding right-click during shield-break cooldown no longer requests the crossed visual or permits automatic attacks to resume behind the guard.
- **Scimitar guard audio:** suppresses local vanilla/RLCombat/Dynamic Surroundings swing sounds during guard/cooldown, plus a short release grace period for queued sounds. Shield block/break and hurt sounds are not suppressed. A deliberate new attack ends the grace period.
- **Scimitar shield bash:** left-click while actively guarding with two scimitars. A single server-targeted, wall-clipped hit up to 4 blocks away combines both hands' current attack-damage attributes and target-specific enchantment damage. The successful hit invokes both weapon hit callbacks and enchantment on-hit callbacks, plus fire/knockback support. Both blades receive Spartan Shields' configured bash cooldown (30 ticks by default), and that mod's global bash-disable setting is respected. A missed bash also consumes the cooldown. No ordinary sword swing is sent for the bash.
- **LevelUp shield skill:** the installed ShieldBlockBonus implementation checks `EnumAction.BLOCK`, not `ItemShield`, so active scimitar guard already qualifies. Its chance to negate remaining damage remains governed by the player's enabled skill and level. Hits Arsenal already fully blocks never need that extra damage-negation roll. Disabled guard intentionally does not qualify.
- **Combined autoattack speed:** the effective rates of both weapons are added for the alternating autoattack only. If main/offhand cooldowns are `M` and `O` ticks, the interval is `1 / (1/M + 1/O)`, limited to one swing per tick. Example: two 2 attacks/second weapons produce 4 alternating swings/second. Live haste/attribute/quality effects are included. A temporary, restored attack-speed modifier keeps RLCombat's native per-hand strength calculation aligned with the accelerated ability; normal item stats remain unchanged outside that call.
- **Bulwark bash:** the bash now starts with the full `1 + live armor attribute`, without normal weapon-charge scaling, and uses a distinct damage source so the ordinary Bulwark damage recalculation does not scale it again. Existing passive/critical/damage-event modifiers and the target's defenses can still change actual health lost; this is not armor-bypassing damage.

## Scope and verification

The scimitar bash sums live damage attributes (including equipment/quality modifiers) and enchantment damage from both weapons. Other mods can modify the resulting Forge damage event. This does not guarantee every external mod's special bonus that only exists inside its private ordinary-melee routine will be included; the bash remains a custom ability, not two ordinary attacks.

Build/reobfuscation passed with 113 regression checks (60 configuration/charge, 35 sound classification, 18 guard/timing). No live Minecraft play-test was performed.

Test: block until disabled and keep right-click held; check both perspectives and queued sounds. Then release, guard again and left-click to bash. Compare two different weapon/enchantment sets. Test Bulwark against an unarmored target with known armor points on the player. Apply Combo from a book in a survival anvil.

Source safety copy: `tmp/bug-audit/before-2.0.3.zip`.
