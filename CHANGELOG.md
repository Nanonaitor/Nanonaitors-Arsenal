# Changelog

## 1.1.6 - 2026-08-17

- Ball & Chain outward and returning travel now accelerates or slows with the wielder's live attack speed, Haste, and Rotation Force.
- Kept client and server charge, return-hit timing, sounds, and 3D travel animation synchronized at altered speeds.
- Added explicit Haste scaling to Flail and Ball & Chain rotation speed.
- Expanded Ball & Chain solid-block collision sampling across its width and height so close or slightly offset blocks are not skipped.
- Added common-config effective-level caps for Long Chain and Rotation Force; datapacks can provide higher natural enchantment levels when desired.
- Clarified that Gold Ball & Chain's second rotation reaches both full 12-block base reach and full throw damage.

## 1.1.3 - 2026-07-26

- Flail combat links now use connected segments of Minecraft's 3D Iron Chain block model instead of the flat Iron Chain item sprite.
- Adopted the redesigned RuneScape-inspired Scimitar silhouette for every vanilla tier, with consistent gold guards, wooden grips, material-colored blades, and the reduced in-hand model size/thickness.
- Fixed linked off-hand Claws surviving hand or hotbar swaps; temporary linked copies are now removed from every inventory slot whenever their real main-hand Claw is no longer active.
- Reoriented the actual Ball & Chain idle and charging sprites in third person so the player's hand grips the chain rather than overlapping the ball; inventory and first-person presentation remain unchanged.
- Ball & Chain charging swings now process weapon enchantment damage and post-hit effects while retaining their `0.5x` base/attribute damage multiplier.
- Fixed a player-tick crash when changing away from a Ball & Chain during its outgoing or returning throw animation; switching weapons now cancels the active throw safely.
- Increased the redesigned Scimitar's inventory, hotbar, and item-browser display size by 15% without changing its held scale.
- Updated the Copper, Stone, Gold, and Iron Flail sprites and the Netherite Morning Star sprite with the finalized artwork.

## 1.1.2 - 2026-07-26

- Corrected release packaging so the complete compiled mod, including its Forge entry point, is included in the distributed JAR.
- No gameplay or balance changes from 1.1.1.

## 1.1.1 - 2026-07-26

- Gold Ball & Chain now skips charge stage 2: its second revolution delivers the full 2.25x damage and 12-block reach of a normal third charge.
- Battering Rams now stop immediately on attack release, keep client/server charge state synchronized, and use a smoother third-person running stride.
- Flail revolutions now travel in the same apparent direction in first- and third-person views.
- Fixed normal Bulwark strikes always using the minimum 20% attack-charge multiplier; fully charged strikes now deal the intended `1 + armor points` damage.
- Battering Ram entity damage now snapshots and consumes attack charge when a continuous ram begins; mining remains available immediately regardless of charge.

## 1.1.0 - 2026-07-26

- Scimitars now guarantee Weakness II for 10 seconds on every successful hit.
- Ball & Chain combat renders now use a six-protrusion spiked head on every vanilla tier.
- Flails stop spinning while the player blocks with a conventional shield; Defenders remain compatible.
- Cleaned weapon tooltips, including removing the Battering Ram hunger-warning line while preserving its actual hunger requirement.
- Removed dormant models and textures for unregistered non-vanilla compatibility tiers.
- Ball & Chain charging swings now deal 0.5x damage; throws deal 1.25x, 1.75x, or 2.25x damage by charge.
- Fully charged Ball & Chain throws apply full enchantment damage and Armor Fracture only once, on the outgoing hit.
- Ball & Chain recipes now use two Chains and a material block; Netherite remains a Smithing Table upgrade.
- Every fourth fully charged, properly alternating Claw hit is now a guaranteed critical strike.
- Reduced Scimitar base damage by roughly 10%, rounded to the nearest half point.
- Bulwark attacks and bashes now scale with vanilla attack-charge strength.
- Stabilized the Battering Ram's third-person charge pose and corrected the Flail's third-person rotation direction.
- Added a cracked-armor status icon for Armor Fracture.

## 1.0.0 - 2026-07-23

