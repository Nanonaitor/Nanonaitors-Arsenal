# 0.1.0-beta.2 — rendering corrections

- Fixed item client-extension selection during superclass construction. Weapon kind
  and shield type are not initialized at that point, so the previous conditional
  registration silently skipped the weapon poses.
- Register all weapon arm-pose enum extensions up front. The Scimitar render hook
  previously created a new enum value after HumanoidModel's switch table existed,
  causing the reported index-12 / length-12 crash.
- Restored the Morning Star third-person charge/release pose registration without
  changing its normal first-person holding position.
- Ported the original 1.12.2 Bulwark and Tartsy cube geometry, 64x64 texture UVs,
  mirror flags, child rotations, renderer transforms and item display transforms.
  No old texture has been stretched over the simplified modern placeholder geometry.
- Use the vanilla blocking arm with the original shield display transforms.
- Client smoke checks now verify weapon/shield extension installation, bake both
  original shield models, and check the humanoid arm-pose switch-table capacity.

These checks are not a substitute for testing positions and animation-mod
interactions inside the full Restless Horizons modpack.
