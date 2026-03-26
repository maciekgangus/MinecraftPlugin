# Contributing — Adding a New Casino Game

## Prerequisites

| Tool | Minimum version |
|------|-----------------|
| JDK  | 21              |
| Gradle | 9.x (wrapper included) |

Run `./gradlew build` to verify everything compiles and all existing tests pass before starting.

---

## Step-by-step: add a new game

### 1. Create a package for the game

```
src/main/java/dev/casino/games/<your_game>/
```

Example: `src/main/java/dev/casino/games/slots/`

### 2. Implement `CasinoGame`

```java
package dev.casino.games.slots;

import dev.casino.core.PluginContext;
import dev.casino.games.CasinoGame;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class SlotsGame implements CasinoGame {

    private final PluginContext context;

    public SlotsGame(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start(Player player) {
        // Build your game GUI using GuiBuilder and open via context.gui().open(player, menu)
    }

    @Override
    public void stop(Player player) {
        // Clean up any per-player state; must be safe to call with no active session
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.LEVER);
    }

    @Override
    public String getName() {
        return "slots";          // unique snake_case key
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Slot Machine");
    }
}
```

Key points:
- Inject `PluginContext` in the constructor — never call static methods or use a service locator.
- Use `context.economy()` for gold operations (`withdraw`, `deposit`, `hasFunds`).
- Use `context.gui().open(player, menu)` to open any inventory; never call `player.openInventory()` directly.
- `stop()` **must be idempotent** — it is called when the server shuts down and when the player disconnects, even if no session is active.

### 3. Register the game in `GameRegistrar`

Open `src/main/java/dev/casino/games/GameRegistrar.java` and add one line:

```java
public static void register(PluginContext context) {
    context.games().register(new SlotsGame(context));   // ← add this
}
```

**This is the only existing file you need to modify.**

### 4. Write tests

Create a test class in `src/test/java/dev/casino/games/<your_game>/`:

```java
class SlotsGameTest {

    private ServerMock server;

    @BeforeEach void setUp() {
        server = MockBukkit.mock();
        MockBukkit.createMockPlugin();
    }

    @AfterEach void tearDown() { MockBukkit.unmock(); }

    @Test
    void stopWithNoSessionDoesNotThrow() {
        // build a minimal PluginContext stub and verify idempotent stop
    }
}
```

Run `./gradlew test` — all tests must stay green.

### 5. Verify the full build

```bash
./gradlew build
```

The resulting JAR at `build/libs/CasinoPlugin-*.jar` is ready to drop into a Paper 1.21.4 server's `plugins/` folder.

---

## Building a game GUI

Use `GuiBuilder` to construct menus declaratively:

```java
GuiMenu gameMenu = new GuiBuilder()
    .title(Component.text("Slot Machine"))
    .size(3)                               // rows (1–6)
    .item(13, new GuiItem(
        new ItemStack(Material.LEVER),
        event -> spin(player)              // click handler
    ))
    .build();

context.gui().open(player, gameMenu);
```

## Gold economy

All amounts are in **ingot units** (1 block = 9 ingots, 9 nuggets = 1 ingot):

```java
EconomyManager eco = context.economy();

if (!eco.hasFunds(player, 5)) {
    player.sendMessage(Component.text("Need at least 5 gold ingots."));
    return;
}

eco.withdraw(player, 5);   // takes gold from inventory → Enderchest
eco.deposit(player, 10);   // gives gold, drops overflow naturally
```

## Code style

- English-only names for variables, classes, and methods.
- Every public type must have an interface.
- No static state or singletons — use constructor injection.
- `stop()` must be idempotent.
- All new logic must be covered by JUnit 5 + MockBukkit tests.
