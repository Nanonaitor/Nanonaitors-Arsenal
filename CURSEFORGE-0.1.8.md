# CurseForge upload — Forge 1.20.1 v0.1.8

## Upload settings

| Field | Value |
| --- | --- |
| File | `nanonaitors-arsenal-1.20.1-0.1.8.jar` from `build/libs` |
| Display name | Nanonaitor's Arsenal - 1.20.1 - 0.1.8 |
| Release type | Release |
| Minecraft version | 1.20.1 only |
| Mod loader | Forge only |
| Java version | Java 17 |
| Environment | Client and server |
| Minimum Forge | 47.4.18 |
| Required external mods | None beyond Forge |
| Changelog format | Markdown |

## Ready-to-paste changelog

### Nanonaitor's Arsenal 0.1.8 — Minecraft 1.20.1

Requires Minecraft Java 1.20.1, Forge 47.4.18 or newer 47.x, and Java 17.
Install Arsenal on both client and server for multiplayer.

Brings over the weapon animations, visuals, sounds and combat changes from the 1.12.2 releases 2.0.21 to 2.0.26.

#### Weapon animations

- New third-person animations for the Battering Ram, Blade Staff, Ball and Chain, Flail, Sun-War Bulwark, Morning Star, Claws and paired Scimitars.
- Battering Ram: carried at the hip like a heavy gun; two-handed, both hands grip its plank handles. Charging pulls back, drives forward and bobs, with a recoil on hits and walls; normal attacks are a quick lunge.
- Blade Staff: held crosswise in both hands and paddled side to side. With an item in the other hand it is gripped at the middle and strikes like a sword.
- Ball and Chain: one arm swings the ball while the free hand holds the slack of the chain; the throwing hand yanks the chain back as the ball returns. While guarding, the ball is held up in front.
- Flail: whirled overhead from a stick handle; the chain arcs over the head, and in first person it circles without covering the view.
- Claws: low ready stance; each strike sweeps in from the side with a torso twist. Both hands can strike at once, every third strike lunges, and slash streaks trail the blades.
- Morning Star: the charge raises it gradually (first and third person), highest exactly at full charge.
- Scimitars: guard crosses both blades; a shield bash slashes both outward (first and third person).
- Sun-War Bulwark: braced with both hands, or carried steadily one-handed.
- Swings follow each weapon's real attack speed, and poses ease between motions.
- New client `animations` config section to turn poses and attack-speed swings on or off.

#### Models and chains

- 3D models use true pixels (no stretched textures).
- Larger, rounder Ball and Chain head with a chain ring facing the chain; it tumbles in flight, and full-charge throws leave a trail.
- 3D chains that alternate like modern Minecraft chains, in iron (red for Living and Sentient); `chainMaterialColors` client option for tier colors. The Flail's chain is lighter.
- New Blade Staff art. Reflecting keeps the tier's sprite, and the hotbar icon spins.

#### Sounds

- Ball and Chain: chain rattle on swings and throws, and a louder clash with dust and sparks when a throw hits a block.
- Battering Ram: creak when a charge starts, bash on hits and walls.
- Claws: metal slash on each hit, ring on every third.
- Subtitles for the new sounds, and a new `sounds.volume` client option.

#### Combat

- Scimitars: blocking an attack while holding a scimitar (dual guard or any shield) makes your next hit or shield bash deal x1.3 (the bash multiplies its combined dual-wield damage). Fades after 5 seconds. Replaces Weakness, which can be added back in `scimitarHitExtra`.
- Ball and Chain: crouch + right-click to guard; standing right-click does nothing by itself, and holding it during a wind-up spins faster.
- Ball and Chain: thrown hits follow the ball as drawn, using a slightly larger area than the ball; block crashes use a slightly smaller one, the ball stops with its surface at the wall, and throws cut short by a block keep their speed.
- Ball and Chain: Knockback shoves blocked melee attackers back without hurting them.
- Defenders: raising a Defender no longer cancels a Ball and Chain wind-up or throw, or a Morning Star charge, and a Defender can be raised mid-attack.
- Any shield-type off-hand item, including the Tartsy Shield and Sun-War Bulwark, takes right-click over an Arsenal weapon.

#### Blade Staff

- 10% larger in hand.
- Recipes use Spartan Weaponry's Pole when installed (Spartan Fire's Witherbone Pole for Myrmex tiers), otherwise a stick.

#### World equipment

- Named mobs and mobs with named or customized gear (names, lore, attributes, unbreakable or other data) keep their weapons; only plain or enchanted gear can become an Arsenal weapon.

#### Updating

Replace the previous Arsenal JAR and restart Minecraft. Do not keep multiple Arsenal versions installed. Multiplayer clients and servers should use the same version.

The scimitar `scimitarHit` effect list is now `scimitarHitExtra` and empty by default, so Weakness stops unless you add `minecraft:weakness@tier@10` back. Animation and sound options are in the new client config, `config/nanonaitors_arsenal-client.toml`.
