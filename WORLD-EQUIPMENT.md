# World equipment integration — 1.12.2

Arsenal can participate in existing vanilla/Spartan melee-weapon loot rolls and newly spawned vanilla humanoid equipment rolls. No extra mod is required.

## Defaults

- 25% of eligible Forge loot-table weapon results use a same-material Arsenal weapon instead. The original pool size, roll count, conditions, luck weight and loot functions remain in use. No extra item is added.
- 25% of eligible newly spawned vanilla humanoids that already receive melee equipment use a same-material Arsenal weapon instead. Unarmed mobs, bows, named weapons, players and already-loaded mobs are not converted.
- Supported mobs: zombies, husks, zombie villagers, zombie pigmen, skeletons, strays, wither skeletons and vindicators. Skeletons/strays qualify only if another spawn rule already gives them an eligible melee weapon.
- Alternatives: Morning Star, Scimitar, Blade Staff, Claws, Flail, Battering Ram and Ball & Chain. Disabled weapon families and unavailable material tiers are excluded. Linked Claws are internal equipment, not independent loot.
- Unsupported materials such as lead are skipped rather than upgraded. Special shields are not substituted for ordinary swords or shields.
- Mobs use their normal melee AI; this does not give them player-only charged or right-click abilities.

Settings are in Mods → Nanonaitor's Arsenal → Config → worldEquipment. Restart after changes. Server settings govern world generation and mob equipment.

## Socketed

Arsenal weapons extend ItemSword, so Socketed's existing SWORD category already recognizes them. Standard loot calls the original LootEntryItem generation path, allowing Socketed's own loot mixin to roll sockets once. Arsenal does not override socket category exclusions, tier weights, maximum counts or enchantability-based roll settings.

New Arsenal mob equipment optionally calls Socketed's MOB_DROP API. Existing socket capabilities are retained, and Socketed skips stacks that already have sockets. With Socketed absent, these are ordinary weapons. If an incompatible Socketed API is encountered, socket integration logs a warning instead of becoming a required dependency.

Inspected profiles: Dregora Parasited contains Socketed and RLSocketed; the inspected older RLCraft, RLCraft Dregora and RLCD recording profiles do not. Detection is mod/API based, not based on launcher profile names.

## Scope

Forge loot tables are supported, including nested tables when loaded. Hard-coded dungeon, Recurrent Complex, Battle Towers or Infernal Mobs generators that bypass Forge loot tables are not rewritten by this integration. Their separate loot lists still need pack-specific additions. RLSocketed's hooks can socket Arsenal items when those private generators are explicitly configured to produce them.

Only future loot generation and new spawn events are affected. Already generated chest contents, existing equipment and server mod lists are not edited.

Automated regression coverage checks tier matching, excluded ranged/unsupported items, preservation of weighted rolls/functions, probability endpoints and repeat-load idempotence. In-game testing with each supported pack remains necessary.
