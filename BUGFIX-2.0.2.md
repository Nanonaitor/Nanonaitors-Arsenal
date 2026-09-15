# Arsenal 1.12.2 — 2.0.2 test update

This is a compiled test update, not a claim that every interaction has been verified inside RLCraft. No world files are changed. Restart Minecraft before testing it.

## Changes and findings

| Request | Implementation / finding |
| --- | --- |
| LevelUp passive critical hits | Its installed implementation listens for direct player `LivingHurtEvent` damage, not just vanilla sword attacks. Arsenal's direct damage paths qualify. Bulwark's armor-based base damage now runs earlier so it no longer overwrites later critical multipliers. No duplicate critical roll was added. LevelUp still requires its skill to be active and a nonempty main hand; an empty-main-hand/offhand-shield attack does not meet that mod's condition. |
| Dual-scimitar guard causes an attack | Client input suppresses attacks while right-click guarding; server rejects unowned duplicate attacks and attacks during item use. |
| Counter Attack while guarding | Scimitar guard cancellation now runs after SME's NORMAL-priority Counter Attack handler. The enchantment retains its own chance and requirements; it is not guaranteed. |
| Scimitar shield break | Uses the installed Spartan iron shield's durability-derived protection and ShieldBreak's live damage thresholds, parry window, disable chance, cooldowns, knockback, and parry/break effects. This is a compatibility implementation because ShieldBreak itself only accepts ItemShield items. |
| Scimitar durability | Each blade receives `max(1, ceil(blockedDamage / 2))` wear before its usual durability/enchantment handling. For example, 5 damage means 3 wear on each blade. |
| Alternating scimitar properties | Ordinary custom scimitar swings delegate to RLCombat with the actual attacking-hand flag: its weapon stack, damage modifiers, enchantments, critical hooks, durability, and successful-hit callbacks. Without RLCombat, vanilla attack handling is used with the selected weapon. |
| Haste and qualities | Scheduling reads the current attack cooldown, temporarily substituting the offhand attributes when needed. With RLCombat installed, its QualityTools-aware modifier helpers are used. Reach selection also uses the selected hand's attributes. |
| Missing second swing | Each swing explicitly restarts the hand animation independently of whether damage succeeds. Target selection includes collidable multipart entities rather than only EntityLivingBase targets. Lycanites/SRP still require in-game confirmation. |
| Ball & Chain balance | Normal throw charges use 1x / 1.5x / 2x base attack damage. Existing matching-metal set charge skipping is retained. Every tier now has 0.5 base attack speed, before modifiers. Tooltips updated. |
| Morning Star / Desolator | Morning Stars accept enabled SME Desolator. Successful custom hits now call the normal enchantment on-hit hook, which SME injects into for Desolator. Its normal chance/level rules still apply. Added missing Reskillable checks to custom Morning Star controls. |
| Universal curses | Universal ALL-type curses are accepted; SME curses use SME's own eligibility check instead of being rejected for its custom NONE type. This includes applicable Possession, without permitting every armor-only curse or bypassing SME's disabled settings. |
| Blade Staff autoswing | Removed Arsenal's hold-to-autoattack handler and obsolete tooltip. RLCombat or other mods may separately implement their own held-input behavior. |
| Blade Staff orientation | Rotated the shared held-item model's first- and third-person transforms by 180 degrees. Inventory art is unchanged. |
| Blade Staff reflection | Requires a living attacker, positive damage, a blockable non-fire source, and no remaining defender hurt-resistance time. Fall and other attackerless environmental damage are not blocked or reflected. |
| Bulwark Guard Strain | Yellow second item bar above durability in normal inventory/hotbar/offhand displays. Adds one point per blocked hit; loses one point per second while carried. At 25 points, the hit is blocked, guard ends, strain resets to zero, and the shield is disabled for 3 seconds. The duration was an implementation assumption because none was specified. Strain synchronization does not trigger a reequip animation. |

## RLCombat scope

Ordinary alternating scimitar strikes now use RLCombat's native pipeline, including its per-hand cooldown, quality attributes, Reskillable checks, reach validation, pre/post damage events, and enchantment context.

Custom flail/chain/ram/Morning Star area attacks are **not converted to ordinary RLCombat sword attacks**. They retain Arsenal's area geometry, charge multipliers, armor mechanics and timing, and call Forge's living damage pipeline. Missing PvP/spectator checks were added for these paths and linked-claw attacks. Some RLCombat features that only execute inside its ordinary attack helper (such as its custom pre/post attack events and ordinary swing cooldown policy) therefore do not govern every ability. This is partial integration, not blanket compatibility with every combat-mod hook.

## Attached Ball & Chain crash

The inspected trace enters `BallAndChainCombat.applyHit`, then fails in injected `EntityLivingBase.getAtkDmgModifierMsg` / `startMsg` damage-reporting methods with a NullPointerException. This points to an injected damage hook failing during the throw, not to a demonstrated null Arsenal target. The trace alone does not identify which experimental mod injected that method; FermiumASM's appearance as a crash deobfuscator does not establish it as the culprit.

A class-content scan of the recording profile's installed mod jars did not find the `getAtkDmgModifierMsg` method name. The exact experimental crash has not been reproduced in that profile, and there is no evidence yet that it occurs there. Arsenal now restores temporary i-frame/adaptation state in `finally` if another mod throws, but deliberately does not swallow or retry the failed hit. **This hardens cleanup; it is not a verified fix for the external injected NullPointerException.** The attached crash file later became unavailable at its supplied path, so deeper identification requires that report/experimental mod list again.

## Verification and focused play-test

- Gradle compilation/reobfuscation succeeded; 60 configuration/charge checks, 35 sound checks, and 14 guard-rule checks passed.
- Static inspection used the recording profile's RLCombat 2.2.4, SME 1.0.8, ShieldBreak 1.1.3 and Spartan Shields 1.5.5 implementations.
- No automated client/world play-test has been performed.
- Test two different scimitar tiers/enchantments on a normal mob, Lycanites mob and SRP mob. Compare each blade's durability, effects and damage; retry with Haste/qualities.
- Guard near a target without clicking attack; confirm no outgoing ordinary hit. Test Counter Attack over multiple incoming hits and shield break outside the initial parry window.
- Test Blade Staff against fall damage, an incoming melee hit, and another hit during hurt resistance.
- Compare all three Ball & Chain charges, including a matching gold/silver set.
- Enchant a Morning Star with Desolator and test repeated successful hits; try Possession on the weapon families.
- For Bulwark, watch the yellow bar, let it decay, then use a sufficiently rapid stream of hits to reach 25 before decay. Confirm the 3-second guard disable.

Source safety copy: `tmp/bug-audit/before-2.0.2-20260914-111342.zip`.

## Installed test build

Installed and hash-verified in `C:/Users/vnano/curseforge/minecraft/Instances/RLCD 1.1 (RECORDING)/mods/nanonaitors-arsenal-1.12.2-2.0.2.jar`.

The previous 2.0.1 JAR was backed up to `tmp/bug-audit/recording-profile-before-2.0.2/` before removal from that profile. Only one Arsenal JAR remains there. No GitHub push or CurseForge publication was performed.
