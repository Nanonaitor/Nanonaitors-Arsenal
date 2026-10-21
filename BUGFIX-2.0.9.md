# RLCraft 2.9.3 compatibility and Blade Staff shield priority

Both client crash reports from 2026-09-16 at 00:48:58 and 00:52:10 in the
RLCraft profile show the same cause: Arsenal's ScimitarAttackBridge expected
Helpers.clearOldModifiers(EntityLivingBase, ItemStack, boolean, boolean, boolean).
The installed RLCombat 2.0.8 exposes clearOldModifiers(EntityLivingBase, ItemStack)
instead. The failure occurred while computing dual-scimitar auto-attack timing.
Those reports used Arsenal 2.0.6, so this was not caused by the new claw nerfs.

The recording profile has RLCombat 2.2.4. Version 2.0.9 supports both helper
signatures, resolves a complete pair before caching initialization, and continues
to use RLCombat's native modifier and attack functions. The older release also
lacks EnchantCompatHandler; bash enchantment callbacks use the existing
equipment-context path when that newer class/fields are unavailable.

Blade Staff was missing from the explicit ShieldUsePriority input handler.
It is now included, with a bound-key fallback and a server-side shield handoff
that clears reflection before the offhand shield processes use. This does not
make two-handed shields usable with an occupied hand if their own rules forbid it.
Removing the shield restores normal empty-offhand reflection controls.

## Testing

- In RLCraft 2.9.3, equip two Scimitars and hold Attack against a mob.
- Repeat in the newer recording profile with mixed weapon tiers and enchantments.
- Guard and bash using two Scimitars; verify both versions avoid missing-API errors.
- Hold a Blade Staff and a normal/Spartan one-handed shield: Use should raise the
  shield, not reflect. Try a rebound keyboard Use key too.
- Test swapping a shield into the offhand during reflection: the shield wins.
- Remove the shield and confirm timed staff reflection still works.

Build/regression checks cover both API signatures and existing functionality.
The real installed RLCombat JAR signatures and legacy helper implementation were
inspected. This is not a full live modpack playtest.

The crash occurred in a multiplayer client. Update Arsenal on both client and
server and restart; no new world is needed. Never replace RLCombat just to match
Arsenal's former API assumption.
