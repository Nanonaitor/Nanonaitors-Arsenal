# 0.1.0-beta.6

- Ball & Chain charging requires an empty offhand. Equipping another item mid-charge cancels it without a throw. An already-released throw can finish.
- Arsenal shield abilities exclusively own attack input. Tartsy's dash no longer also starts the main-hand weapon; release Attack before starting a new weapon attack.
- Server-side checks also reject weapon input during shield use/dashing, including ordinary melee and block attacks.
- Ball & Chain windup and throw now render once in world space, visible from either camera perspective, using the 1.12.2-style player-relative anchor. Removed the separate first-person attack rendering path.
- Animated inventory icons and hidden active held-item models remain unchanged.
- Network protocol updated: use this build on both client and server.

Manual checks in Restless Horizons: charge with empty/offhand occupied; equip an item mid-charge; hold Attack during Tartsy dash; compare Ball & Chain in first/third person while moving and looking around.
