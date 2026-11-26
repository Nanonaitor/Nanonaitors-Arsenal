# CurseForge upload — Forge 1.20.1 v0.1.9

## Upload settings

| Field | Value |
| --- | --- |
| File | `nanonaitors-arsenal-1.20.1-0.1.9.jar` from `build/libs` |
| Display name | Nanonaitor's Arsenal - 1.20.1 - 0.1.9 |
| Release type | Release |
| Minecraft version | 1.20.1 only |
| Mod loader | Forge only |
| Java version | Java 17 |
| Environment | Client and server |
| Minimum Forge | 47.4.18 |
| Required external mods | None beyond Forge |
| Changelog format | Markdown |

## Ready-to-paste changelog

### Nanonaitor's Arsenal 0.1.9 — Minecraft 1.20.1

Requires Minecraft Java 1.20.1, Forge 47.4.18 or newer 47.x, and Java 17.
Install Arsenal on both client and server for multiplayer.

#### Ball and Chain

- Full-charge hits crack armour with a new sound and show the Armor Fracture icon over the target.
- A thrown ball now smashes projectiles in its path, on the way out and back, with the same crack (lighter). Your own projectiles are left alone.
- Subtitles for the new sound.
- With an empty offhand, the whole wind-up also blocks attacks from the front, as on 1.12.2.

#### Electric Dragonbone and Dragonforge

- Electric Dragonbone hits arc chain lightning to nearby enemies: 5, 4, 3, 2 and 1 armor-bypassing damage per hop, jumping up to 8 blocks. Configurable in `compatibility`.
- Ice & Fire's Dragonforge can now upgrade Dragonbone weapons into Flamed, Iced and Electric Dragonbone, keeping their name, enchantments and wear. The crafting-table upgrades still work.

#### Multiplayer

- The server now sends its combat settings (reach, charge time, enchantment switches and caps, scimitar counter) to every player, so clients no longer need a copy of the server's config.
- Other players' paired claws now show both hands striking at once.

#### Fixes

- The `reach.ballThrowPerCharge` setting now works; throws used the flail radius instead. Both default to 4, so nothing changes unless you set it.

#### Updating

Replace the previous Arsenal JAR and restart Minecraft. Do not keep multiple Arsenal versions installed. Multiplayer clients and servers must use the same version: this release changes the network protocol.
