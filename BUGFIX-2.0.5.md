# Arsenal 1.12.2 — 2.0.5 test update

## Ball & Chain

- Lowered the first-person guarding ball from Y -0.35 to -0.52; distance and scale are unchanged.
- Added guard-only hidden-hand models for all 19 existing tiers. The ordinary held sprite is hidden in both first- and third-person hand transforms while shielding; the separately rendered 3D ball remains visible. Inventory/GUI art remains the original idle sprite.
- An equipped real shield takes right-click priority. Ball & Chain no longer consumes the attack key while that shield is requested/active, allowing the shield mod's normal left-click bash input through.
- Raising a real shield cancels Ball & Chain attack/return state rather than competing for the active hand. Its animation cleanup no longer lowers another item's shield.

## Scimitars and real shields

- Explicitly route right-click to the equipped real shield in scimitar/shield and Ball & Chain/shield loadouts. This keeps offhand-attack handlers from taking over the same click.
- A lone scimitar reports no blocking use action and does not enter a fake guard. The paired-use flag changes only when the equipment pairing changes, not every frame or each block.
- Cross-guard and the combined scimitar bash remain **two-scimitar abilities**. A single scimitar plus a real shield uses the real shield's normal block/bash behavior; an unsupported lone scimitar does not become a shield.
- Ordinary mainhand scimitar attacks are rejected while the real shield is active; native Spartan shield bash uses its own shield damage path and retains priority.

## Blade Staff reflection

The old code rejected `isUnblockable()` and fire-marked damage before cancelling it. Lycanites projectiles can apply a separate armor-piercing magic hit as well as their ordinary hit, so these exclusions admitted damage during the reflection window.

- Cancel incoming combat damage first at the attack event, including armor-bypassing magic and attacker-attributed fire/projectile damage.
- Only then attempt return damage when there is a living attacker, the defender is outside hurt-resistance time, and no reflection is already being resolved. Projectile owner resolution includes throwable, arrow and fireball owners.
- Failed return damage, unknown projectile ownership, or the defender's existing hurt resistance no longer removes the protection itself.
- Retaliation during the return-damage call is blocked but not reflected again, preventing loops.
- Added a final hurt-event cancellation fallback for mods entering that damage stage directly. It blocks without dealing duplicate reflection damage.
- Attackerless environmental damage such as falling/lava/void is still not blocked. Direct health edits or custom effects that bypass Forge's damage events are not guaranteed to be intercepted.

## Verification

The build and existing 113 regression checks passed; 19 guard models were validated for matching idle textures and hidden hand transforms. No live Minecraft play-test has been performed. Verify shield priority and native bash with both scimitar+shield and Ball & Chain+shield, and test the staff against a Lycanites projectile with both armor-piercing and ordinary damage components.

Source safety copy: `tmp/bug-audit/before-2.0.5.zip`.
