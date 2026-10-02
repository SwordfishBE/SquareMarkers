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
- OPAC claim names and colors refresh automatically without restarting
- Optional live warp markers from Fabric Essentials, Essential Commands, and HuskHomes
- Optional live Waystones markers, including sharestones
- Optional last-death markers with configurable expiry
- Persistently hide/show individual markers and inspect status through admin commands
- Configurable layer priorities, labels, feedback, and individual marker types

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

### Marker visibility and status

These commands require console access or gamemaster command permissions:

```text
/squaremarkers status
/squaremarkers status details
/squaremarkers marker list
/squaremarkers marker list "minecraft:overworld" nether_portals
/squaremarkers marker list "minecraft:overworld" nether_portals 2
/squaremarkers marker hide "minecraft:overworld" nether_portals "100:64:200"
/squaremarkers marker show "minecraft:overworld" nether_portals "100:64:200"
```

`status` shows a compact seven-line summary. Green means enabled/visible,
yellow means disabled/hidden, and dark gray means not installed. Use
`status details` for integration names and per-world layer counts.

Use the exact world, layer and ID from `marker list`, or tab completion. Lists
have ten identities per page; labels help identify UUID-based markers. Quote
dimensions, coordinate IDs, and names containing spaces. Hiding affects all
squaremap viewers and does not delete the portal, warp, waystone or claim.
Changes become visible on squaremap's next web update. Hidden markers continue
to track source changes; showing restores their latest state immediately.

Exclusions are saved in `config/squaremarkers/hidden-markers.json` and survive
reloads, restarts and source deletion. `show` also clears exclusions for absent
sources, including disabled layers. A recreated marker with the same identity
stays hidden until shown. Warps use their source/name identity: renaming a warp
creates a new identity. OPAC visibility applies to an owner's complete claim
group in that dimension, rather than unstable polygon fragments; an owner name
change creates a new identity. Status reads cached data without scans or database
requests; enabled integration status is not a guarantee of integration health.

### Player death markers

Death locations are public on squaremap, so this feature defaults to disabled.
Enable it explicitly under `marker-settings`:

```yaml
  deaths:
    enabled: false
    priority: 50
    # Marker lifetime in seconds.
    lifetime: 1800
```

`lifetime` is in seconds (1–604800); the default is 30 minutes. Only the last
death per player is retained, including across dimensions. Markers use
`death.png` and show the player's name, coordinates and UTC death/expiry times.
They are not removed by respawning or collecting items, only by expiry or a new
death. Unmapped worlds are not recorded. With this feature enabled, death
locations are visible even for players hidden by squaremap's live-player setting;
disable death markers or hide that player's death marker if privacy is required.

Deaths are stored in `config/squaremarkers/deaths.json` on normal server saves
and shutdown. Reload/restart does not reset expiry; offline time counts too.
Changing the lifetime applies to new deaths only. The existing once-per-second
tick expires markers without scanning chunks or creating per-player threads.
At most 10000 latest-death records are retained; at the limit the oldest is
replaced. New config options are added without overwriting existing settings.

The configuration is generated at `config/squaremarkers/config.yml` on first startup. Run `/squaremarkers reload` from the console or as an operator after editing it. Marker data is stored in per-world JSON files below
`config/squaremarkers/`.

The public squaremap API does not expose permanent always-visible labels.
Therefore `always-show-name` and `always-show-text` use squaremap hover tooltips.
Click popups remain available where applicable.

OPAC claim names and colors are checked in memory every 30 seconds. Only changed
marker styles and labels are updated, without recalculating claim geometry.
Configure the interval in seconds with
`marker-settings.open-parties-and-claims.metadata-refresh-interval` (5–3600).
No check runs when OPAC is absent or its integration is disabled. squaremap's
own update interval adds a short delay before changes appear in the browser.

Warp markers appear in separate layers for Fabric Essentials, Essential Commands,
and HuskHomes. All three integrations are enabled by default when the matching
mod is installed. Fabric Essentials and Essential Commands changes are reflected
within about one second. HuskHomes changes use its event callbacks and an
asynchronous database refresh, with a five-minute reconciliation for changes
outside its commands. Existing configuration files automatically receive missing
warp options without changing other settings. To disable an integration or change
its layer priority, edit these entries under `marker-settings` in
`config/squaremarkers/config.yml`:

Important for HuskHomes: squaremap layers are visible to all map viewers. If you
use permission-restricted warps whose locations must stay private, set
`marker-settings.huskhomes-warps.enabled` to `false`; squaremap cannot apply
per-player warp permissions here.

```yaml
  fabric-essentials-warps:
    enabled: true
    priority: 50
  essential-commands-warps:
    enabled: true
    priority: 50
  huskhomes-warps:
    enabled: true
    priority: 50
```

Reload SquareMarkers after editing the configuration.

At server startup, SquareMarkers makes one background Modrinth request to check
for a newer listed Fabric release for the same Minecraft version. Only an
available update is announced at INFO level. An unavailable project or network
failure is silent at normal log levels.

When [Waystones](https://github.com/TwelveIterations/Waystones) is installed, its
waystones appear in a separate layer using the `warp_stone.png` icon. New,
renamed, and removed stones update through Waystones events; no recurring
Waystones scan runs. Sharestones are included by default. Unnamed or
undiscovered waystones are hidden by default. Configure this integration under
`marker-settings`:

```yaml
  waystones:
    enabled: true
    priority: 50
    include-sharestones: true
    include-undiscovered: false
```

Existing configuration files receive missing options automatically while
retaining their current values. Waystones itself and its dependencies are
optional; SquareMarkers does not need them when this integration is unused.

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
