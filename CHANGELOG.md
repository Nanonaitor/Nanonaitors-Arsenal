# Changelog

## 1.2.0 - 2026-08-21

- Added native hold-to-auto-attack controls for both paired Claws: left-click
  controls the main claw and right-click controls the linked claw.
- Required a completely charged attack for automatic strikes, i-frame piercing,
  and progress toward the guaranteed fourth-hit critical; rapid manual attacks
  remain possible but cannot trigger those charged bonuses.
- Removed the alternating-hand requirement while retaining the matching paired
  claw and empty-offhand requirements.
- Reduced paired-Claw knockback by 50% so consecutive strikes can remain in range.
- Made main- and offhand held attacks animate on whiffs without playing false hit
  sounds; linked-claw impact audio now plays only after confirmed damage.
- Straightened the standalone Iron Chain sprite and all repeated animated chain
  links used by Flails and Balls & Chains.
- Leveled the Flail's third-person orbit to match the modern horizontal swing
  instead of climbing diagonally above the player.
- Added the standalone Iron Chain item to Arsenal's creative tab.

## 1.1.6 - 2026-08-17

- Added optional Reskillable attack requirements matching equivalent Spartan Weaponry tiers while leaving Wood, Stone, Living, and Sentient unrestricted.
- Prevented custom Flail, Ball & Chain, Battering Ram, and linked-Claw attacks from bypassing active Reskillable requirements.
- Added Spartan Weaponry handles to applicable recipes and Spartan Fire Witherbone Handles to Myrmex recipes when those mods are installed.
- Corrected Myrmex Stinger Flails to use one matching Stinger and one matching Chitin.
- Reworked the Living Ball & Chain recipe around a Living Core, Bolster Husks, and Dried Tendons.
- Improved Ball & Chain close-block collision checks and clarified Gold's two-rotation full charge.
- Confirmed that attack speed, Haste, and Rotation Force scale Ball & Chain wind-up, outward travel, return travel, hit timing, and animation speed.

## 1.1.0 - 2026-08-13

- Added a guaranteed critical to every fourth fully charged, correctly alternating Claw hit.
- Rebalanced Ball & Chain wind-ups to 0.5x base damage while retaining full enchantment bonuses.
- Rebalanced Ball & Chain throws to 1.25x/1.75x/2.25x damage and made Gold skip directly to the 12-block full-charge result on its second revolution.
- Limited full-charge Armor Fracture to the outgoing throw and made weapon/offhand swaps cancel active throws safely.
- Made Bulwark strikes, Bulwark bashes, and Battering Ram entity hits scale with the vanilla attack-cooldown meter.
- Restricted Sweeping Edge to Scimitars and reduced Scimitar damage by roughly 10%, rounded to the nearest half point.
- Updated vanilla Ball & Chain recipes to use two Iron Chains and a full material block where applicable.
- Backported the finalized Stone/Gold/Iron Flail sprites, Ball & Chain grip orientation, Iron Chain combat model, and cracked-armor Armor Fracture icon.
- Added standalone physical left/right mouse control for main/offhand Claw attacks without requiring RLCombat.
- Added the extremely durable two-handed Sun-War Bulwark with passive reduction,
  all-direction combat guarding, armor-scaled slow attacks, movement tradeoffs,
  durability costs, and a server-authoritative four-block area bash.
- Added a dedicated recipe, tooltip, blocking model, and 3D model for the Bulwark.
- Added modern alternating flat/upright chain-link geometry to Flail and Ball &
  Chain animations without requiring a vanilla Chain item.
- Made Gold Ball & Chain reach full charge in two swings and added a full-charge
  confirmation jingle for every tier.
- Removed the unintended vanilla movement slowdown caused by the Ball & Chain's
  synchronized animation-use state.
- Backported the modern Bulwark and Battering Ram held/active transforms and
  two-handed player poses to Forge 1.12.2.
- Backported the modern Ball & Chain inventory/active sprites, material-colored
  Flail swing sprites, Iron Chain visuals, and Diamond Morning Star tab icon.
