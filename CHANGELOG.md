# Changelog

## Unreleased

- Replaced all 19 Blade Staff tier textures with the new 32x32 double-bladed
  artwork, using tier palettes for the blade ends and retaining the handle.
  Weapon pixels are fully opaque; empty background pixels remain transparent.
- 2.0.3: Consume Ball and Chain attack mouse events and queued/held vanilla
  attack input, preventing parallel targeted melee swings. Charge and rendering
  read the physical configured attack button instead; fast-spin boost remains
  physical-button based and server-timed damage/audio cadence is unchanged.
- 2.0.2: Correct RLCombat's sound-event prefix to `player.swing_` (the OGG
  filenames use `swing_`), include `player.swordslash`, and suppress Dynamic
  Surroundings sword/blunt/tool swing overlays for the Ball and Chain wielder.
- 2.0.1: Isolated Ball and Chain damage-window audio under dedicated sound IDs.
  Suppress nearby local vanilla melee and RLCombat swing sounds while wielding
  the weapon, including delayed sounds after mouse release. Wind-up sweeps and
  outbound/return damage passes retain their timed swing or successful-hit sound.
- Added a Reskillable Defense 16 requirement to equip and use the Sun-War
  Bulwark when Reskillable is installed.
- Replaced the Dragonbone and desert/jungle Myrmex weapon art supplied for
  Ball and Chain, Flail (including swinging frames), Morning Star, and Scimitar;
  superseded PNGs were overwritten in place so no duplicate legacy set remains.
- Made the Bulwark blocking model use the exact normal-model GUI transform so
  its inventory and hotbar icon cannot change while the player is shielding.
- Updated the Bulwark third-person test to right-hand `[0, 30, 15]` and
  left-hand `[10, 22, 22]`; moved its held first-person pose outward and made
  its blocking pose rise four units from that resting position.
- Kept the Bulwark's `[0, 30, 17]` third-person test position and made its
  first-person blocking transforms exactly match its confirmed-visible held
  transforms.
- Updated the Bulwark positioning test to right-hand translation `[0, 30,
  17]` and left-hand `[10, 22, 24]`, and restored its first-person blocking
  transforms to the proven visible Tartsy/vanilla shield anchors.
- Updated Bulwark positioning test 1 to right-hand translation `[0, 30, 20]`
  and equivalent left-hand translation `[10, 22, 27]`.
- Updated the Bulwark positioning test to right-hand translation `[0, 30,
  25]` and equivalent left-hand translation `[10, 22, 32]`.
- Updated the Bulwark positioning test to right-hand translation `[0, 30,
  35]` and equivalent left-hand translation `[10, 22, 42]`.
- Updated the Bulwark positioning test to right-hand translation `[0, 33,
  33]` and equivalent left-hand translation `[10, 25, 40]`.
- Updated the Bulwark positioning test to right-hand translation `[0, 20,
  35]` and equivalent left-hand translation `[10, 12, 42]`.
- Updated the Bulwark positioning test to right-hand translation `[0, 30,
  30]` and equivalent left-hand translation `[10, 22, 37]`.
- Updated the Bulwark positioning test to right-hand translation `[0, 50,
  30]` and equivalent left-hand translation `[10, 42, 37]`.
- Updated the Bulwark positioning test to right-hand translation `[-5, 50,
  0]` and equivalent left-hand translation `[5, 42, 7]`.
- Updated the Bulwark positioning test to right-hand translation `[-5, 50,
  30]` and equivalent left-hand translation `[5, 42, 37]`.
- Updated the Bulwark positioning test to right-hand translation `[15, 50,
  15]` and equivalent left-hand translation `[25, 42, 22]`.
- Set the test Bulwark third-person right-hand blocking translation to
  `[0, 50, 0]` and applied its equivalent left-hand offset `[10, 42, 7]`.
- Centered the player-level Bulwark blocking pose using the interpolated X
  midpoint between its confirmed far-left and far-right test positions.
- Preserved the Bulwark's player-level blocking height and vanilla depth while
  compensating its X position for the raised-arm sideways displacement.
- Raised the vanilla-anchored Bulwark blocking model by 30 model units so it
  sits at the player's level instead of extending below the ground.
