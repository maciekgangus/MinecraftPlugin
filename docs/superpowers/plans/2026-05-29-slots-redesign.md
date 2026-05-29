# Slots Game Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fully rewrite `SlotsGame` as a pure-Java, multi-player slot machine with animated cascading reels and a proper pay table, replacing the broken Python-IPC + global-lock implementation.

**Architecture:** Per-player `SlotSession` holds bet, spin state, inventory reference, and pre-drawn result. `SlotSymbol` enum owns weighted random draw and payout evaluation. `SlotsGame` drives the GUI and schedules one `BukkitRunnable` per session; the runnable directly mutates the player's open inventory each frame to avoid reopen flicker.

**Tech Stack:** Paper 1.21.4, MockBukkit 4.45 (tests), JUnit 5

---

## File Map

| Path | Action | Responsibility |
|------|--------|----------------|
| `games/slots/SlotsLayout.java` | Create | GUI slot-index constants |
| `games/slots/SlotSymbol.java` | Create | Enum: material, weight, payout multipliers, weighted draw, evaluate |
| `games/slots/SlotSession.java` | Create | Per-player mutable state (bet, spinning, task, inventory ref, result) |
| `games/slots/SlotsGame.java` | Full rewrite | `CasinoGame` impl: GUI open, bet controls, spin handler, animation loop, payout |
| `test/…/slots/SlotSymbolTest.java` | Create | Pure-Java tests for evaluate() |
| `test/…/slots/SlotsGameTest.java` | Create | MockBukkit tests: GUI, bet, spin, animation |

No files outside `games/slots/` are changed.

---

## Task 1: Data structures — `SlotsLayout`, `SlotSymbol`, `SlotSession`

**Files:**
- Create: `src/main/java/dev/casino/games/slots/SlotsLayout.java`
- Create: `src/main/java/dev/casino/games/slots/SlotSymbol.java`
- Create: `src/main/java/dev/casino/games/slots/SlotSession.java`
- Test: `src/test/java/dev/casino/games/slots/SlotSymbolTest.java`

- [ ] **Write failing test for `SlotSymbol.evaluate()`**

```java
// src/test/java/dev/casino/games/slots/SlotSymbolTest.java
package dev.casino.games.slots;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static dev.casino.games.slots.SlotSymbol.*;
import static org.junit.jupiter.api.Assertions.*;

class SlotSymbolTest {

    @Test void threeOfAKindDiamond() {
        assertEquals(100, evaluate(new SlotSymbol[]{DIAMOND, DIAMOND, DIAMOND}, 5));
    }

    @Test void threeOfAKindCoalRefund() {
        assertEquals(3, evaluate(new SlotSymbol[]{COAL, COAL, COAL}, 3));
    }

    @Test void threeOfAKindIronDoubles() {
        assertEquals(10, evaluate(new SlotSymbol[]{IRON, IRON, IRON}, 5));
    }

    @Test void twoOfAKindGoldPays() {
        // GOLD twoMultiplier = 2 → 2 * 2 = 4
        assertEquals(4, evaluate(new SlotSymbol[]{GOLD, GOLD, COAL}, 2));
    }

    @Test void twoOfAKindEmeraldRefund() {
        // EMERALD twoMultiplier = 1 → 1 * 1 = 1
        assertEquals(1, evaluate(new SlotSymbol[]{EMERALD, COAL, EMERALD}, 1));
    }

    @Test void twoOfAKindDiamondOnPositions1And3() {
        // reels[0] == reels[2] path
        assertEquals(15, evaluate(new SlotSymbol[]{DIAMOND, GOLD, DIAMOND}, 5));
    }

    @Test void twoOfAKindIronNoPay() {
        assertEquals(0, evaluate(new SlotSymbol[]{IRON, IRON, COAL}, 5));
    }

    @Test void twoOfAKindCoalNoPay() {
        assertEquals(0, evaluate(new SlotSymbol[]{COAL, EMERALD, COAL}, 10));
    }

    @Test void noMatch() {
        assertEquals(0, evaluate(new SlotSymbol[]{DIAMOND, GOLD, EMERALD}, 10));
    }

    @Test void drawReturnsValidSymbol() {
        assertNotNull(draw(new Random(42)));
    }

    @Test void drawDistributionFavoursCheaperSymbols() {
        // coal weight=35 should appear more often than diamond weight=5 over many draws
        Random rng = new Random(0);
        int diamonds = 0, coal = 0;
        for (int i = 0; i < 10_000; i++) {
            SlotSymbol s = draw(rng);
            if (s == DIAMOND) diamonds++;
            if (s == COAL) coal++;
        }
        assertTrue(coal > diamonds * 3, "Coal should appear far more often than diamond");
    }
}
```