- Hid the ordinary held Flail and Ball & Chain models during sustained swings,
  while preserving their inventory and hotbar sprites and separate 3D effects.
- Prevented Ball & Chain wind-up attacks from mining blocks, matching Flails.
- Corrected the 1.12.2 Bulwark's third-person carry and guard orientation so
  the shield remains upright instead of inheriting the arm's downward pitch.
- Distinguished Desert and Jungle Myrmex Stinger Flail recipes by replacing
  their iron ingot with the matching Desert or Jungle Myrmex chitin.
- Sent paired-claw offhand damage before its visual swing packet so RLCombat
  cannot misclassify and reduce the custom full-damage attack.
- Living and Sentient Scimitars now guarantee Weakness III and Weakness IV
  respectively for 5 seconds instead of using the normal 10% Weakness proc.
- Living Flail and Ball & Chain 3D heads now use SRP's Hivesteel texture;
  Sentient versions use SRP's Bleeding Obsidian texture.
- Myrmex Flail and Ball & Chain 3D heads now use matching Jungle or Desert
  Resin textures; Stinger variants use the corresponding Cocoon texture.
- Dragon Bone Flail and Ball & Chain 3D heads now use Ice and Fire's Dragon
  Bone Block pattern, with custom red, purple, and light-blue recolors for
  Fire, Lightning, and Ice Dragon-Blooded variants.
- Ball & Chain sprites now hide only in first- and third-person hands during
  swings while remaining visible in inventories, hotbars, JEI, and the world.
- Dragon Bone Battering Ram heads now share the Dragon Bone Block texture and
  custom Fire, Lightning, and Ice recolors used by animated weapon heads.

## 1.0.0 - 2026-07-21

- Initial public release for Minecraft 1.12.2 and Forge 14.23.5.2860.

- Added deterministic Living/Sentient family procs using SRP's native potion effects.
- Added Corrosion to Morning Stars, Bleeding to Claws, Immalleable to Flails,
  held Rage to Battering Rams, and parasite-only Debar/adaptation bypass to
  Balls and Chains.
- Made Living Scimitars guarantee Weakness II and Sentient Scimitars guarantee
  Weakness III.
