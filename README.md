# 🏠 FNWLobby

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,redis" />
</p>

<p align="center">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0-blue?style=flat-square" />
  <img alt="Minecraft" src="https://img.shields.io/badge/minecraft-1.8.8%20--%201.21.11-brightgreen?style=flat-square" />
  <img alt="Status" src="https://img.shields.io/badge/status-unstable%20%2F%20in%20development-orange?style=flat-square" />
  <img alt="License" src="https://img.shields.io/badge/license-private-lightgrey?style=flat-square" />
</p>

**FNWLobby** is the dedicated lobby plugin of the **FNW** Minecraft server network. It provides everything a network lobby needs: a customizable hotbar, GUI menus for server/gamemode selection, jump pads, an admin "guard" mode that locks player state, fishing-rod teleport, and Redis-based network integration.

👨‍💻 Developed by: [AmirVoid12](https://amirvoid12.ir) — Tabriz 🇮🇷

---

## ⚠️ Project Status

> This plugin, along with its related repositories, is **not fully stable yet**.
> We are actively working on fixing bugs and improving stability, and the project is **being updated at all times** ⏳.
> If you run into a bug or issue, feel free to open an issue — feedback is always welcome 🙏.

---

## 📜 About This Repository

This project was originally private and belonged entirely to **FlameNetwork** 🔒. For private reasons, FlameNetwork has been shut down 🛑, and as a result these sources have been made **public** 🌍. They are still being **debugged and updated continuously** 🔧, so expect frequent changes, fixes, and improvements over time.

---

## 🔗 Related Repositories

FNWLobby is part of a larger ecosystem that includes the following plugins. All of these projects work together as part of the FNW network, but just like FNWLobby, they are **not fully stable yet** and are under continuous development.

| Project | Description | Link |
|---|---|---|
| 🔥 **FNWCore** | The core plugin of the network — ranks, teleportation, homes, warps, vanish, tab/scoreboard, and more | [github.com/AmirVoid12/FNWCore](https://github.com/AmirVoid12/FNWCore) |
| 🌉 **FNWProxy** | Network proxy plugin/module, handles connection management and routing between servers | [github.com/AmirVoid12/FNWProxy](https://github.com/AmirVoid12/FNWProxy) |
| 🌫️ **FNWLimbo** | Limbo server (keeps players in a waiting/loading state) | [github.com/AmirVoid12/FNWLimbo](https://github.com/AmirVoid12/FNWLimbo) |

> 💡 FNWLobby is designed to run as a backend server behind FNWProxy, alongside FNWCore and FNWLimbo, as part of the full FNW network.

---

## ✨ Features

- 🎒 Fully configurable **hotbar** system (`hotbar.yml`) — define items, materials, lore, glow, and actions per slot
- 📋 **GUI menu system** (`menus.yml`) — build server selectors and custom menus without touching code
- 🦘 **Jump pads** — configurable launch velocity, admin add/remove commands, and live particle effects
- 🛡️ **Guard mode** — locks a player's walk speed, food, fire ticks, fall distance, applies speed/night vision effects, and forces clear weather/daytime (useful for admin vanish/spectate-style states)
- 🎣 **Fishing rod teleport** — cast and reel in to teleport, a classic lobby feature
- 🕳️ Void teleport protection back to spawn
- 🔌 **PlaceholderAPI** support (soft-dependency)
- 🔴 Redis integration for cross-server data and network awareness
- 📡 BungeeCord plugin messaging channel registered for cross-server transfers
- 🍂 **Folia-aware scheduling** — detects and adapts to Folia at runtime
- ⚙️ `/lobbycore` admin command — reload, toggle guard, open menus/sub-menus, manage jump pads, all with tab-completion

---

## 🖥️ Supported Versions

```
Minecraft 1.8.8  ->  1.21.11   (Spigot / Paper, Folia-aware)
```

---

## 🚀 Usage / Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/AmirVoid12/FNWLobby.git
   cd FNWLobby
   ```

2. **Build the project**
   FNWLobby resolves the Spigot 1.8.8 API directly from the official SpigotMC snapshot repository, so — unlike FNWCore/FNWLimbo — you do **not** need to manually place a `server.jar` in a `libs` folder. Just run:
   ```bash
   ./gradlew build
   ```
   The compiled plugin jar will be generated in `build/libs/`.

3. **Drop the jar** into your Spigot/Paper server's `plugins/` folder, start it once to generate `config.yml`, `menus.yml`, and `hotbar.yml`, then edit them (Redis connection, menus, hotbar items, jump pads) to fit your lobby.

4. Make sure this server points to the **same Redis instance** used by FNWProxy and the rest of the network, so cross-server features work correctly.

### ⚠️ Known Issue — Java Version

Just like FNWCore and FNWLimbo, FNWLobby targets **Spigot 1.8.8**, which **only runs on Java 8**. Since the Spigot API is pulled from Maven here instead of a local jar, you generally won't hit build-time Java errors — but make sure the **server** you actually run FNWLobby on is started with **Java 8**, as that's a requirement of Spigot 1.8.8 itself, not of this plugin.

---

## 🛠️ Built With

<p>
  <img src="https://skillicons.dev/icons?i=java" />
</p>

- ☕ Java 17 (toolchain, compiled with `--release 17`)
- 🐘 Gradle (Shadow + Run-Paper plugins)
- 🎮 Spigot API 1.8.8
- 🔌 PlaceholderAPI (soft-dependency)
- 🔴 Jedis (Redis) + Apache Commons Pool2 (relocated)
- 🍂 Folia-aware scheduler

---

## 📄 License

This project was originally private and belongs to **FlameNetwork**. Due to private reasons, FlameNetwork has been shut down, and these sources have since been made public. The code is provided as-is and is continuously debugged and updated.

---

<p align="center">
  Made with ❤️ by <a href="https://amirvoid12.ir">AmirVoid12</a>
</p>

<p align="center">
  ⭐ <b>Don't forget to star this repository!</b> ⭐
</p>
