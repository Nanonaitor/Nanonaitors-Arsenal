# 0.1.0-beta.3 — active ability animation compatibility

- Morning Star, Battering Ram, dual Scimitar guard, and Blade Staff reflect now
  apply their active third-person poses after the normal model animation pass.
  This includes animation callbacks inside that pass. Idle movement is unchanged.
- Scimitar guard now activates its previously disconnected model predicate.
  First-person guard explicitly renders a mirrored crossed pair instead of
  depending on the vanilla shield/hand animation.
- Blade Staff's held-model visibility and the detached spinning visual use the
  same local reflect timer, eliminating disagreement when use-state packets arrive.
- Flail active hand views use the supplied animated sprite again. Inventory,
  dropped-item and item-frame views keep the ordinary static sprite.
- Tartsy dash plays the original 1.12.2 sweep sound once when a dash starts.
- Added client checks for animated Flail hand models versus static GUI icons
  across every tier. The render compatibility hook is client-only.

Full Restless Horizons visual testing is still required, especially with optional
animation/shader mods. No unrelated animation settings are disabled.
