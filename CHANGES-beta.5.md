# 0.1.0-beta.5 — animated inventory lighting

- Confirmed in the client: inactive Flail and Ball & Chain used flat lighting,
  while their active separate-transforms wrappers used directional block lighting.
- Set gui_light to front on all 42 active model wrappers. Inventory lighting
  is selected from the wrapper, not just the inner GUI perspective.
- Added checks that both wrapper and GUI-perspective lighting stay flat.
- Animated inventory icons, empty hand models, world-space attack visuals and
  original texture colors are unchanged.
