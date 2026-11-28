# CurseForge upload — Forge 1.20.1 v0.1.10

## Upload settings

| Field | Value |
| --- | --- |
| File | `nanonaitors-arsenal-1.20.1-0.1.10.jar` from `build/libs` |
| Display name | Nanonaitor's Arsenal - 1.20.1 - 0.1.10 |
| Release type | Release |
| Minecraft version | 1.20.1 only |
| Mod loader | Forge only |
| Java version | Java 17 |
| Environment | Client and server |
| Minimum Forge | 47.4.18 |
| Required external mods | None beyond Forge |
| Changelog format | Markdown |

## Ready-to-paste changelog

### Nanonaitor's Arsenal 0.1.10 — Minecraft 1.20.1

Requires Minecraft Java 1.20.1, Forge 47.4.18 or newer 47.x, and Java 17.
Install Arsenal on both client and server for multiplayer.

#### Custom materials

- Add up to 8 of your own weapon materials in `config/nanonaitors_arsenal-materials.toml`, one per line: `id, Display Name, #RRGGBB, ingredient, harvest level, durability, attack damage, enchantability`.
- Each one adds every weapon in that material, named after it ("Amethyst Flail"), crafted like the iron weapon with your ingredient (an item or a `#tag`), with its metal tinted your colour. Handles, logs and wood keep theirs.
- The harvest level (0-4) picks which built-in tier its special rules follow. Restart after changes; the server and every client need the same list.

#### Battering Ram

- Normal hits swing the ram from back to front and push like Knockback II at full cooldown (I otherwise). The Knockback enchantment adds to it, on normal and charge hits.
- Ramming into a wall you can't break stops the charge with a thump, bounces you back and shakes the camera. Release use to charge again.

#### Ball and Chain

- Holding use to spin faster keeps the spin and charge progress instead of jumping ahead.
- A full-charge hit gives the thrower a short camera jolt.
- Shorter tooltip.

#### Scimitars

- Ordinary swings leave a faint air-slash streak along the blade.

#### Looks and tooltips

- Enchanted Flails and Balls and Chains glint on their 3D head, chain links and handle.
- Arsenal enchantments explain what they do under their name on items and books; Knockback explains what it adds to the Ball and Chain and Battering Ram.
- Every common config option now has an explanation in the file.

#### Fixes

- Holding a Flail, Ball and Chain, Morning Star or paired Scimitars no longer blocks other mods' area checks (for example a mod calling your horse to you).

#### Updating

Replace the previous Arsenal JAR and restart Minecraft. Do not keep multiple Arsenal versions installed. Multiplayer clients and servers must use the same version: this release changes the network protocol.