- [ ] **Run to confirm compilation error (SlotSymbol doesn't exist)**

```
cd MinecraftPlugin && ./gradlew test --tests "dev.casino.games.slots.SlotSymbolTest" 2>&1 | tail -10
```
Expected: compile error.

- [ ] **Create `SlotsLayout.java`**

```java
// src/main/java/dev/casino/games/slots/SlotsLayout.java
package dev.casino.games.slots;

public final class SlotsLayout {
    public static final int REEL_1       = 2;
    public static final int REEL_2       = 4;
    public static final int REEL_3       = 6;
    public static final int BET_DECREASE = 10;
    public static final int BET_DISPLAY  = 13;
    public static final int BET_INCREASE = 15;
    public static final int SPIN         = 17;
    public static final int BACK         = 26;

    private SlotsLayout() {}
}
```

- [ ] **Create `SlotSymbol.java`**

```java
// src/main/java/dev/casino/games/slots/SlotSymbol.java
package dev.casino.games.slots;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public enum SlotSymbol {
    DIAMOND(Material.DIAMOND,    5,  20, 3),
    GOLD   (Material.GOLD_INGOT, 15,  8, 2),
    EMERALD(Material.EMERALD,    20,  4, 1),
    IRON   (Material.IRON_INGOT, 25,  2, 0),
    COAL   (Material.COAL,       35,  1, 0);

    private final Material material;
    private final int weight;
    private final int threeMultiplier;
    private final int twoMultiplier;

    private static final List<SlotSymbol> POOL;

    static {
        List<SlotSymbol> pool = new ArrayList<>();
        for (SlotSymbol s : values()) {
            for (int i = 0; i < s.weight; i++) pool.add(s);
        }
        POOL = List.copyOf(pool);
    }

    SlotSymbol(Material material, int weight, int threeMultiplier, int twoMultiplier) {
        this.material        = material;
        this.weight          = weight;
        this.threeMultiplier = threeMultiplier;
        this.twoMultiplier   = twoMultiplier;
    }

    public Material getMaterial() { return material; }

    public static SlotSymbol draw(Random rng) {
        return POOL.get(rng.nextInt(POOL.size()));
    }

    /** Returns total payout gold for given 3-reel result and bet amount. 0 = loss. */
    public static int evaluate(SlotSymbol[] reels, int bet) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) {
            return bet * reels[0].threeMultiplier;
        }
        SlotSymbol pair = findPair(reels);
        if (pair != null && pair.twoMultiplier > 0) {
            return bet * pair.twoMultiplier;
        }
        return 0;
    }

    private static SlotSymbol findPair(SlotSymbol[] reels) {
        if (reels[0] == reels[1]) return reels[0];
        if (reels[1] == reels[2]) return reels[1];
        if (reels[0] == reels[2]) return reels[0];
        return null;
    }
}
```

- [ ] **Create `SlotSession.java`**

```java
// src/main/java/dev/casino/games/slots/SlotSession.java
package dev.casino.games.slots;

import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

final class SlotSession {
    int bet = 1;
    boolean spinning = false;
    BukkitTask animationTask = null;
    Inventory openInventory = null;
    final SlotSymbol[] result = new SlotSymbol[3];
}
```

- [ ] **Run tests — confirm all pass**

```
cd MinecraftPlugin && ./gradlew test --tests "dev.casino.games.slots.SlotSymbolTest" 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`, 11 tests pass.

- [ ] **Commit**

```bash
git add src/main/java/dev/casino/games/slots/SlotsLayout.java \
        src/main/java/dev/casino/games/slots/SlotSymbol.java \
        src/main/java/dev/casino/games/slots/SlotSession.java \
        src/test/java/dev/casino/games/slots/SlotSymbolTest.java
git commit -m "feat(slots): add SlotSymbol, SlotSession, SlotsLayout data structures"
```

---

## Task 2: `SlotsGame` — GUI, bet controls, start/stop

**Files:**
- Create: `src/main/java/dev/casino/games/slots/SlotsGame.java`
- Create: `src/test/java/dev/casino/games/slots/SlotsGameTest.java`

- [ ] **Write failing tests for SlotsGame basics**

```java
// src/test/java/dev/casino/games/slots/SlotsGameTest.java
package dev.casino.games.slots;

import dev.casino.core.PluginContext;
import dev.casino.economy.CasinoBettingData;
import dev.casino.economy.GoldEconomyManager;
import dev.casino.economy.StatsManager;
import dev.casino.games.DefaultGameRegistry;
import dev.casino.gui.DefaultGuiManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SlotsGameTest {

    private ServerMock server;
    private GoldEconomyManager economy;
    private DefaultGuiManager guiManager;
    private SlotsGame game;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        var plugin = MockBukkit.createMockPlugin();
        economy = new GoldEconomyManager();
        guiManager = new DefaultGuiManager();
        var betting = new CasinoBettingData();
        var statsManager = new StatsManager(plugin, betting);
        PluginContext context = new PluginContext(plugin, economy, betting, statsManager,
                new DefaultGameRegistry(), guiManager, null);
        game = new SlotsGame(context);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test void getNameReturnsSlots() {
        assertEquals("slots", game.getName());
    }

    @Test void getIconReturnsNoteBlock() {
        assertEquals(Material.NOTE_BLOCK, game.getIcon().getType());
    }

    @Test void startOpens3RowGui() {
        game.start(player);
        assertEquals(27, player.getOpenInventory().getTopInventory().getSize());
    }

    @Test void stopIsIdempotent() {
        assertDoesNotThrow(() -> {
            game.stop(player);
            game.stop(player);
        });
    }

    @Test void stopAfterStartIsIdempotent() {
        game.start(player);
        assertDoesNotThrow(() -> {
            game.stop(player);
            game.stop(player);
        });
    }

    @Test void betDisplayShowsDefaultBetOf1() {
        game.start(player);
        ItemStack display = player.getOpenInventory().getTopInventory().getItem(SlotsLayout.BET_DISPLAY);
        assertNotNull(display);
        String text = PlainTextComponentSerializer.plainText().serialize(
                Objects.requireNonNull(display.getItemMeta().displayName()));
        assertTrue(text.contains("1"), "Bet display should show 1");
    }

    @Test void betIncreaseWithSufficientFundsWorks() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10));
        game.start(player);
        clickSlot(SlotsLayout.BET_INCREASE);

        String text = betDisplayText();
        assertTrue(text.contains("2"), "Bet should increase to 2");
    }

    @Test void betIncreaseBeyondBalanceIsIgnored() {
        // player has 0 gold — bet should stay at 1
        game.start(player);
        clickSlot(SlotsLayout.BET_INCREASE);

        String text = betDisplayText();
        assertTrue(text.contains("1"), "Bet should not increase past balance");
    }

    @Test void betDecreaseAtMinimumIsIgnored() {
        game.start(player);
        clickSlot(SlotsLayout.BET_DECREASE);

        String text = betDisplayText();
        assertTrue(text.contains("1"), "Bet should not go below 1");
    }

    @Test void betIncreaseThenDecreaseRoundTrips() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10));
        game.start(player);

        clickSlot(SlotsLayout.BET_INCREASE); // → 2
        clickSlot(SlotsLayout.BET_INCREASE); // → 3
        clickSlot(SlotsLayout.BET_DECREASE); // → 2
        clickSlot(SlotsLayout.BET_DECREASE); // → 1
        clickSlot(SlotsLayout.BET_DECREASE); // stays 1

        assertTrue(betDisplayText().contains("1"), "Bet should be back to 1");
    }

    @Test void spinWithInsufficientFundsSendsMessageAndKeepsBalance() {
        game.start(player); // 0 gold
        clickSlot(SlotsLayout.SPIN);

        assertNotNull(player.nextComponentMessage(), "Should receive error message");
        assertEquals(0, economy.getBalance(player));
    }

    // --- helper methods ---

    private String betDisplayText() {
        ItemStack item = player.getOpenInventory().getTopInventory().getItem(SlotsLayout.BET_DISPLAY);
        assertNotNull(item);
        return PlainTextComponentSerializer.plainText().serialize(
                Objects.requireNonNull(item.getItemMeta().displayName()));
    }

    void clickSlot(int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        guiManager.handleClick(event);
    }
}
```

- [ ] **Run to confirm compilation error (SlotsGame doesn't exist)**

```
cd MinecraftPlugin && ./gradlew test --tests "dev.casino.games.slots.SlotsGameTest" 2>&1 | tail -10
```
Expected: compile error.

- [ ] **Create `SlotsGame.java` — GUI and bet controls (no animation yet)**

```java
// src/main/java/dev/casino/games/slots/SlotsGame.java
package dev.casino.games.slots;

import dev.casino.core.PluginContext;
import dev.casino.games.CasinoGame;
import dev.casino.gui.GuiBuilder;
import dev.casino.gui.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlotsGame implements CasinoGame {

    private static final int MAX_BET = 64;
    private static final int MIN_BET = 1;

    private final PluginContext context;
    private final Map<UUID, SlotSession> sessions = new ConcurrentHashMap<>();
    private final Random rng = new Random();

    public SlotsGame(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start(Player player) {
        stop(player);
        SlotSession session = new SlotSession();
        sessions.put(player.getUniqueId(), session);
        openSlotGui(player, session);
    }

    @Override
    public void stop(Player player) {
        SlotSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        if (session.animationTask != null) {
            session.animationTask.cancel();
            if (session.spinning) {
                context.economy().deposit(player, session.bet);
            }
        }
    }

    @Override
    public ItemStack getIcon() {
        return createItem(Material.NOTE_BLOCK, Component.text("Slot Machine", NamedTextColor.GOLD));
    }

    @Override
    public String getName() { return "slots"; }

    @Override
    public Component getDisplayName() {
        return Component.text("Slot Machine", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    private void openSlotGui(Player player, SlotSession session) {
        GuiBuilder builder = new GuiBuilder()
                .title(Component.text("Slot Machine", NamedTextColor.DARK_GRAY))
                .size(3)
                .item(SlotsLayout.REEL_1, new GuiItem(reelItem(null), null))
                .item(SlotsLayout.REEL_2, new GuiItem(reelItem(null), null))
                .item(SlotsLayout.REEL_3, new GuiItem(reelItem(null), null))
                .item(SlotsLayout.BET_DECREASE, new GuiItem(
                        createItem(Material.RED_WOOL,
                                Component.text("Bet -1  (Current: " + session.bet + ")", NamedTextColor.RED)),
                        e -> {
                            if (!session.spinning && session.bet > MIN_BET) {
                                session.bet--;
                                openSlotGui(player, session);
                            }
                        }))
                .item(SlotsLayout.BET_DISPLAY, new GuiItem(
                        createItem(Material.GOLD_INGOT,
                                Component.text("Bet: " + session.bet + " Gold", NamedTextColor.GOLD)), null))
                .item(SlotsLayout.BET_INCREASE, new GuiItem(
                        createItem(Material.LIME_WOOL,
                                Component.text("Bet +1  (Current: " + session.bet + ")", NamedTextColor.GREEN)),
                        e -> {
                            if (!session.spinning && session.bet < MAX_BET
                                    && context.economy().getBalance(player) > session.bet) {
                                session.bet++;
                                openSlotGui(player, session);
                            }
                        }))
                .item(SlotsLayout.SPIN, new GuiItem(
                        createItem(Material.LEVER,
                                session.spinning
                                        ? Component.text("SPINNING...", NamedTextColor.GRAY, TextDecoration.BOLD)
                                        : Component.text("SPIN!", NamedTextColor.YELLOW, TextDecoration.BOLD)),
                        e -> handleSpin(player, session)))
                .item(SlotsLayout.BACK, new GuiItem(
                        createItem(Material.ARROW, Component.text("BACK", NamedTextColor.GRAY)),
                        e -> {
                            stop(player);
                            context.openMainCasinoMenu(player);
                        }));

        context.gui().open(player, builder.build());
        session.openInventory = player.getOpenInventory().getTopInventory();
    }

    private void handleSpin(Player player, SlotSession session) {
        if (session.spinning) return;
        if (!context.economy().hasFunds(player, session.bet)) {
            player.sendMessage(Component.text("Not enough gold to spin!", NamedTextColor.RED));
            return;
        }
        // animation implemented in next task
    }

    private ItemStack reelItem(SlotSymbol symbol) {
        if (symbol == null) {
            return createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        }
        return createItem(symbol.getMaterial(), Component.text(symbol.name(), NamedTextColor.WHITE));
    }

    private ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
```

- [ ] **Run tests — confirm all pass**

```
cd MinecraftPlugin && ./gradlew test --tests "dev.casino.games.slots.SlotsGameTest" 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`, all 10 tests pass.

- [ ] **Commit**

```bash
git add src/main/java/dev/casino/games/slots/SlotsGame.java \
        src/test/java/dev/casino/games/slots/SlotsGameTest.java
git commit -m "feat(slots): add SlotsGame with GUI and bet controls"
```

---

## Task 3: `SlotsGame` — spin logic, cascading animation, payout

**Files:**
- Modify: `src/main/java/dev/casino/games/slots/SlotsGame.java`
- Modify: `src/test/java/dev/casino/games/slots/SlotsGameTest.java`

- [ ] **Add failing spin tests to `SlotsGameTest`**

Add these three tests (after the existing ones, before the helper methods):

```java
@Test void spinDeductsBetImmediately() {
    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
    game.start(player);
    int before = economy.getBalance(player);

    clickSlot(SlotsLayout.SPIN);

    assertEquals(before - 1, economy.getBalance(player), "Bet should be deducted on spin start");
}

@Test void spinBlocksSecondClickWhileSpinning() {
    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
    game.start(player);
    int before = economy.getBalance(player);

    clickSlot(SlotsLayout.SPIN);
    clickSlot(SlotsLayout.SPIN); // second click must be a no-op

    assertEquals(before - 1, economy.getBalance(player), "Only one bet should be deducted");
}

@Test void spinResolvesAfterAnimation() {
    player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10));
    game.start(player);

    clickSlot(SlotsLayout.SPIN);

    // animation: 30 frames × 2 ticks/frame = 60 ticks; advance 62 for safety
    server.getScheduler().performTicks(62L);

    // SPIN button should be re-enabled (not "SPINNING...")
    ItemStack spinBtn = player.getOpenInventory().getTopInventory().getItem(SlotsLayout.SPIN);
    assertNotNull(spinBtn);
    String text = PlainTextComponentSerializer.plainText().serialize(
            Objects.requireNonNull(spinBtn.getItemMeta().displayName()));
    assertFalse(text.contains("SPINNING"), "Spin button should be re-enabled after animation completes");

    // Result message should be in the queue
    assertNotNull(player.nextComponentMessage(), "Player should receive a result message");
}
```

- [ ] **Run to confirm the three new tests fail**

```
cd MinecraftPlugin && ./gradlew test --tests "dev.casino.games.slots.SlotsGameTest.spinDeductsBetImmediately" \
  --tests "dev.casino.games.slots.SlotsGameTest.spinBlocksSecondClickWhileSpinning" \
  --tests "dev.casino.games.slots.SlotsGameTest.spinResolvesAfterAnimation" 2>&1 | tail -15
```
Expected: three FAILs (handleSpin does nothing currently).

- [ ] **Add imports to `SlotsGame.java` (top of file, after existing imports)**

```java
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
```

- [ ] **Replace `handleSpin` and add `resolveResult` in `SlotsGame.java`**

Replace the existing `handleSpin` stub with:

```java
private void handleSpin(Player player, SlotSession session) {
    if (session.spinning) return;
    if (!context.economy().hasFunds(player, session.bet)) {
        player.sendMessage(Component.text("Not enough gold to spin!", NamedTextColor.RED));
        return;
    }

    context.economy().withdraw(player, session.bet);
    context.betting().getSlotsStats(player).addBet(session.bet);
    context.statsManager().saveStatsAsync();

    session.spinning = true;
    session.result[0] = SlotSymbol.draw(rng);
    session.result[1] = SlotSymbol.draw(rng);
    session.result[2] = SlotSymbol.draw(rng);

    session.openInventory.setItem(SlotsLayout.SPIN,
            createItem(Material.LEVER,
                    Component.text("SPINNING...", NamedTextColor.GRAY, TextDecoration.BOLD)));

    BukkitTask task = new BukkitRunnable() {
        int tick = 0;

        @Override
        public void run() {
            // Guard: if a new session replaced this one, silently stop
            if (sessions.get(player.getUniqueId()) != session) {
                cancel();
                return;
            }
            // Guard: if the player closed/switched the inventory, refund and stop
            if (player.getOpenInventory().getTopInventory() != session.openInventory) {
                context.economy().deposit(player, session.bet);
                session.spinning = false;
                sessions.remove(player.getUniqueId());
                cancel();
                return;
            }

            tick++;

            if (tick <= 19) {
                // All three reels spin
                session.openInventory.setItem(SlotsLayout.REEL_1, reelItem(SlotSymbol.draw(rng)));
                session.openInventory.setItem(SlotsLayout.REEL_2, reelItem(SlotSymbol.draw(rng)));
                session.openInventory.setItem(SlotsLayout.REEL_3, reelItem(SlotSymbol.draw(rng)));
            } else if (tick == 20) {
                // Reel 1 locks
                session.openInventory.setItem(SlotsLayout.REEL_1, reelItem(session.result[0]));
                session.openInventory.setItem(SlotsLayout.REEL_2, reelItem(SlotSymbol.draw(rng)));
                session.openInventory.setItem(SlotsLayout.REEL_3, reelItem(SlotSymbol.draw(rng)));
            } else if (tick < 25) {
                session.openInventory.setItem(SlotsLayout.REEL_2, reelItem(SlotSymbol.draw(rng)));
                session.openInventory.setItem(SlotsLayout.REEL_3, reelItem(SlotSymbol.draw(rng)));
            } else if (tick == 25) {
                // Reel 2 locks
                session.openInventory.setItem(SlotsLayout.REEL_2, reelItem(session.result[1]));
                session.openInventory.setItem(SlotsLayout.REEL_3, reelItem(SlotSymbol.draw(rng)));
            } else if (tick < 30) {
                session.openInventory.setItem(SlotsLayout.REEL_3, reelItem(SlotSymbol.draw(rng)));
            } else {
                // Reel 3 locks → resolve
                session.openInventory.setItem(SlotsLayout.REEL_3, reelItem(session.result[2]));
                resolveResult(player, session);
                cancel();
            }
        }
    }.runTaskTimer(context.plugin(), 0L, 2L);

    session.animationTask = task;
}

private void resolveResult(Player player, SlotSession session) {
    int payout = SlotSymbol.evaluate(session.result, session.bet);

    if (payout > 0) {
        context.economy().deposit(player, payout);
        context.betting().getSlotsStats(player).addWon(payout);
        context.statsManager().saveStatsAsync();
        player.sendMessage(Component.text("You won " + payout + " gold!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    } else {
        player.sendMessage(Component.text("No win. Better luck next time!", NamedTextColor.RED));
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 0.8f);
    }

    session.spinning = false;
    session.animationTask = null;
    openSlotGui(player, session);
}
```

- [ ] **Run all slots tests — confirm all pass**

```
cd MinecraftPlugin && ./gradlew test --tests "dev.casino.games.slots.*" 2>&1 | tail -15
```
Expected: `BUILD SUCCESSFUL`, all 24 tests pass (11 SlotSymbolTest + 13 SlotsGameTest).

- [ ] **Commit**

```bash
git add src/main/java/dev/casino/games/slots/SlotsGame.java \
        src/test/java/dev/casino/games/slots/SlotsGameTest.java
git commit -m "feat(slots): implement cascading animation, spin logic, and payout resolution"
```

---

## Task 4: Full build verification

**Files:** none changed.

- [ ] **Confirm no stale `SlotsGame.java` at the old path**

```
find MinecraftPlugin/src -name "SlotsGame.java"
```
Expected: exactly one result — `...games/slots/SlotsGame.java`

- [ ] **Run the full test suite**

```
cd MinecraftPlugin && ./gradlew test 2>&1 | tail -20
```
Expected: `BUILD SUCCESSFUL`, zero failures.

- [ ] **Build the JAR**

```
cd MinecraftPlugin && ./gradlew build 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Verify JAR**

```
ls -lh MinecraftPlugin/build/libs/
```
Expected: `CasinoPlugin-1.0.0-SNAPSHOT.jar` present.

- [ ] **Final status check**

```
git status
```
Expected: clean working tree. If any uncommitted files remain, commit with:
```
git commit -m "chore(slots): finalize slots redesign"
```
