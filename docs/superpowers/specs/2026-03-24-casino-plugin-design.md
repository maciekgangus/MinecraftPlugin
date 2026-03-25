# Casino Plugin — Architecture Design Spec
**Date:** 2026-03-24
**Status:** Approved
**Target:** Paper 1.21.4, Java 21, Gradle KTS, 4-developer team

---

## 1. Project Goals

A scalable Minecraft Casino plugin **template**. Four developers must be able to add new casino games independently without modifying each other's code. Currency is gold (ingots + nuggets) read from player inventory and Enderchest. All interaction is via a Chest GUI triggered by a single command.

Out of scope for this spec: concrete game implementations (Slots, Blackjack, etc.), database persistence, per-game configuration files, Vault integration, i18n.

---

## 2. Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 21 |
| API | Paper API 1.21.4 |
| Build | Gradle 8.x with Kotlin DSL (`build.gradle.kts`) |
| Unit tests | JUnit 5 + MockBukkit 3.x |
| DI approach | Constructor injection (no frameworks, no statics) |
| Libraries (shaded) | Lombok, FoliaLib, bStats |
| Libraries (compileOnly) | Lombok annotation processor |
| CI/CD | GitHub Actions |

Note: `boosted-yaml` is intentionally excluded from the initial template — configuration requirements are undefined at this stage. It can be added when a concrete config schema is designed. `XSeries` is excluded for the same reason — add if cross-version material compatibility is needed.

Note on Maven migration: the existing `pom.xml` and Eclipse IDE files (`.classpath`, `.factorypath`, `.project`, `.settings/`) are a throwaway prototype. They will be deleted and replaced with Gradle KTS. The only files carried forward are `src/` and `CLAUDE.md`.

---

## 3. Architecture

### 3.1 Dependency Injection Strategy — Approach B (Constructor Injection)

No static singletons. `CasinoPlugin.onEnable()` is the single composition root:

```
CasinoPlugin.onEnable()
 ├─ GoldEconomyManager economy = new GoldEconomyManager()
 ├─ DefaultGameRegistry registry = new DefaultGameRegistry()
 ├─ DefaultGuiManager guiManager = new DefaultGuiManager()
 ├─ PluginContext context = new PluginContext(plugin, economy, registry, guiManager)
 ├─ new GameRegistrar().registerAll(registry, context)   ← ONLY file devs touch
 ├─ MainCasinoMenu mainMenu = new MainCasinoMenu(registry, guiManager)
 ├─ CasinoCommand command = new CasinoCommand(mainMenu, guiManager)
 ├─ List<Lifecycle> lifecycles = List.of(/* future managers */)
 ├─ lifecycles.forEach(Lifecycle::onEnable)
 └─ register command + listeners (GuiClickListener, PlayerQuitListener)
```

`CasinoPlugin` stores the `List<Lifecycle>` as a field and iterates it in `onDisable()` in reverse order. Future managers implementing `Lifecycle` are added to this list — no other changes needed.

`PluginContext` is an immutable record holding all service references.

### 3.2 Game Registration — Open/Closed Principle

Adding a new game must NOT require modifying `CasinoPlugin.java`. The solution is a dedicated `GameRegistrar` class:

```java
public final class GameRegistrar {
    public void registerAll(GameRegistry registry, PluginContext context) {
        // Each developer adds exactly one line here:
        // registry.register(new SlotGame(context));
        // registry.register(new BlackjackGame(context));
    }
}
```

`GameRegistrar.java` is the **single** file all four developers collaborate on. Merge conflicts on this file are trivial (one line per game, no logic). `CasinoPlugin.onEnable()` is never modified after the initial scaffold.

### 3.3 Package Structure

