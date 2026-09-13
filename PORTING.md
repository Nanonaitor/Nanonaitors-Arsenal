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
