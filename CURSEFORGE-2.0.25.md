# Nanonaitor's Arsenal 2.0.25 — Minecraft 1.12.2

Covers everything since 2.0.21 (2.0.22 to 2.0.25).

## Weapon animations

- New third-person animations for the Battering Ram, Blade Staff, Ball and Chain, Flail, Sun-War Bulwark, Morning Star and paired Scimitars.
- Battering Ram: carried with both hands and pushed forward while ramming. With an item in the other hand it uses the normal animations.
- Blade Staff: held crosswise in both hands and paddled side to side on each attack. With an item in the other hand it is gripped at the middle and strikes like a sword, leading with the blade.
- Ball and Chain: one arm swings the ball in a circle while the free hand holds the slack of the chain. The throw and pull-back follow the ball. While guarding, the ball is held up in front, as in first person.
- Flail: the arm whirls the flail overhead, with the chain running from the hand to the spinning head.
- Sun-War Bulwark: braced with both hands, or carried one-handed with a struggle.
- Scimitars: a guard holds both blades crossed in front, and a shield bash slashes both outward (first and third person).
- Swings follow each weapon's real attack speed.
- Poses ease from one motion to the next instead of snapping.
- Mo' Bends compatible: arms match the vanilla poses, and Mo' Bends keeps animating legs, body and running.
- New `animations` config section to turn weapon poses, Mo' Bends poses and attack-speed swings on or off.

## Chains

- Chains are drawn as 3D links that alternate like modern Minecraft chains, lit by the world around them.
- Chains are dark iron on every tier, as crafted from Iron Chain. Living and Sentient chains are red (tendon).
- New `chainMaterialColors` option (in `animations`, off by default) draws chains in each tier's material instead.
- Ball and Flail heads are now shaded instead of drawn at full brightness.

## Sounds

- A chain rattle plays with each Ball and Chain swing and throw.
- A louder chain clash plays when a thrown ball crashes into a block.

## Ball and Chain

- The thrown ball's hitbox is smaller and follows the ball's drawn path, so hits match where the ball visibly is.
- Defenders: raising a Defender no longer cancels a wind-up or throw. You can guard and keep swinging, as with the Flail.

## Blade Staff

- 10% larger in hand.
- Recipes use Spartan Weaponry's Pole when it is installed (Witherbone Pole for Myrmex tiers). Without Spartan Weaponry they still use a stick.

## World equipment

- Arsenal no longer replaces modpack legendary weapons. Only plain loot (count, damage, random enchantments) can become an Arsenal weapon. Named items, items with lore, custom stats or specific enchantments are left as the pack made them.
- Named mobs and mobs holding named or customized gear keep their weapons.

## Cleanup

- Removed unused claw textures and the old flat chain-link pieces.
- Weapon poses are worked out once per player per frame, and players not holding Arsenal weapons are skipped.

## Notes

- Replace Arsenal 2.0.21 with this JAR; do not keep both versions installed.
- Restart Minecraft after updating.
- Back up important worlds before changing mod versions.
- Install the same Arsenal version on the client and server for multiplayer.