```
src/main/java/dev/casino/
├── CasinoPlugin.java                  # Main plugin, composition root
│
├── core/
│   ├── PluginContext.java             # Immutable record: plugin, economy, registry, guiManager
│   └── Lifecycle.java                 # Interface: void onEnable(); void onDisable();
│                                      # Implemented by managers that need lifecycle hooks
│
├── economy/
│   ├── EconomyManager.java            # Interface (service contract)
│   └── GoldEconomyManager.java        # Impl: inventory + enderchest gold
│
├── gui/
│   ├── GuiMenu.java                   # Interface for all GUI screens
│   ├── GuiItem.java                   # ItemStack + InventoryClickEvent handler
│   ├── GuiBuilder.java                # Fluent builder → produces Inventory
│   ├── GuiManager.java               # Interface: open/handleClick/cleanup/getActivePlayers
│   └── DefaultGuiManager.java        # Impl: ConcurrentHashMap<UUID, GuiMenu>
│
├── commands/
│   └── CasinoCommand.java            # /casino → delegates to GuiManager.open()
│
├── games/
│   ├── CasinoGame.java               # Interface: start/stop/getIcon/getName/getDisplayName
│   ├── GameRegistry.java             # Interface: register/getAll/findByName
│   ├── GameRegistrar.java            # EDIT THIS FILE to add a new game
│   └── DefaultGameRegistry.java      # LinkedHashMap-based implementation (preserves order)
│
├── listeners/
│   ├── GuiClickListener.java         # InventoryClickEvent → GuiManager.handleClick()
│   └── PlayerQuitListener.java       # PlayerQuitEvent → GuiManager.cleanup() + game.stop()
│
└── menus/
    └── MainCasinoMenu.java           # Main casino menu (implements GuiMenu)
```

---

## 4. Interface Contracts

### `CasinoGame`
```java
public interface CasinoGame {
    /** Called when the player starts this game. Implementations open a game-specific GUI. */
    void start(Player player);

    /** Called when the game session must end. Must clean up all state for this player. */
    void stop(Player player);

    /** Icon displayed in the main casino menu. Must not return null. */
    ItemStack getIcon();

    /** Unique identifier used as the registry key. Snake_case, no spaces. */
    String getName();

    /** Adventure Component displayed as the item display name in the main menu. */
    Component getDisplayName();
}
```

`stop(Player)` is called by three triggers:
1. `PlayerQuitListener` when a player disconnects mid-game.
2. `CasinoPlugin.onDisable()` iterates all active sessions via `GuiManager` and calls `stop()`.
3. A game may call `stop()` on itself when its own end condition is reached.

Implementations must be idempotent — calling `stop()` on a player with no active session must not throw.

### `EconomyManager`
```java
public interface EconomyManager {
    /** Returns gold balance in ingot units. Counts from inventory + Enderchest.
     *  Conversion: 1 gold block = 9 ingots, 9 gold nuggets = 1 ingot (floor division). */
    int getBalance(Player player);

    /** Withdraws amount (in ingots) from player. Returns false if insufficient funds.
     *  Withdrawal order: inventory first, then Enderchest.
     *  Denominations consumed smallest-first (nuggets → ingots → blocks) to make change. */
    boolean withdraw(Player player, int amount);

    /** Deposits amount (in ingots) into player inventory as gold ingots.
     *  If inventory is full, deposited into Enderchest. If both full, dropped at feet. */
    void deposit(Player player, int amount);

    /** Returns true if getBalance(player) >= amount. */
    boolean hasFunds(Player player, int amount);
}
```

Note: `player.getEnderChest()` in Paper API always returns the Enderchest contents regardless of whether the player has it open — no special handling required.

### `GuiMenu`
```java
public interface GuiMenu {
    /** Builds and returns the Inventory for this screen. Called by GuiManager.open(). */
    Inventory buildInventory(Player player);

    /** Handles a click event routed from GuiManager. Implementations cancel the event
     *  and execute the GuiItem action at the clicked slot. */
    void handleClick(InventoryClickEvent event);

    /** Title rendered in the inventory title bar. */
    Component getTitle();

    /** Inventory size. Must be a multiple of 9, between 9 and 54. */
    int getSize();
}
```

### `GuiManager`
```java
public interface GuiManager {  // not in original spec — added per reviewer C-3
    /** Builds the menu inventory, registers Player→GuiMenu mapping, and opens it. */
    void open(Player player, GuiMenu menu);

    /** Routes click to the active GuiMenu for this player. Cancels event if player
     *  has an active casino GUI. No-ops if player has no active GUI (not a casino inv). */
    void handleClick(InventoryClickEvent event);

    /** Removes the player's active GUI mapping. Calls game.stop() if a game is active. */
    void cleanup(UUID playerId);

    /** Returns all currently tracked players. Used by onDisable() to flush sessions. */
    Set<UUID> getActivePlayers();
}
```

The `Map<UUID, GuiMenu>` inside `DefaultGuiManager` is a `ConcurrentHashMap` to be safe under Folia region-threaded execution.

