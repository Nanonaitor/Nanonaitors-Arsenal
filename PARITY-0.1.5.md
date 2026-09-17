# 0.1.5 — Recent 1.12.2 fixes reviewed through 2.0.9

## Ported

- Removed the Claws' guaranteed fourth-hit critical, banking, special crit
  particles/sounds and outdated Combo Critical trait label. Ordinary criticals
  and unrelated Tartsy Shield bonuses are unchanged.
- Both claw hands share a four-tick minimum between confirmed fully charged
  i-frame-piercing hits for each player/target pair. This is 0.2 seconds at 20 TPS,
  not a universal limit on all damage sources or all players combined.
- Early offhand piercing attempts briefly wait for an available window, with
  current target/weapon/skill/shield checks. They do not consume durability or
  offhand cooldown until attempted. Failed offhand hits restore prior i-frames.
- Old player and claw-stack critical counters are removed on server ticks.
- No base damage or offhand damage multiplier nerf. Auto-attacks, paired equipment,
  shared durability/enchantments and reduced knockback remain.
- Fixed staff reflection cleanup on both client and server so it stops only
  staff use, not an offhand shield that has just been raised. Existing shield
  priority remains; shield-specific restrictions/cooldowns still apply.

## Already present or not applicable

- Normal weapon enchantment eligibility was already present in 1.20.1 and is
  covered by the all-tier/all-family enchantment GameTest. No blanket unlock
  or new enchantments were added. Staff sweep restrictions remain.
- The 1.12.2 RLCombat 2.0.8/2.2.4 reflection-API fix is not applicable: that
  integration is not used by the Forge 1.20.1 port.
- So Many Enchantments Desolator and Better Survival Combo bridges are specific
  to their 1.12.2 mods, not available enchantments that can simply be copied here.
- Existing 1.20.1 dual-scimitar bash/input fixes, requirement levels/config and
  purple Requirements tooltip format are preserved.

## Test in Restless Horizons

1. Hold both claw attack controls against one mob, including with speed bonuses.
   Fully charged piercing hits from both hands must share the four-tick window.
2. Confirm the fourth hit has no automatic damage spike or claw-specific crit cue.
3. Switch targets; the hit window must be independent. Normal jump/enchantment
   critical mechanics are not disabled by this change.
4. Start staff reflection, equip a one-handed shield offhand, then hold Use.
   The shield must stay raised after reflection cleanup. Test again without a shield.
5. Recheck mixed-tier dual-scimitar bash and skill tooltip colors.

Automated tests include repeated actual claw damage, shared-hand rejection,
interval boundaries, legacy-counter cleanup and staff-to-shield handoff, plus
the previous suite. Full modpack rendering/input testing is still required.