- Preserved SRP's native Sentient Prey drawback on the wielder.
- Created the Forge 1.12.2 project foundation.
- Reserved the `nanonaitors_arsenal` mod ID.
- Established a specialized-weapon design direction.
- Added wood, stone, gold, iron, and diamond Morning Stars.
- Added fully charged, confirmed-hit Armor Fracture stacking.
- Added tier-based Armor Fracture caps from 40% through 100%.
- Added custom Morning Star item textures with wooden hafts and tiered heads.
- Reworked Morning Star textures as readable diagonal 32x32 item sprites.
- Added wood, stone, gold, iron, and diamond RuneScape-shaped Scimitars.
- Replaced the original Sever concept with a 10% fully charged-hit chance to apply Weakness II for 2 seconds.
- Lengthened Scimitar handles for better held-item proportions.
- Enlarged and deepened the Scimitar blade curve and added Spartan Greatsword-style held transforms.
- Added paired Claws with linked offhand items and alternating i-frame piercing.
- Added alternating offhand attack animation and wrist-mounted Claw model transforms.
- Added a real right-click offhand Claw attack with an independent cooldown.
- Made linked Claws mirror the main Claw's enchantments, glint, and durability.
- Exempted the paired Claw strike from RLCombat's generic 50% offhand penalty.
- Replaced flat held Claw sprites with fitted 3D cuffs and forward-facing blades.
- Reworked Claw recipes to use a wooden backing and stick instead of leather.
- Made offhand Claw hit sounds reliably play for the attacker and observers.
- Added five tiered Flails with continuous two-block circular attacks.
- Added matching Flail sprites with wooden handles and iron-colored chains.
- Reduced every Claw tier to half its equivalent vanilla sword's base damage.
- Separated the Claw blades, knuckle plate, and wrist cuff to prevent model z-fighting.
- Tightened the Claw model seams without reintroducing overlapping surfaces.
- Made linked Claws sword-class items for correct weapon audio and added offhand whiff swings.
- Reworked Flails into confirmed-hit, equal-damage three-block area weapons.
- Converted Flails to server-authoritative held-left-click hitboxes with no required target.
- Reduced Flail attack speed from 1.2 to 0.8 and preserved block mining.
- Added optional Quality Tools detection and mirrored only its `Quality` tag when present.
- Added automatic deletion for any linked Claw dropped into the world.
- Changed Flail range to hitbox distance and allowed swings while mining blocks.
- Allowed real offhand items at the cost of disabling all paired Claw abilities.
- Reduced every Claw tier's base damage by 1 point.
- Added recipes, models, tooltips, and 32x32 sprites for both new weapon families.
- Added an explicit Arsenal creative tab with a Diamond Scimitar icon and grouped weapons.
- Added shared vanilla and modded sword-enchantment compatibility to every weapon family.
- Added a standalone circular Flail animation with no Mo' Bends dependency.
- Added first-version wood, stone, gold, iron, and diamond Battering Rams.
- Added held-left-click Ram charging, one hit per target per charge, and 3x3 wood/cobblestone breaking.
- Added long 3D log Ram models with iron bands, two grips, tiered spikes, recipes, tooltips, and creative-tab entries.
- Removed an isolated stray pixel from the Diamond Flail texture.
- Added wood, stone, gold, iron, and diamond Ball and Chain weapons.
- Added three-charge frontal wind-up sweeps with normal sword reach and a +/-1-block vertical area.
- Added release throws with 4/8/12-block reach, scaling damage and knockback, multi-target line hits, and solid-block collision.
- Added tiered maximum-charge armor piercing, permanent mob armor fracture, and temporary player Armor Fracture on throw hits.
- Added a two-handed 3D grip, dynamic iron chain, faceted tiered ball, recipes, tooltips, and creative-tab entries.
- Added a shared alternate Flail sprite that appears throughout continuous swinging and returns to the tier sprite on release.
- Replaced shader-sensitive line/cube weapon effects with opaque textured 3D chain links and tier-colored balls.
- Changed the Flail animation to orbit horizontally around the wielder at its full three-block reach.
- Added tiered Battering Ram breaking: soft soil; then planks; then logs/cobblestone; then ordinary stone.
- Changed the Battering Ram charge to a shield-style block animation with both arms braced in third person.
- Moved the Ram's 3x3 breaking plane above the floor and gave Gold Stone-level breaking with faster attack speed.
- Changed Ram recipes to use full material blocks and clamped the tier-colored front spike UVs.
- Fixed MmmMmmMmmMmm dummy detection, disabled block breaking while spinning a Flail, and added a sharper air-cut sound.
- Rebuilt animated balls with non-overlapping, explicitly mapped tier textures.
- Added pitch-aware Ball and Chain aiming, outward and retrieval hits, and a launch/retrieval swing lock.
- Added continuous and per-impact Battering Ram exhaustion, a three-hunger-icon cutoff, and explicit per-block durability loss.
- Locked camera facing while charging a Battering Ram and lengthened its rear log model.
- Reworked tier-specific Flail inventory/swing models and the Ball and Chain held model.
- Removed the forced Ball and Chain arm-swing loop that caused held-item jitter.
- Fixed Battering Ram block breaking at diagonal angles and synchronized durability loss.
- Removed empty-air Flail attack sounds and kept only its handle visible during the external orbit animation.
- Stabilized continuous Flail and Ball and Chain held-model states to prevent model flicker.
- Restored flat Flail inventory sprites while keeping only the handle visible during orbiting.
- Replaced fake vanilla Flail attacks with dedicated remote animation and Ball and Chain-style sweep sounds.
- Added all-tier glass breaking and iron/diamond clay and terracotta breaking to Battering Rams.
- Corrected out-of-range UVs on the extended Ram log and Ball and Chain head models.
- Restored the Stone Battering Ram recipe using Forge's standard stone ingredient.
- Hid internal Linked Claw items from JEI when JEI is installed.
- Increased Flail attack and orbit reach from 3 to 4 blocks.
- Removed the Ball and Chain's repeated vanilla third-person arm swing.
