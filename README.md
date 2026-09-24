# 🟦 SquareMarkers

SquareMarkers automatically adds useful points of interest to a [squaremap](https://github.com/jpenilla/squaremap) web map on Fabric servers.
It is a Fabric-only port of [Pl3xMarkers](https://modrinth.com/plugin/pl3xmarkers).

[![GitHub Release](https://img.shields.io/github/v/release/SwordfishBE/SquareMarkers?display_name=release&logo=github)](https://github.com/SwordfishBE/SquareMarkers/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/SwordfishBE/SquareMarkers/total?logo=github)](https://github.com/SwordfishBE/SquareMarkers/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/7SYL5SOe?logo=modrinth&logoColor=white&label=Modrinth%20downloads)](https://modrinth.com/mod/squaremarkers)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1704723?logo=curseforge&logoColor=white&label=CurseForge%20downloads)](https://www.curseforge.com/minecraft/mc-mods/squaremarkers)

---

## ✨ Features

- Player-created area markers
- Nether portal markers
- End portal markers
- End gateway markers
- Lightning strike markers with a configurable lifetime
- Beacon markers
- Sign markers
- Cross-dimensional player markers
- Optional Open Parties and Claims claim areas
- Configurable layer priorities, labels, feedback, and individual marker types
- Fabric Essentials and Essential Commands warps
- Waystones markers

---

## 🧑‍🤝‍🧑 Player-created areas

1. Give several banners the same name in an anvil. Their color determines the area color.
2. Place a lodestone and one named banner on top of it at every corner.
3. SquareMarkers connects the outermost points and displays the area on squaremap.

Points may be added or removed later. Two aligned points create a circle; two non-aligned points create a rectangle. The default maximum area size is 512 blocks and can be changed in the configuration.

Basic HTML is accepted in area names: `b`, `i`, `u`, `br`, and a `span` with a hexadecimal text color.

---

## 🔨 Other automatic markers

- Travel through a Nether portal to register it.
- Activate a beacon to register it.
- Place and edit a sign directly above a lodestone to register it.
- End portals, end gateways, and lightning strikes are detected automatically.

---

## ⚙️ Configuration and commands

The configuration is generated at `config/squaremarkers/config.yml` on first startup. Run `/squaremarkers reload` from the console or as an operator after editing it. Marker data is stored in per-world JSON files below
`config/squaremarkers/`.

The public squaremap API does not expose permanent always-visible labels.
Therefore `always-show-name` and `always-show-text` use squaremap hover tooltips.
Click popups remain available where applicable.

---

## 📦 Installation

| Platform   | Link |
|------------|------|
| GitHub     | [Releases](https://github.com/SwordfishBE/SquareMarkers/releases) |
| Modrinth | [SquareMarkers](https://modrinth.com/mod/squaremarkers) |
| CurseForge | [SquareMarkers](https://www.curseforge.com/minecraft/mc-mods/squaremarkers) |


1. Download the latest JAR from your preferred platform above.
2. Place the JAR in your server's `mods/` folder.
3. Make sure [Fabric API](https://modrinth.com/mod/fabric-api) and [squaremap](https://modrinth.com/plugin/squaremap) are also installed.
4. Start Minecraft — the config file will be created automatically.

---

## 🧱 Building from Source

```bash
git clone https://github.com/SwordfishBE/SquareMarkers.git
cd SquareMarkers
chmod +x gradlew
./gradlew build
```

The remapped mod jar is written to `build/libs/`.

---

## 📄 Credits and license

SquareMarkers is based on Pl3xMarkers by Gjorgdy and retains its Apache-2.0
license. squaremap is developed by jpenilla and contributors.
