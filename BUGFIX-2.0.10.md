# 2.0.10 — shield interaction priority

Removed direct shield activation from the client mouse event and Blade Staff tick fallback. These bypassed vanilla entity/block interactions, causing Tartsy (and other shields paired with Arsenal weapons) to intercept trading and chest opening.

The Blade Staff still yields item use when its offhand is occupied, and weapon ability handlers still respect shield input. Shield activation now uses Minecraft's normal entity interaction, block interaction, and item-use flow. Shield cooldowns and permissions remain enforced by the shield item.

Test with Tartsy alone and with Blade Staff in the main hand: right-click a villager, chest, door, and empty air. Trading and block interaction should work; empty-air use should guard. Repeat with a keyboard-bound Use key. Remove the shield and verify staff reflection still works.
