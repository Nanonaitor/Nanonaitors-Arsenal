# Arsenal 1.12.2 — 2.0.4 test update

- **Dual scimitar bash audio:** plays vanilla's normal sweep/attack sound at normal pitch. A dedicated Arsenal sound-event ID keeps the intentional bash sound audible while queued, unintended guard swings remain suppressed. Blocking itself still uses the shield sound.
- **Stuck mining after switching:** entering scimitar controls aborts any active block-digging session and clears stale attack key state and queued presses. While paired scimitars own left-click, vanilla mining input stays cleared and block-breaking events are cancelled. The paired autoattack reads physical input independently. Single-scimitar use only resets the old session on the equipment transition, not continuously.
- **Ball & Chain first-person guard:** moved the guard model from the full-hand render event into the prepared first-person hand-render stage. It uses a centered visible position and suppresses the duplicate ordinary held sprite. This retains the 3D guarding ball; it does not replace it with an animated attack sprite.
- **Ball & Chain inventory animation:** the animated sprite now requires explicit attack state. Simply raising the item to shield no longer enables it. Local guarding clears stale wind-up state. Remote players now receive explicit wind-up heartbeats instead of treating every active-hand use as attacking; throw/return animation remains separately tracked.

Source safety copy: `tmp/bug-audit/before-2.0.4.zip`.

Verification: compilation/reobfuscation and the existing 113 regression checks passed; JSON sound definitions parsed successfully. Live modpack rendering/input has not been play-tested. Restart Minecraft, guard with Ball & Chain while watching the hotbar, and test mining a block then switching directly to paired scimitars while holding/releasing left-click. Check intentional scimitar bash audio separately from an ordinary incoming block.