- Restored the Bulwark's third-person blocking X/Z coordinates to Minecraft's
  vanilla shield hand anchors while retaining the custom model's vertical lift.
- Preserved the Bulwark's centered third-person guard placement while moving
  its depth forward so it renders in front of the torso and near the hands.
- Shifted the Bulwark's third-person blocking pose across the remaining side
  gap to cover the wielder's torso and moved it slightly farther forward.
- Closed the remaining sideways gap in the Bulwark's third-person blocking
  pose and shifted it slightly forward after it sat just behind the wielder.
- Corrected the Bulwark blocking pose's sideways overshoot while preserving
  the manually tuned right-hand height and depth.
- Applied the manually tuned third-person Bulwark blocking position to the
  packaged model and mirrored the same offset relationship for the off hand.
- Lowered both third-person Bulwark blocking transforms by 30 model units
  after confirming that increasing their JSON Y translation raises the model.
- Re-centered the Bulwark's third-person blocking depth between the confirmed
  front/behind positions and lowered the pose substantially beside the player.
- Lowered the Bulwark's third-person blocking pose after the previous vertical
  overshoot and pulled it another 16 model units toward the wielder.
- Pulled the Bulwark's third-person blocking pose another 24 model units
  inward along the confirmed depth axis without altering its height or angle.
- Lowered the Bulwark slightly within inventory slots, moved both first-person
  poses lower and farther toward the screen edge, and corrected its blocking
  third-person placement along the player-depth axis so it meets the body.
- Pulled the Bulwark's third-person blocking pose substantially closer to the
  wielder, raised its inventory icon into the slot, and raised its normal and
  blocking first-person poses so the shield remains visible on-screen.
- Corrected the reversed inward axis on the Bulwark's raised third-person
  blocking pose so the shield sits beside the wielder instead of floating away.
- Raised the Bulwark slightly and moved it inward in its resting third-person
  pose so its handle meets the wielder's hand; raised both blocking poses
  substantially and moved them much closer to the player's body.
- Closed the remaining resting third-person gap between the Bulwark and the
  wielder's arm, and raised both third-person blocking poses by 16 model units.
- Corrected the reversed horizontal adjustment on the rotated third-person
  Bulwark and moved it inward from the previous good resting position.
- Raised the resting third-person Bulwark another 12 model units and moved it
  another 6 units inward so its body sits over the wielding arm.
- Corrected the direction of the Bulwark's resting hand offset and raised its
  non-blocking first- and third-person poses substantially over the held hand.
- Raised Tartsy's non-blocking first-person pose further into view and moved
  both non-blocking Bulwark third-person poses inward over the held hand.
- Raised Tartsy's resting first-person pose and moved its third-person model
  slightly closer to the wielder's hand.
- Corrected the Sun-War Bulwark's upside-down orientation and reduced its
  enlarged model from 150% to 120% of base size.
- Flipped both custom shield meshes 180 degrees around their centered local
  axes so their fronts face outward in inventory and first-/third-person views.
- Increased the Sun-War Bulwark model to 150% size in every render context.
- Stopped replacing the Bulwark movement modifier every tick, eliminating the
  movement-FOV zoom pulse while preserving its carry and guard slowdowns.
- Replaced the lossy vanilla-JSON conversions of both shield models with a
  dedicated renderer using their original Blockbench geometry, exact pivots,
  and multi-axis rotations. Removed a duplicated Tartsy face that caused
  coplanar flickering.
- Recentered both custom shield renderers and replaced their old conversion-
  specific display transforms with stable vanilla shield hand, guard, GUI,
  ground, and frame poses so they remain attached naturally at every angle.
- Restored the Sun-War Bulwark's proper localized display name instead of the
  vanilla ItemShield fallback name, "Shield."
- Reoriented the Tartsy Shield's resting main-hand third-person model, angled
  both third-person guarding poses 60 degrees outward, and lowered its resting
  first-person presentation to preserve more of the player's view.
- Fixed the Sun-War Bulwark losing its 40% carry movement penalty when the
  opposite hand became occupied; its 15% passive damage reduction and carry
  slowdown now remain active together while held.
- Removed the fixed dual-Scimitar auto-attack speed clamp so real attack-speed
  bonuses and penalties now control its alternating cadence.
