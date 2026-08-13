# Nanonaitor's Arsenal

A Forge 1.12.2 weapon mod focused on specialized weapons with distinct combat
roles, counters, and active abilities rather than interchangeable damage tiers.

The current 1.12.2 release is `1.1.0`for Forge 14.23.5.2860.

This branch contains the Minecraft 1.12.2 port. The repository's default
`main` branch contains the Minecraft 26.1.2 release.

## Weapon families

- Sun-War Bulwark
- Morning star
- Scimitar
- Flail
- Paired claws
- Battering ram
- Ball and chain

## Implemented mechanics

### Sun-War Bulwark

An extremely durable two-handed fortress shield that requires an empty offhand. While
ready it passively reduces non-void damage by 15%. Active guarding works from
all directions and completely stops direct melee, projectiles, magic, and
explosions, but not falling, fire, lava, drowning, starvation, suffocation, or
similar environmental hazards. Movement is reduced by 40% while carried and
75% while guarding. Its extremely slow 0.25-speed strikes deal
`1 + total armor points` damage. Attacking while guarding performs a
server-authoritative four-block-radius bash with armor-scaled damage, strong
knockback, and a 3-second cooldown. Blocked attacks and successful bash
targets consume durability. Normal strikes and bashes scale with the vanilla
attack-cooldown meter.

### Morning Stars

Morning Stars are slower and hit harder than equivalent tier swords. Fully charged 
confirmed hits build Armor Fracture, reducing the target's vanilla armor attribute
20% per level. Wood caps at 40%, stone and gold at 60%, iron at 80%, and diamond at 
100%. The effect lasts 30 seconds on mobs and 10 seconds on players.

### Scimitars

Fast weapons with a large hooked profile. Ordinary tiers inflict Weakness I for 10 
seconds; higher tiers inflict Weakness II and III respectively.

### Claws

Paired weapons that automatically equip a hidden linked claw in an empty
offhand. Left-click attacks with the main claw; right-clicking a living target
attacks with the offhand claw on an independent cooldown. A fully charged hit
can pierce normal damage invulnerability frames only when the previous
confirmed hit used the opposite hand. Both claws share durability and
enchantments. Every 4th fully charged, correctly alternating hit against the
same target is a guaranteed critical. 

A real offhand item is allowed, but it disables every paired Claw ability until
the offhand is empty again. Linked Claws are deleted immediately if they become 
dropped world entities.

### Flails

Flails are wide-area crowd-control weapons. Hold left-click while aiming anywhere 
to create a full four-block attack sphere every 25 ticks; no primary target is required. 
Every visible, non-allied enemy receives a complete weapon hit. Block breaking is
disabled while spinning. Range is measured between entity hitboxes so elevated,
lowered, tall, and special entities are detected. 

Flails have 0.8 attack speed. A standalone horizontal swing animation shows
an opaque tier-colored ball and repeated iron links orbiting four blocks around
the wielder.

### Battering Rams

Battering Rams are extremely slow, two-handed charge weapons. 
Hold left-click with an empty offhand to rush forward, strike each enemy once per 
uninterrupted charge, and smash a 3x3 face at body level without drilling into the floor. 

Wood breaks soft soil; stone and gold add crafted plank blocks; 
iron adds logs and cobblestone derivatives; 
diamond adds ordinary stone derivatives. 

Gold has Stone-level breaking but a
faster 1.0 attack speed. Each non-wood recipe uses a full material block for its
spike. 

Each enemy hit or block broken consumes one durability.
Charging continuously builds exhaustion, with additional exhaustion for each block or
enemy struck, and cannot start or continue at three visible hunger icons or
less.

### Balls and Chains

Two-handed Ball and Chain weapons combine frontal wind-up sweeps with a charged
line attack. Hold left-click to swing once every 25 ticks; each sweep strikes a
three-block area ahead extending one block above and below the player, and adds
one charge up to three. Wind-up sweeps deal half base damage while retaining
full enchantment bonuses. Releasing launches the ball through every enemy in its
path for 4/8/12 blocks in the exact aimed direction, including upward and
downward shots, stopping at the first solid block. It damages every target once
on the outward pass and again while returning to the wielder. A new wind-up
cannot begin until retrieval finishes. Release damage is
125%/175%/225% and knockback also rises with charge. Gold reaches the full
12-block, 225% third-charge result on its second revolution. A full-charge throw
pierces 25%/50%/50%/75%/100% armor from wood through diamond. Every confirmed
throw hit permanently reduces a mob's armor by at least two points (or 10%,
whichever is greater); players receive ten seconds of tier-scaled Armor
Fracture instead, applied only by the outgoing pass. Switching weapons or filling
the offhand cancels an active throw safely. The dynamic chain and solid tier-textured ball render the compact wind-up,
collision-limited launch, and return while both hands hold the grip.

## Enchanting

Every Arsenal weapon is damageable, reports the Forge `sword` tool class, and
accepts normal sword enchantments through enchanting tables and enchanted books.
This includes Sharpness, Smite, Bane of Arthropods, Knockback, Fire Aspect,
Looting, Unbreaking, and Mending. Sweeping Edge is restricted to Scimitars.
Modded enchantments that target ordinary swords can use the same compatibility path.

## Optional RLCraft material integration

The mod remains fully standalone. When their source mods and ingredients are
present, all six weapon families also gain Silver, Bronze, Steel, Umbrium,
Dragonbone, Flamed/Iced/Electric Dragonbone, Desert/Jungle Myrmex, Desert/Jungle
Myrmex Stinger, Living, and Sentient tiers. Missing tiers are hidden from the
creative tab and JEI and do not register recipes.

- Silver deals +2 damage to undead.
- Myrmex weapons deal +4 damage to non-arthropods and Death Worms; Stinger
  variants also inflict Poison III for 10 seconds.
- Dragon-blooded weapons retain their fire, ice, or chain-lightning identity,
  knockback, and opposed-dragon bonus damage. Chain lightning triggers at most
  once per attack tick during multi-target attacks.
- Umbrium uses iron-class combat stats with higher enchantability.
- Living weapons gain evolution points only from parasite kills and evolve into
  the corresponding Sentient weapon at SRP's configured health threshold.
  Enchantments, NBT/quality data, and proportional durability are preserved.

Living attacks always apply their family specialty. Sentient attacks apply the
level-II version and retain SRP's normal chance to mark their wielder as Prey:

- Morning Stars inflict Corrosion; Claws inflict Bleeding.
- Flails inflict Immalleable.
- Scimitars guarantee Weakness II/III, extending their existing identity.
- Battering Rams grant Rage I/II while held and cleanse that Rage when unequipped.
- Balls and Chains ignore SRP damage adaptation and inflict Debar I/II against
  parasites only.

## Build

Use Java 8 and the supplied Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

Release JARs are created under `build/libs/`.

## License

Copyright 2026 Nanonaitor. See [LICENSE.md](LICENSE.md).
