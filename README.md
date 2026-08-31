<div align="center">

<img src="assets/logo.png" alt="Hopper Tweaks" width="256"/>

# Hopper Tweaks

A Fabric mod that makes the hopper transfer amount configurable — per world,
with a command or a config screen, and fully compatible with Carpet, Carpet
TIS Addition (hopper counters / scounter) and lithium (with its hopper
optimization disabled).

</div>

---

## Features

- **Configurable transfer amount** — hoppers move up to 64 items per cycle
  instead of the vanilla 1.
- **Per world** — the value is stored in `hoppertweaks.properties` inside each
  world's save folder, so every world keeps its own speed.
- **Stack aware** — a transfer never exceeds the source stack or the free
  space of the destination. A nearly full chest simply receives what fits.
- **Composter exception** — pushing into composters always stays at the
  vanilla speed of one item per cycle.
- **Carpet hopper counter compatible** — counter lines keep draining; the mod
  never leaves a transfer half-done, so `setChanged` always fires and the
  counters see correct amounts.
- **Scounter compatible** — Carpet TIS Addition's `hopperNoItemCost` /
  `/scounter` hooks count the exact transferred amount, also for partial
  inserts.
- **Config screen** — edit the running world's speed from Mod Menu
  (Cloth Config required). `/hopperspeed` without arguments prints the current
  value.

## Usage

```
/hopperspeed                 show the current world's hopper speed
/hopperspeed <1-64>          set the hopper speed for this world
```

Requires operator permissions. The value is saved with the world and restored
on the next start.

## Supported versions

Each Minecraft version lives on its own branch; `main` tracks the newest one.

| Minecraft     | Java | Branch       | Fabric API       | Cloth Config | Mod Menu |
| ------------- | ---- | ------------ | ---------------- | ------------ | -------- |
| 26.2          | 25   | `main`       | 0.158.0+26.2     | 26.2.155     | 20.0.1   |
| 26.1.2        | 25   | `mc/26.1.2`  | 0.155.2+26.1.2   | 26.1.154     | 18.0.0   |
| 26.1.1        | 25   | `mc/26.1.1`  | 0.155.2+26.1.2   | 26.1.154     | 18.0.0   |
| 26.1          | 25   | `mc/26.1`    | 0.155.2+26.1.2   | 26.1.154     | 18.0.0   |
| 1.21.11       | 21   | `mc/1.21.11` | 0.141.6+1.21.11  | 21.11.153    | 17.0.x   |

Artifacts are suffixed accordingly, e.g. `hopper-tweaks-1.0.0+mc26.2.jar`.

## Requirements

- [Fabric Loader](https://fabricmc.net/use/) (0.19.3 or newer)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config API](https://modrinth.com/mod/cloth-config) (for the config
  screen)
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) for the config button

Note: if [lithium](https://modrinth.com/mod/lithium) is installed, its hopper
optimization must be disabled for this mod to take effect
(`mixin.block.hopper = false` in `config/lithium.properties`). Lithium
replaces the vanilla transfer code that this mod modifies; every other
lithium optimization stays active.

## Building

Java 21 (1.21.11 branch) or Java 25 (26.x branches), then:

```
# Windows
.\gradlew.bat build

# Linux/macOS
./gradlew build
```

The finished jar is written to `build/libs/`.

## License

Distributed under the [MIT License](LICENSE) — © 2026 UnlimitedBytes.
