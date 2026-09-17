# Arsenal 1.20.1 v0.1.1 — comparison with 1.12.2 v2.0.6

This update applies the relevant recent 1.12.2 fixes to the independent Forge
1.20.1 port. It is not a claim that every RLCraft-only integration exists here.
The 1.12.2 source and installed 1.12.2 build were not changed.

## Changes and focused tests

| Area | Change | How to test |
| --- | --- | --- |
| Ram controls | Charge moved from Attack to Use/right-click. Ordinary mainhand attacks work with an occupied offhand and apply additional 0.5-strength knockback. Charge remains empty-offhand-only. | Equip Ram + offhand Scimitar. Left-click must hit normally; right-click must attack with the Scimitar. Empty the offhand and hold right-click: charge, terrain breaking, hunger consumption and camera locking should work. |
| Morning Star / Flail + Scimitar | Offhand Scimitar controls work while either mainhand ability has an active-use state. | Hold left-click with each mainhand weapon, face a zombie and right-click the offhand Scimitar. Check damage and offhand durability. |
| Selected-hand calculations | Scimitar damage, reach and attack speed use the selected stack's attribute modifiers. Other attribute modifiers remain included. Calculations no longer change the live player attributes. | Use different tiers and Sharpness levels in each hand, then swap them. Add a Quality modifier and a damage/reach attribute modifier. Compare each blade's damage, effects and durability. |
| Dual attack cadence | Combined per-hand speeds affect the alternating scheduler only, not the player's global attack-speed attribute. Haste increases the scheduler's speed by 20% per level. | Hold attack with two Scimitars, repeat with Haste II, and swap one blade for a differently modified blade. Verify alternation continues and the ordinary attack-speed attribute is not permanently inflated. |
| Independent immunity | Offhand Scimitar hits have their own hurt-immunity window. Their damage can land during the mainhand window, but rapid repeated offhand hits still respect the offhand window. State is restored in finally. | Attack one durable target with both blades. Compare against repeated same-hand hits. This is not the Claws' unconditional fully-charged paired-hit piercing. |
| Multipart targeting | Scimitar ray selection accepts Forge PartEntity hitboxes with living parents and forwards the hit to the actual part. Selected-hand reach is wall-clipped. | Test a multipart modded boss from different angles. Also put a solid wall between you and a mob: the Scimitar must not reach through it. Unusual non-Forge multipart implementations need separate testing. |
| Cross-guard input | Paired guarding rejects ordinary attacks and stops stale block-mining state. Single Scimitars no longer advertise a fake BLOCK animation. | Start mining, switch to two Scimitars, then attack and guard. A single Scimitar should not bob in and out of a fake block. |
| Guard durability | Each Scimitar takes max(1, ceil(blocked damage / 2)) wear before ordinary durability/enchantment handling. | Block a known 5-damage hit: expect 3 wear per blade without Unbreaking. |
| Guard disable | Both blades receive cooldowns and cannot re-enter blocking/animation during disable. Forge's canDisableShield check receives Spartan's iron_basic_shield when installed, otherwise vanilla shield. Uses modern axe-style chance and a 5-second disable, not legacy ShieldBreak thresholds. | Have an axe attacker repeatedly hit your guard. After disable, hold right-click: crossed guard must remain unavailable until cooldown ends. |
| Scimitar bash | Attack while cross-guarding deals one combined hit using both stacks' attribute/enchantment damage, invokes both stacks' enchantment callbacks, applies wear to both, plays a sword sweep sound and gives both blades a 30-tick cooldown. | Guard with differently enchanted blades and left-click a nearby mob. Check one combined hit, sword audio, both durability values and 1.5-second guard lockout. |
| Real-shield priority | A real shield in the offhand owns Use instead of competing with Arsenal mainhand abilities. Raising it cancels Ball & Chain winding/flight rather than preserving conflicting state. | Test Scimitar + vanilla/Spartan shield and Ball & Chain + shield. Hold Use and try the installed shield's bash controls. Arsenal must not launch its mainhand ability. Native third-party bash behavior still belongs to that mod. |
| Ball & Chain balance | Throw multipliers are 1x / 1.5x / 2x; all tiers have 0.5 base attack speed. Existing gold/silver charge skipping and occupied-offhand acceleration restriction remain. | Compare three charge levels against an unarmored target; then test gold and matching silver armor. Right-click acceleration must require an empty offhand, not disable ordinary throws. |
| Ball & Chain guard visual | Guarding uses idle GUI art, hides the ordinary held sprite in first/third person, and draws a separate lowered first-person ball at [0,-0.52,-1.05]. | Watch inventory/hotbar while guarding: no animated attack icon. In first person see only the lowered ball; in third person see the carried ball without a duplicate hand sprite. Attack animation should still appear during winding/flight. |
| Blade Staff | Arsenal auto-swing removed. Held poses flipped 180 degrees; third-person translation moved to [0,2.5,0]. Reflect/spin animation remains separate. | Normal clicks attack; holding must not trigger Arsenal's removed autoattack. Check both hands and perspectives for position/alignment. Other mods can still implement their own autoattack. |
| Staff protection | Combat damage is blocked before return damage is attempted. Defender i-frames suppress retaliation, not protection. Environmental damage such as fall/lava is excluded. Failed retaliation does not consume the successful-reflection cooldown. Hurt-event fallback blocks direct hurt-event combat paths without duplicate reflection. | Compare melee, arrows, attacker-attributed magic, fall and lava. Repeated combat hits during defender immunity should be blocked without repeated reflected damage. Direct health edits outside Forge damage events are not covered. |
| Bulwark bash | Bash uses full 1 + armor points regardless of the normal attack charge bar; later damage handling no longer replaces it with charge-scaled damage. | With 20 armor points, bash an unarmored target at an empty attack bar: base damage should be 21 before target reductions or external modifiers. Ordinary non-bash attacks still use cooldown scaling. |
| Bulwark Guard Strain | Yellow second item bar; 25 blocked hits disable it for 3 seconds and reset strain. One strain point decays each second while carried. Cooldown also applies to inventory/menu guard. | Block rapidly enough to outrun decay. Watch the yellow bar rise, reset and lock out guard at 25. Stop blocking and confirm 1 point/sec decay. |
| Target safety | Custom attacks exclude spectators/allies and honor player-vs-player permission/server PvP setting. | On a test server disable PvP; abilities should not damage another player. Test allied teams as well. |
| Claws mining | Server mining cancellation and item block-attack permission both reject Claws/Linked Claws. | Try mining stone with Claws; combat controls must not break it. |
| Staff sweep overlap | Blade Staff cannot perform vanilla sword sweeping, and rejects new Sweeping Edge/Arc Slash book application. Existing enchantment data is not deleted. | Attack a cluster: only the staff's intended splash should occur. Try applying a Sweeping Edge book in an anvil. |
| Damage ordering | Ordinary Bulwark base damage is set at the high-priority damage stage, before later critical/damage multipliers. Strain-only item updates do not trigger reequip animation. | Check a primed Tartsy critical followed by a Bulwark hit; watch idle strain decay for item bobbing. |

