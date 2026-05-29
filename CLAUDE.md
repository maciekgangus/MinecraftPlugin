# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Minecraft Casino Plugin (Paper API 1.21.4, Java 21). Currency is gold items (nuggets/ingots/blocks) taken from player inventory and Enderchest. All interaction happens through Chest GUIs opened via `/casino`. Outputs a single fat JAR.

## Build & Test Commands

```bash
# Build fat JAR → build/libs/CasinoPlugin-1.0.0-SNAPSHOT.jar
./gradlew build

# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "dev.casino.economy.GoldEconomyManagerTest"

# Build without tests
./gradlew shadowJar
```

CI runs `./gradlew build --no-daemon` which covers both compilation and tests.

## Architecture

`CasinoPlugin.java` is the composition root — it constructs all services, wires them together, and registers commands/listeners. No singletons or statics.

**`PluginContext`** (record) is the DI container passed to every game constructor. It holds: `EconomyManager`, `CasinoBettingData`, `StatsManager`, `GameRegistry`, `GuiManager`, and the main menu `GuiMenu`. Games call `context.openMainCasinoMenu(player)` for back-navigation.

**Adding a new game** — the only two files that need changing:
1. Create a class implementing `CasinoGame` in its own package under `dev.casino.games.<gamename>/`.
2. Add one line in `GameRegistrar.register()`: `context.games().register(new YourGame(context));`

**`CasinoGame` interface** — `start(Player)`, `stop(Player)` (must be idempotent), `getIcon()`, `getName()` (snake_case registry key), `getDisplayName()`.

**GUI layer** — never create `Inventory` directly. Use `GuiBuilder` (fluent, `size(rows)`, `item(slot, guiItem)`, `build()`). Open/close via `GuiManager`. `DefaultGuiManager` tracks active players; `GuiClickListener` delegates all inventory click events to the active `GuiMenu`.

**Economy** — `GoldEconomyManager` counts all three gold tiers (nuggets ÷9, ingots ×1, blocks ×9 ingots) from both inventory and Enderchest. Withdrawal consumes smallest denomination first and returns change. All amounts in ingot units.

**Stats persistence** — `StatsManager` reads/writes `plugins/CasinoPlugin/stats.yml` on enable/disable. `CasinoBettingData` holds per-game, per-UUID maps of `BettingData` (gold bet / gold won). When adding a new game with stats, add a section key in `StatsManager.loadStats()` and `saveStats()`.

## Key Rules

- Every new component must be backed by an interface.
- No server startup scripts, server configs, or `start.sh` / `.bat` files.
- English identifiers only (variable, method, and class names).
- Games must not modify inventory directly — always go through `EconomyManager` and `GuiManager`.
- `stop(Player)` must be safe to call when no session exists (idempotent).