### `GameRegistry`
```java
public interface GameRegistry {
    void register(CasinoGame game);
    Collection<CasinoGame> getAll();         // returns Collection<CasinoGame> in insertion order
    Optional<CasinoGame> findByName(String name);
}
```

### `Lifecycle`
```java
public interface Lifecycle {
    void onEnable();
    void onDisable();
}
```

Implemented by managers that need startup/shutdown hooks (e.g., future `DatabaseManager`). `CasinoPlugin` calls `onEnable()` / `onDisable()` on all registered `Lifecycle` implementors in order. Base template managers (`GoldEconomyManager`, `DefaultGameRegistry`) do not implement it — they are stateless or their state is managed externally.

---

## 5. GUI Click Routing

```
InventoryClickEvent
 └─ GuiClickListener.onInventoryClick()
     └─ GuiManager.handleClick(event)
         ├─ lookup player UUID in Map<UUID, GuiMenu>
         ├─ if not found: return (not a casino inventory — do NOT cancel)
         ├─ cancel event (prevents item theft)
         └─ activeMenu.handleClick(event)
             └─ GuiItem.getAction().accept(event)  // Consumer<InventoryClickEvent>
```

The UUID-based lookup is the canonical guard. `GuiClickListener` must check `event.getView().getType()` only as a secondary fast-exit (e.g., ignore `CRAFTING` inventory type). The primary guard is always the UUID map.

---

## 6. Plugin Descriptor

Use `plugin.yml` (legacy format, fully supported on Paper 1.21.4 via the default plugin loader). `paper-plugin.yml` (Paper's new bootstrapper format) is intentionally deferred — it requires a `PluginBootstrap` class and changes how dependencies are loaded, adding complexity without benefit for this template.

```yaml
# src/main/resources/plugin.yml
name: CasinoPlugin
version: '${version}'
main: dev.casino.CasinoPlugin
api-version: '1.21'
description: Minecraft Casino Plugin Template
authors: [Team]
commands:
  casino:
    description: Opens the main casino menu
    usage: /casino
    permission: casino.use
permissions:
  casino.use:
    description: Allows using the casino
    default: true
```

---

## 7. Build Configuration

### `build.gradle.kts` key points
- Group: `dev.casino`, version: `1.0.0-SNAPSHOT`
- `shadowJar` task (`com.github.johnrengelman.shadow`) produces the final shaded `.jar`
- `paper-api:1.21.4-R0.1-SNAPSHOT` is `compileOnly`
- `lombok` is `compileOnly` + `annotationProcessor`
- `FoliaLib` and `bstats-bukkit` are `implementation` (shaded into output jar)
- Test dependencies: `junit-jupiter`, `mockbukkit-mockbukkit` (latest 3.x)
- `processResources` task filters `plugin.yml` to inject `${version}`
- Default `jar` task is disabled; `build` depends on `shadowJar`

### GitHub Actions Workflow
```yaml
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - run: ./gradlew build
      - uses: actions/upload-artifact@v4
        with:
          name: CasinoPlugin
          path: build/libs/*-all.jar
```

---

## 8. Testing Strategy

| Component | Test type | Key assertions |
|---|---|---|
| `GoldEconomyManager` | MockBukkit | Known inventory → `getBalance()` returns correct ingot count; `withdraw()` removes correct items; `hasFunds()` edge cases |
| `DefaultGameRegistry` | Pure JUnit 5 | `register()` + `getAll()` order; `findByName()` present/absent; duplicate key behavior |
| `DefaultGuiManager` | MockBukkit | `open()` registers UUID mapping; `handleClick()` routes to correct menu; `cleanup()` removes mapping |
| `CasinoCommand` | MockBukkit | Console sender rejected; player sender opens MainCasinoMenu via GuiManager |

---

## 9. Developer Workflow — Adding a New Game

1. Create `src/main/java/dev/casino/games/<gamename>/MyGame.java` implementing `CasinoGame`.
2. Add `registry.register(new MyGame(context));` in `GameRegistrar.registerAll()`.
3. No other files need modification.

Full details in `CONTRIBUTING.md`.

---

## 10. What Is Out of Scope

- Concrete game implementations (Slots, Blackjack, Roulette, etc.)
- Database / persistence layer
- Per-game config files
- Vault / economy plugin integration
- Localization / i18n
- `paper-plugin.yml` bootstrapper (deferred — use `plugin.yml`)
- `boosted-yaml` (deferred — no config schema defined yet)
