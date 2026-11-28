# Forge 1.20.1 port — first test beta

Independent project. Primary behavior reference: Arsenal 1.12.2 2.0.1.
Starting implementation: Arsenal 26.1.2 modern combat/input state machines.
Target: Java 17, Forge 47.4.18. Independent from the existing source projects.

Scope: Morning Star, Scimitar, Claws/Linked Claw, Flail, Battering Ram,
Ball & Chain, Blade Staff, Tartsy Shield and Sun-War Bulwark; associated
effects/enchantments, tier assets, recipes, first/third-person ability visuals.

Restless Horizons reference integrations: Ice & Fire, Spartan Weaponry,
Spartan Fire, Defiled Lands Preborn, Quality Forked, JEI/EMI, Defenders.
Optional content must be registry-detected, not a hard dependency.
Living material accepts the verified srp_spartans long-blade fragment. Optional
SRP effects still require their original registry IDs; that add-on alone does
not provide the original SRP creature/effect systems.

The beta builds and has isolated server tests plus a development-client model
bake check. See README.md for controls, installation, configuration, exact test
coverage, and the deliberately documented compatibility differences.

Successive test builds have been installed into Restless Horizons for user
playtesting. Development-client checks run separately, without using user worlds.
Full-modpack compatibility and visual checks remain ongoing before a stable release.

## 0.1.8: 1.12.2 2.0.21 to 2.0.26

Ported, adapted to this build's architecture: third-person weapon poses
(`client/pose`, applied around `setupAnim` by `AbilityPoseMixin`), 3D chains and the
new ball head (`ChainRenderer`, `ClientWeaponRenderer`), claw slash streaks, the
scimitar block counter, Ball and Chain guard/throw rules (`BallPath`), Defender
hand-off, ram/claw/chain sounds with a client volume option, the new Blade Staff
art and pole recipes, and the world-equipment rule for designed gear.
Mo' Bends: its skeleton replaced every Arsenal pose. `MoBendsPoseMixin` (optional, skipped
without Mo' Bends) hands Arsenal's pose to Mo' Bends the way Mo' Bends adopts other animation
mods' poses, while Arsenal poses the arms (`client/MoBendsCompat`). See CURSEFORGE-0.1.8.md.
RLCombat: it attacks with the off-hand weapon on right-click, so a lone off-hand Scimitar hit
twice. With RLCombat installed that strike is left to RLCombat (`compat/RLCombatCompat`), as
1.12.2 sent it through RLCombat's attack; Arsenal keeps it when it already owns the click.
Fungal Infection: Spore (mod id `spore`) works as a parasite mod for Living and Sentient
weapons: its mobs evolve Living weapons (`compatibility.parasiteMods`), Living weapons are made
from the diamond weapon and a Symbiotic Core, and Living Morning Star and Claws inflict Spore's
Corrosion and Hemorrhage.
Dev runs: `-PdevMods=talents,rlcombat,mobends,spore` adds the jars in `tmp/dev-mods/<name>/`;
`-PcombatProbe` presses the real attack keys against a dummy and logs each weapon's hit count;
`-PposeSheets=scimitars,...` limits `-PposeCapture`.
The loot-table swap is ported as a global loot modifier (`LootAlternativesModifier`,
`worldEquipment.lootIntegration` / `lootReplacementChance`): it changes rolled stacks and
never edits loot tables. Also ported: furnace recycling for the Silver, Bronze, Steel and
Umbrium tiers and the Blade Staff (`nanonaitors_arsenal:tag_smelting` takes a tag result,
as 1.12.2 used the ore dictionary), the JEI hiding rules (`compat/jei`), and Talents as a
second level-requirement backend.
