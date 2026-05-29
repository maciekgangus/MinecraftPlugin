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

    // --- helpers ---

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