- Blade Staff melee splash remains active with an occupied offhand, while
  Sweeping Edge and Rin's So Many Enchantments' Arc Slash are now rejected and
  removed from Blade Staffs to prevent overlapping sweep effects.
- Replaced the Tartsy Shield with the supplied Charge Targsy 3D model and
  texture, adapted to Minecraft's supported item-model rotations.
- Replaced the Sun-War Bulwark with the supplied custom 3D model and texture.
- Claws now give villagers, mounts, and other entities first priority on right
  click before the linked-claw attack is considered.
- Prevented RLCombat/Better Combat from layering its local swing swoosh over
  the Ball and Chain's timed swing audio.
- Removed duplicate Rotation Force explanation lines from item tooltips.
- Added the Breeched curse, which doubles Tartsy Shield and Sun-War Bulwark
  disable cooldowns and cannot be combined with Recovery.

## 2.0.0 - 2026-08-31

- Added tiered Blade Staffs with continuous empty-offhand attacks, two-block
  melee splash damage, a timed damage-reflection spin, and Stun on reflected
  non-melee attacks.
- Added the one-handed Tartsy Shield: it negates one hit before a four-second
  disable, can launch an invulnerable shield dash, Stuns struck enemies, and
  primes a guaranteed critical after a confirmed dash hit.
- Added the Stunned effect and the Recovery shield enchantment.
- Backported chargeable Morning Stars with quarter-charge damage scaling,
  horizontal area attacks, full-charge Stun chance, particles, sound, and
  first- and third-person animations.
- Added independent offhand Scimitar attacks, alternating dual-wield attacks,
  crossed dual-Scimitar guarding, durability handling, and first- and
  third-person poses.
- Added Ball and Chain guarding with its rendered ball, accelerated wind-up,
  movement tradeoffs, blocking sounds, and corrected first-/third-person state.
- Expanded Sun-War Bulwark main-/offhand use, two-handed guard poses, GUI guard,
  cooldown handling, and offhand bash damage.
- Improved paired Claw input priority, simultaneous hand animation, charged
  auto-attacks, i-frame piercing rules, knockback, and confirmed-hit audio.
- Reworked every Flail tier with animated sprites, straight chains, material
  spiked-ball renders, crossed planes, and fading motion trails.
- Added animated Ball and Chain sprites and material-correct recolors across
  vanilla, Dragonbone, Myrmex, Living, Sentient, and other optional tiers.
- Updated Morning Star and Scimitar artwork, held sizing, tooltips, and tier
  recolors. Original commissioned artwork is credited to Star Artsy.
- Added concise item summaries with Shift-expanded mechanical descriptions.
- Added optional XAT race weapon affinities for Scimitars, Flails, Claws,
  Morning Stars, Sun-War Bulwarks, Balls and Chains, Battering Rams, and Blade
  Staffs, with configurable damage values and a master toggle.
- Improved RLCraft Gold and Silver set bonuses, Reskillable checks, Dragonforge
  conversions, Spartan/Quark recipe compatibility, Quality Tools integration,
  and SRP Living/Sentient behavior.
- Living and Sentient Blade Staffs cleanse Call of the Hive from nearby
  entities while held.

## 1.2.3 - 2026-08-24

- Fixed Gold and Silver Arsenal weapons being detected but not registered as
  native members of RLCraft's SetBonus weapon sets.
- Magic Infused Weapon and Quicksilver Hands now activate and display through
  SetBonus itself, with a numeric fallback for customized pack configurations.

## 1.2.2 - 2026-08-24

- Fixed RLCraft's Magic Infused Weapon and Quicksilver Hands bonuses for all
  Arsenal Gold and Silver weapons by detecting the pack's exact SetBonus sets
  and applying their configured +50% damage/attack-speed attribute operations.
- Added native Fire, Ice, and Lightning Dragonforge upgrades for every
  Dragonbone Arsenal weapon family while preserving names, enchantments,
  durability, qualities, and other item data.

## 1.2.1 - 2026-08-23

- Fixed a dedicated-server startup crash caused by the compatibility tooltip
  event subscriber loading Minecraft's client-only tooltip classes on the server.
- Restricted compatibility tooltip registration to the physical client without
  changing any item tooltip content or gameplay behavior.

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