- Ported the complete standalone Arsenal weapon roster to Minecraft 26.1.2 / Forge 64.0.12.
- Rebuilt combat input and networking for the modern Forge API.
- Rebuilt Flail and Ball & Chain visuals for the modern render-submission system while preserving their original motion, iron links, and material-colored 3D heads.
- Added the Sun-War Bulwark with the same intended mechanics as the 1.12.2 build.
- Added all vanilla-tier recipes, tooltips, item models, linked Claw behavior, creative inventory support, and Armor Fracture.
- Assigned main and linked Claw attacks directly to left and right click instead of automatically alternating left clicks.
- Fixed an initial timestamp overflow that prevented Ball & Chain charging, attacks, and 3D visuals from starting.
- Restored pre-hit charge tracking and visible red Armor Fracture particles for fully charged Morning Star hits.
- Rebuilt Flail and Ball & Chain visuals with compact tiered heads and alternating solid 3D chain links.
- Separated linked-Claw cooldown from the vanilla main-hand attack meter so both claws can strike simultaneously.
- Stopped Flail display entities immediately when attack input is released.
- Added native enchanting-table and shield-enchantment compatibility to Arsenal equipment.
- Enlarged and centered the two-handed Bulwark, increased its slow armor-scaled strike, and expanded its bash to a true 4-block radius.
- Reworked Bulwark damage to 1 plus total armor points, added 30% carried and 60% guarding movement penalties, an overhead braced guard pose, and repaired the bash input with a visible cooldown and heavy confirmation sound.
- Hardened Flail and Ball & Chain cleanup so transient links are hidden and emptied before removal.
- Lowered and widened the guarding Bulwark across the player's back, doubled bash cooldown to 3 seconds, and added 4,096 durability consumed by each blocked hit and successful bash target.
- Enforced the Bulwark's empty-offhand requirement across guarding, damage, bash, movement, passive defense, and its two-handed pose.
- Gold Ball & Chain now reaches full power in two 50% stages with an 8-block maximum reach.
- Restricted Sweeping Edge to Scimitars while preserving other melee and durability enchantments for Arsenal weapons.
- Increased Bulwark movement penalties to 40% carried and 75% guarding, with a shield-impact sound on every durability-consuming block.
- Added individual Claw swing sounds and a distinct critical confirmation sound when alternating attacks pierce invulnerability frames.
- Routed linked offhand Claw strikes through vanilla enchantment damage, knockback, and post-attack effect processing.
- Shifted the Battering Ram's forward 3x3 break area to feet-through-head height so ground-level obstacles in its path are cleared without mining beneath the player.
- Replaced Battering Ram name matching with extensible per-tier block tags, adding grass and other soft terrain plus broader wood, cobble, clay, concrete, brick, stone, Nether and End masonry progression.
- Restored the Flail's secondary circular swinging sprite while attack is held using the modern item-state system.
- A successful Bulwark bash now forcibly lowers the shield for its 3-second cooldown.
- Replaced the Flail's static circular held-attack model with its handle and collar
  so only the animated 3D chain supplies links around the player.
- Added an active Ball & Chain held model showing one iron grip-ring while the
  tier-colored ball winds up, travels outward, and returns.
- Removed the duplicate server-spawned chain rig that caused delayed and stationary
  ghost links to overlap the smooth client-rendered Flail and Ball & Chain.
- Restored registration of the transient client renderer so Flail and Ball & Chain
  combat once again display their animated 3D links and tier-colored heads.
- Standardized Ball & Chain throw reach to 4 blocks per charge for every tier;
  Gold keeps its two-charge progression and now reaches 8 blocks at full charge.
- Added a one-time confirmation chime when a Ball & Chain reaches its maximum charge.
- Added dedicated low two-handed carry and braced charging poses for Battering Rams.
- Replaced the Ram's vanilla block-mining swing with a braced running charge animation.
- Realigned Battering Rams along the player's forward axis and separated both hands across their front and rear grips.
- Added Ball & Chain swing, release, and return sounds.
- Replaced the experimental Diamond Ball & Chain art with a compact round-ball idle
  icon and a separate circular chain-and-ball swinging frame, both at 32x32.
- Extended the matching idle and swinging Ball & Chain sprite pair to Wood, Stone,
  Gold, and Iron with tier-specific material colors and consistent iron links.
- Aligned every animated Flail chain link to the chain path and restored the prior
  tier-colored swinging sprites. First person uses only the clean held-item frame,
  while the full animated 3D chain remains exclusive to third person.
- Routed every Flail AOE target through effective melee damage, including off-hand
  Defender attack bonuses, Strength, attack-charge scaling, enchantment damage,
  and normal hurt-event bonuses such as Finesse.
- Added a Ball & Chain–only first-person 3D renderer: the material-colored ball and
  smaller aligned links now appear during windup, outward throw, and return while
  the Flail retains its clean sprite-only first-person presentation.
- Corrected the third-person Ball & Chain transform for Forge's world-aligned post-
  render pose: the hand and windup rotate with the body while the release follows
  the unmodified eye look vector used by the actual line attack.
- Added Copper and Netherite versions of every weapon family, including names,
  recipes, item definitions, tier-colored sprites, and animated render materials.
- Copper follows vanilla Copper tool properties. Netherite weapons are
  fire-resistant and upgrade from their corresponding Diamond weapon through the
  Smithing Table, preserving enchantments and custom data.
