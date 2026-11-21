# Nanonaitor's Arsenal 2.0.26 — Minecraft 1.12.2

Covers everything since 2.0.21.

## Weapon animations
- New third-person animations for the Battering Ram, Blade Staff, Ball and Chain, Flail, Sun-War Bulwark, Morning Star, Claws and paired Scimitars.
- Battering Ram: carried at the hip like a heavy gun; two-handed, both hands grip its plank handles. Charging pulls back, drives forward and bobs, with a recoil on hits; normal attacks are a quick lunge.
- Blade Staff: held crosswise in both hands and paddled side to side. With an item in the other hand it is gripped at the middle and strikes like a sword.
- Ball and Chain: one arm swings the ball while the free hand holds the slack of the chain; the throwing hand yanks the chain back as the ball returns. While guarding, the ball is held up in front.
- Flail: whirled overhead from a stick handle; the chain arcs over the head, and in first person it circles without covering the view.
- Claws: low ready stance; each strike sweeps in from the side with a torso twist. Both hands can strike at once, every third strike lunges, and slash streaks trail the blades.
- Morning Star: the charge raises it gradually (first and third person), highest exactly at full charge.
- Scimitars: guard crosses both blades; a shield bash slashes both outward.
- Sun-War Bulwark: braced with both hands, or carried steadily one-handed.
- Swings follow each weapon's real attack speed, and poses ease between motions.
- Mo' Bends compatible. New `animations` config section to turn poses on or off.

## Models and chains
- 3D models use true pixels (no stretched textures).
- Larger, rounder Ball and Chain head with a chain ring facing the chain; it tumbles in flight, and full-charge throws leave a trail.
- 3D chains that alternate like modern Minecraft chains, in iron (red for Living and Sentient); `chainMaterialColors` option for tier colors. The Flail's chain is lighter.
- Blade Staff: reflecting keeps the tier's sprite, and the hotbar icon spins.

## Sounds
- Ball and Chain: chain rattle on swings and throws, and a louder clash with dust and sparks when a throw hits a block.
- Battering Ram: creak when a charge starts, bash on hits and walls.
- Claws: metal slash on each hit, ring on every third.
- New `sounds.volume` client option.

## Combat
- Scimitars: blocking an attack while holding a scimitar (dual guard or any shield) makes your next hit or shield bash deal x1.3 (the bash multiplies its combined dual-wield damage). Fades after 5 seconds. Replaces Weakness, which can be added back in `scimitarHitExtra`.
- Ball and Chain: crouch + right-click to guard; holding right-click during a wind-up spins faster. Damage hits use a slightly larger area than the ball, block crashes a slightly smaller one, and throws cut short by a block keep their speed.
- Ball and Chain: Knockback shoves blocked melee attackers back without hurting them.
- Defenders: raising a Defender no longer cancels a Ball and Chain wind-up or throw, or a Morning Star charge.
- Fixed the Tartsy Shield (and other Arsenal shields) dropping right after being raised while holding a Blade Staff.

## Blade Staff
- 10% larger in hand.
- Recipes use Spartan Weaponry's Pole when installed (Witherbone Pole for Myrmex tiers), otherwise a stick.

## World equipment
- Modpack legendary weapons are never replaced: only plain loot can become an Arsenal weapon. Named mobs and mobs with named or customized gear keep their weapons.

## Notes
- Replace Arsenal 2.0.21 with this JAR; do not keep both versions installed.
- Restart Minecraft after updating, and back up important worlds first.
- Install the same Arsenal version on the client and server for multiplayer.
- The scimitar `scimitarHit` effect list is now `scimitarHitExtra` and empty by default, so Weakness stops unless you add it back.
