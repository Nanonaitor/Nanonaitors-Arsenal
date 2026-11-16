# Nanonaitor's Arsenal 2.0.20 - Minecraft 1.12.2

## Guards

- Ball and Chain now guards against every attack from any direction, both while guarding and for the whole wind-up. The guard can't be broken. Each block costs durability and plays a metal clang. It does not guard while the ball is thrown or returning.
- A thrown Ball and Chain smashes projectiles along its path.
- The Sun-War Bulwark guard can't be broken. Strain is now display-only, and a bash no longer drops the guard or puts the shield on cooldown.
- Guards now block fire attacks that have an attacker (fireballs, dragon breath) and lightning strikes.

## Mod compatibility

- Fixed missing silver, bronze and steel weapon recipes with mods that add their ingots late, such as HBM's Nuclear Tech Mod. Missing tier recipes and smelting recipes are now registered after all mods have loaded.
- Ball and Chain recipes accept ore-dictionary metal blocks. Bronze heavy weapons use bronze blocks when available and only fall back to ingots when no bronze block exists.
- A full Defiled Lands golden scale armor set now unlocks Arsenal's gold perks, just like the gold armor set.
- Battering Ram block breaking now respects land-protection mods.
- ShieldBreak and RLCombat compatibility no longer crashes if something unexpected happens. It logs a warning once and falls back to normal behavior.

## Balance

- Bronze durability raised from 200 to 350.
- Battering Rams and Balls and Chains have 1.5x durability.
- Weapon damage and attack speed are unchanged.

## Fixes

- Bulwark armor damage only replaces its own melee swing and no longer affects Thorns.
- The scimitar bash cooldown always applies.
- Offhand claw attacks now use vanilla reach and line-of-sight limits.
- Blade Staff splash damage no longer hits teammates, PvP-protected players or your own pets.
- Temporary combat state is no longer saved into player data.
- The Breeched enchantment now displays as "Breached". Its ID and config key are unchanged, so existing items and configs keep working.

## Performance and cleanup

- Fewer attribute and potion-effect updates sent every tick, plus general code cleanup.

## Notes

- Replace Arsenal 2.0.19 with this JAR; do not keep both versions installed.
- Restart Minecraft after updating.
- Back up important worlds before changing mod versions.
- Install the same Arsenal version on the client and server for multiplayer.