## Already present; retained

- Per-weapon crafting switches keep existing items registered and usable.
- Configurable effect lists, reach settings, Stunned entity blacklist, shield
  enchantment controls and Arsenal-enchantment enable settings.
- Matching gold/silver armor bonuses and configured Weakness effects.
- Linked Claw creation/removal, shared durability/item data, charged combo rules.
- Ball & Chain armor piercing/fracture does not permanently damage dropped armor.
- Morning Star charge/swing animation and Blade Staff timed reflection visuals.
- The 1.20.1 artwork, tiers and existing active-icon lighting fixes are preserved.

## Version-specific differences / not claimed

- The installed Restless Horizons mod list does not contain the 1.12.2 RLCombat,
  LevelUp2, SME or Better Survival integrations. Their private hooks, Counter
  Attack skill rolls, Passive Critical skill rolls, Desolator and nunchaku Combo
  are not silently recreated or registered as substitute enchantments.
- Normal mainhand attacks use the modern game's combat pipeline. Custom ability
  damage and Scimitar alternation invoke Forge damage events and selected-stack
  enchantment callbacks, but are not a claim of full compatibility with every mod
  that intercepts Player.attack or reads only the player's physical mainhand.
- Universal curses that already permit these SwordItem-based weapons remain
  governed by modern enchantment eligibility. SME-specific Possession eligibility
  cannot be exercised without an equivalent modern enchantment implementation.
- The legacy Reskillable Defense-16 equip rule is not a standalone player skill
  system. No 1.20.1 skill-mod substitute or new Defense attribute is invented here.
- The 1.20.1 configuration is a Forge common TOML; it is not the 1.12.2 config GUI.
  Enchantment toggles retain this port's registration/application behavior rather
  than deleting enchantment IDs from existing worlds.
- Spartan Shields exists here, but the 1.12.2 ShieldBreak mod's durability-derived
  iron shield power and custom thresholds are not ported. The modern fallback is
  described above; it must not be advertised as identical legacy behavior.
- The 1.12.2 experimental injected Ball & Chain crash is not reproduced here;
  no claim is made that updating this port repairs an external injected hook.
- The self-closing development client smoke test passed, including 255 registered
  item models, arm-pose initialization, animated GUI lighting and hidden active
  chain-weapon hand sprites. Manual visual screenshots/full Restless Horizons gameplay were not run.
  Automated GameTests and resource validation do not establish modpack-wide parity.

## Files and safety

Validation: 18 server GameTests passed; the resource audit found zero missing
Arsenal references across 692 models. The client smoke test passed as described
above. See `parity-release-validation.log` and `parity-client-smoke.log`.

- Source safety copy: `profile-backups/before-1.12.2-parity.zip`.
- New artifact: `build/libs/nanonaitors-arsenal-1.20.1-0.1.1.jar`.
- Protocol changed from 2 to 3 for the new Scimitar bash packet. A multiplayer
  client and server must both update; do not mix old and new Arsenal builds.
- Restart Minecraft to load the updated code. No new world is required; back up
  the world and use a copy for balance/compatibility testing.
- No GitHub or CurseForge publication is part of this comparison task.

Installed and hash-verified in Restless Horizons: only
`nanonaitors-arsenal-1.20.1-0.1.1.jar` remains in that profile's mods folder.
The previous `0.1.0-beta.7` JAR is backed up in
`profile-backups/restless-before-0.1.1/`. Final packaging succeeded; see
`parity-final-package.log`.
