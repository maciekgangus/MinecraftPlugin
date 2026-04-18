package dev.casino.games.blackjack;

import dev.casino.core.PluginContext;
import dev.casino.economy.GoldEconomyManager;
import dev.casino.games.DefaultGameRegistry;
import dev.casino.gui.DefaultGuiManager;
import net.kyori.adventure.text.Component;
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

import static org.junit.jupiter.api.Assertions.*;

class BlackjackGameTest {

    private GoldEconomyManager economy;
    private DefaultGuiManager guiManager;
    private BlackjackGame game;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        ServerMock server = MockBukkit.mock();
        var plugin = MockBukkit.createMockPlugin();
        economy = new GoldEconomyManager();
        guiManager = new DefaultGuiManager();
        PluginContext context = new PluginContext(plugin, economy, new DefaultGameRegistry(), guiManager);
        game = new BlackjackGame(context);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getNameReturnsBlackjack() {
        assertEquals("blackjack", game.getName());
    }

    @Test
    void getDisplayNameIsNotEmpty() {
        assertNotNull(game.getDisplayName());
    }

    @Test
    void getIconReturnsBanner() {
        assertEquals(Material.WHITE_BANNER, game.getIcon().getType());
    }

    @Test
    void startWithInsufficientFundsSendsMessage() {
        game.start(player);

        Component message = player.nextComponentMessage();
        assertNotNull(message, "Should have received an error message");
        assertTrue(PlainTextComponentSerializer.plainText().serialize(message).contains("You don't have enough gold"));

        assertFalse(guiManager.getActivePlayers().contains(player.getUniqueId()), 
            "Player should not have an active game session in GuiManager");

        assertEquals(0, economy.getBalance(player), "Balance should remain 0");
    }

    @Test
    void startWithSufficientFundsOpensGui() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);
        
        assertNotNull(player.getOpenInventory(), "Inventory view should not be null");
        assertNotNull(player.getOpenInventory().getTopInventory(), "Top inventory should not be null");
        assertEquals(InventoryType.CHEST, player.getOpenInventory().getTopInventory().getType());
        assertEquals(0, economy.getBalance(player), "Player did not lose gold for betting after game started");
        assertTrue(guiManager.getActivePlayers().contains(player.getUniqueId()), "No active players");
    }

    @Test
    void stopIsIdempotent() {
        assertDoesNotThrow(() -> game.stop(player));
        
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);
        assertDoesNotThrow(() -> game.stop(player));
        assertDoesNotThrow(() -> game.stop(player));
    }

    @Test
    void hitDrawsAnotherCard() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);

        int initialItems = countItems(player.getOpenInventory().getTopInventory());
        clickSlot(player, 39);

        int afterHitItems = countItems(player.getOpenInventory().getTopInventory());
        assertTrue(afterHitItems >= initialItems, "Hit did not increase card count");
    }

    @Test
    void standEndsGameAndDealerPlays() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);

        clickSlot(player, 41);

        ItemStack playAgain = player.getOpenInventory().getTopInventory().getItem(50);
        assertNotNull(playAgain);
        assertEquals(Material.ARROW, playAgain.getType(), "Game did not end");
    }

    @Test
    void playerCanBust() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);

        int maxHits = 12;
        for (int i = 0; i < maxHits; i++) {
            clickSlot(player, 39);
            ItemStack playAgain = player.getOpenInventory().getTopInventory().getItem(50);
            if (playAgain != null && playAgain.getType() == Material.ARROW) {
                Component msg = player.nextComponentMessage();
                assertNotNull(msg, "Should have received a game-over message");
                String text = PlainTextComponentSerializer.plainText().serialize(msg);
                assertTrue(text.contains("Bust"), "Message should indicate game outcome: " + text);
                return;
            }
        }
        fail("Player did not bust after " + maxHits + " hits");
    }

    private void clickSlot(PlayerMock player, int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        guiManager.handleClick(event);
    }

    private int countItems(org.bukkit.inventory.Inventory inv) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }
}
