# Nanonaitor's Arsenal 2.0.26 — Minecraft 1.12.2

Covers everything since 2.0.21 (2.0.22 to 2.0.26).

## Weapon animations

- New third-person animations for the Battering Ram, Blade Staff, Ball and Chain, Flail, Sun-War Bulwark, Morning Star, Claws and paired Scimitars.
- Battering Ram: carried at the hip like a heavy gun. Two-handed, both hands grip its plank handles. Charging pulls back, drives forward and bobs, with a recoil on hits; normal attacks are a quick lunge.
- Blade Staff: held crosswise in both hands and paddled side to side on each attack. With an item in the other hand it is gripped at the middle and strikes like a sword, leading with the blade.
- Ball and Chain: one arm swings the ball in a circle while the free hand holds the slack of the chain. The throw and pull-back follow the ball. While guarding, the ball is held up in front, as in first person.
- Flail: the arm whirls the flail overhead from a stick handle, and the chain arcs over the head instead of through it.
- Claws: a low ready stance; each strike sweeps in from the side across the front. Each hand swings on its own (both can swing at once), every third strike lunges, and slash streaks trail the blades.
- Sun-War Bulwark: braced with both hands, or carried steadily one-handed.
- Scimitars: a guard holds both blades crossed in front, and a shield bash slashes both outward (first and third person).
- Swings follow each weapon's real attack speed, and poses ease from one motion to the next.
- Mo' Bends compatible: arms match the vanilla poses, and Mo' Bends keeps animating legs, body and running.
- New `animations` config section to turn weapon poses, Mo' Bends poses and attack-speed swings on or off.

## Models and chains

- 3D models use true pixels: textures are no longer stretched across faces.
- The Ball and Chain head is larger and rounder, with a chain ring that faces the chain. A thrown ball tumbles in flight, and fully charged throws leave a trail.
- Chains are 3D links that alternate like modern Minecraft chains, in iron on every tier (red tendon for Living and Sentient), lit by the world around them.
- New `chainMaterialColors` option (in `animations`, off by default) draws chains in each tier's material instead.
- Claws have curved, shorter blades; the Battering Ram head has a flared ring.
- Blade Staff: reflecting keeps the tier's own sprite, and the hotbar icon spins while reflecting.

## Sounds

- Ball and Chain: a chain rattle with each swing and throw, and a louder chain clash with dust and sparks when a thrown ball crashes into a block.
- Battering Ram: the log creaks as a charge starts and bashes on hits and walls.
- Claws: a metal slash on each hit and a ringing accent on every third.
- New `sounds.volume` option (client) scales Arsenal's own sounds.

## Combat

- Scimitars: blocking an attack while holding a scimitar (with the dual-scimitar guard or any shield) empowers your next scimitar hit or shield bash to deal x1.3 (the bash multiplies its combined dual-wield damage). The charge fades after 5 seconds. This replaces Weakness, which was too strong early and too weak late; it can be added back in the renamed `scimitarHitExtra` list. Tune it in the new `scimitars` config section.
- Ball and Chain: the thrown ball's hitbox follows its drawn path, so hits match where the ball visibly is.
- Ball and Chain: Knockback on the ball also shoves back melee attackers whose hits the guard blocks, without hurting them.
- Defenders: raising a Defender no longer cancels a Ball and Chain wind-up or throw. You can guard and keep swinging, as with the Flail.

## Blade Staff

- 10% larger in hand.
- Recipes use Spartan Weaponry's Pole when it is installed (Witherbone Pole for Myrmex tiers). Without Spartan Weaponry they still use a stick.

## World equipment

- Arsenal no longer replaces modpack legendary weapons. Only plain loot (count, damage, random enchantments) can become an Arsenal weapon. Named items, items with lore, custom stats or specific enchantments are left as the pack made them.
- Named mobs and mobs holding named or customized gear keep their weapons.

## Cleanup

- Removed unused claw textures, ball models and the old flat chain-link pieces.
- Weapon poses are worked out once per player per frame, and players not holding Arsenal weapons are skipped.

## Notes

- Replace Arsenal 2.0.21 with this JAR; do not keep both versions installed.
- Restart Minecraft after updating.
- Back up important worlds before changing mod versions.
- Install the same Arsenal version on the client and server for multiplayer.
- Existing configs: the scimitar `scimitarHit` list is now `scimitarHitExtra` and empty by default, so Weakness stops unless you add it back.
