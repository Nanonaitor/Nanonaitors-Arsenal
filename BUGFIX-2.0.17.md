# Arsenal 1.12.2 — 2.0.17

## Living and Sentient durability

The installed Spartan Weaponry 1.6.1 / Spartan Fire 1.4.0 do not supply Living or Sentient tiers. Those are supplied by SRParasites 1.9.21. Its Living and Sentient melee weapons both use MATERIAL_LIVING, whose durability comes from `Living Weapons Durability`.

The Dregora Parasited and RLCD 1.1 (RECORDING) configurations both set that value to **1000**. Arsenal previously hardcoded **1000** for both tiers. Therefore the correct matching values in these profiles remain **1000 → 1000** for Morning Star, Scimitar, Double-Bladed Scimitar, Claws (including linked claw), Flail, Ball and Chain, and Battering Ram.

New behavior: at post-initialization Arsenal copies the registered SRP Living/Sentient sword durability separately to every corresponding Arsenal weapon, so a pack changing SRP's durability no longer leaves Arsenal at its hardcoded default. No new required dependency. Missing SRP references leave the original default intact. Existing damage NBT is not reset.

## Red chains

Living and Sentient animated Flail and Ball and Chain links now use a red item tint (CC3030). The shared iron-chain texture supplies shading. Head, trail, inventory sprite, other tiers and standalone iron chain remain unchanged. This applies to the animation-part render path in both first and third person.

## Defender guard interaction

Arsenal's server and renderer explicitly exempted Defenders from the ordinary shield restriction. Its client request handler nevertheless used the general shield-priority gate, which rejected holding Use Item with a Defender. That prevented the custom flail attack packet from being sent. No accepted swing meant neither damage nor the server-generated sweep ring.

The flail now uses a Defender-aware gate for both held-click and direct-target attacks. Conventional shields and Arsenal shield-bash ownership still suppress it. Client request timing resets when the player instance changes, avoiding stale cooldowns after reconnecting.

The fix is in **Arsenal**, not Defenders. No Defender update is required for this patch. High attack speed did not cause the guard gate, though vanilla hurt-resistance may still reject equal-strength hits that occur too close together; this patch does not remove immunity frames or artificially multiply damage.

## Verification / in-game checklist

- Build and automated regression checks cover attack intervals, chain-part tint selection, all Living/Sentient weapon defaults and the corrected input gate.
- In-world visual and multiplayer verification still required. Test with a Defender in off-hand; hold attack before raising guard, then raise guard before holding attack. Test aiming at a mob, ground, and air near a mob.
- Repeat with Haste IV (`/effect @p minecraft:haste 120 3 true`) and the original attack-speed equipment. 1.6 total attack speed with Haste IV gives a 9-tick Arsenal interval; +1.6 added to the flail's base 0.8 gives 6 ticks.
- Confirm sweep rings continue and targets take damage when eligible. Conventional shields should still stop the flail.
- Check Living/Sentient chain colors in first/third person, and an iron-tier chain as a control.
- Install matching Arsenal versions on client and server for multiplayer. No profile installation or GitHub upload performed by this task.
