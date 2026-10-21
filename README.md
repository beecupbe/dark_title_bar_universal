<div align="center">

# Dark Title Bar (Universal)

**A dark native Windows title bar for Minecraft — one jar from 1.6.4 to 26.x.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.6.4%20→%2026.x-62B47A)
![Loaders](https://img.shields.io/badge/loaders-Forge%20%7C%20NeoForge%20%7C%20Fabric-blue)
![Side](https://img.shields.io/badge/side-client-lightgrey)
![License](https://img.shields.io/badge/license-MIT-green)

</div>

---

**This simple mod just makes the Minecraft window title bar dark.**

It does **not** touch Minecraft code: no Mixins, no coremod transformers, etc. It only calls the Windows API, which is why a single jar works on almost every Minecraft version and mod loader.

## Features

- **One jar for everything** — Forge, NeoForge, Fabric, Quilt from 1.6.4 to the newest releases
- **Zero dependencies** — no Fabric API, no Mixins, etc.
- **Conflict-free** — Minecraft classes are never modified, so it fits into any modpack without any errors.

## Compatibility

| Minecraft | Loader | How it loads |
|---|---|---|
| 1.6.4 – 1.12.2 | Forge | LaunchWrapper `TweakClass` |
| 1.13 – 26.x | Forge | `META-INF/mods.toml` |
| 1.20.1 | NeoForge | `META-INF/mods.toml` |
| 1.20.2 – 26.x | NeoForge | `mods.toml` / `neoforge.mods.toml` |
| 1.14+ | Fabric / Quilt | `fabric.mod.json` |

**Requirements**

- Windows 10 version 1809 or newer, or Windows 11

- Client-side only!!

## Installation

1. Download the latest `darktitlebar-universal-*.jar` from [Releases](../../releases).
2. Drop it into the `mods` folder.
3. Launch the game — that's it. There is no config.

To confirm it works, look for this line in the log:

```text
[DarkTitleBar] dark title bar applied to 0x...
```

## How it works

1. Every loader gets its own one-line entry point, and all of them call the same loader-agnostic core.
2. The core finds the game window through WinAPI (`EnumWindows` → own process → visible window with a caption).
3. It calls `DwmSetWindowAttribute` with `DWMWA_USE_IMMERSIVE_DARK_MODE` (attribute `20`, falling back to `19` for old Windows 10 builds, like 1809-1909) and asks Windows to redraw the frame.
4. A background thread keeps watching for new windows of the process, so recreated windows get darkened as well.

Windows calls go through JNA, which Minecraft already ships starting with 1.8.8+ versions. Minecraft 1.6.4-1.8 has no JNA, so the mod carries a trimmed copy of JNA 5.14.0 (classes plus `jnidispatch.dll` for x86, x64 and ARM64). It extracts that copy to `%TEMP%\darktitlebar\` and loads it in an isolated class loader, so it never clashes with other JNA copies in a modpack. If JNA is already on the classpath, the mod uses that instead.

## Credits

Inspired by these mods, which use the same DWM approach on modern versions:

- [Dark Title Bar For Minecraft](https://github.com/prayoadmii-software/Dark-Title-Bar-For-Minecraft) by prayoadmii-software (Fabric)
- [DarkTitleBar](https://www.curseforge.com/minecraft/mc-mods/darktitlebar) by williamist (Fabric)
- [DarkTitleBarForge](https://github.com/yiyuyan/DarkTitleBarForge) by yiyuyan (Forge)

## License

[MIT](LICENSE)
