# Nanonaitor's Arsenal

A Forge 1.12.2 weapon mod focused on specialized weapons with distinct combat
roles, counters, and active abilities rather than interchangeable damage tiers.

The initial development version is `0.1.0-dev`. Public releases will begin at
`1.0.0` after the first weapon set is implemented and tested.

## Design goals

- Every weapon should solve a specific combat problem or enable a recognizable playstyle.
- Special abilities should have clear costs, cooldowns, counters, and readable feedback.
- Vanilla Forge must load without any optional compatibility mod installed.
- Optional RLCraft-style integrations should activate only when their source mods exist.
- Mechanics should be data-driven enough to port to newer Minecraft versions later.

## Planned weapon families

- Anti-dragon shield
- Heavy two-handed bulwark
- Morning star
- Scimitar
- Flail
- Paired claws
- Battering ram
- Ball and chain

## Build

Use Java 8 and the supplied Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

Development JARs are created under `build/libs/`.

## License

Copyright 2026 Nanonaitor. See [LICENSE.md](LICENSE.md).
