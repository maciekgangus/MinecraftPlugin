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

import java.util.Objects;

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
    void startOpensBettingGui() {
        game.start(player);
        assertNotNull(player.getOpenInventory());
        assertEquals(InventoryType.CHEST, player.getOpenInventory().getTopInventory().getType());
        
        ItemStack betDisplay = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.BET_DISPLAY);
        assertNotNull(betDisplay);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(betDisplay.getItemMeta().displayName())).contains("1 Gold"));
    }

    @Test
    void increaseDecreaseBetWorks() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10));
        game.start(player);

        clickSlot(player, BlackjackLayout.BET_INCREASE);
        ItemStack betDisplay = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.BET_DISPLAY);
        assertNotNull(betDisplay);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(betDisplay.getItemMeta().displayName())).contains("2 Gold"));

        clickSlot(player, BlackjackLayout.BET_DECREASE);
        betDisplay = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.BET_DISPLAY);
        assertNotNull(betDisplay);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(betDisplay.getItemMeta().displayName())).contains("1 Gold"));
    }


    @Test
    void initializeWithInsufficientFundsSendsMessage() {
        game.start(player);
        clickSlot(player, BlackjackLayout.BET_DEAL);

        Component message = player.nextComponentMessage();
        assertNotNull(message, "Should have received an error message");
        assertTrue(PlainTextComponentSerializer.plainText().serialize(message).contains("You don't have enough gold"));

        assertEquals(0, economy.getBalance(player), "Balance should remain 0");
    }

    @Test
    void initializeWithSufficientFundsStartsGame() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        game.start(player);
        
        clickSlot(player, BlackjackLayout.BET_INCREASE);
        

        clickSlot(player, BlackjackLayout.BET_DEAL);

        assertEquals(3, economy.getBalance(player), "Bet should have been withdrawn");
        
        ItemStack betDisplay = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.GAME_BET_DISPLAY);
        assertNotNull(betDisplay);
        assertTrue(PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(betDisplay.getItemMeta().displayName())).contains("2 Gold"));
    }

    @Test
    void hitDrawsAnotherCard() {
        ensureActiveGame();

        int initialItems = countItems(player.getOpenInventory().getTopInventory());
        clickSlot(player, BlackjackLayout.GAME_HIT);

        int afterHitItems = countItems(player.getOpenInventory().getTopInventory());
        
        ItemStack playAgain = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.GAME_PLAY_AGAIN);
        boolean isGameOver = playAgain != null && playAgain.getType() == Material.GOLDEN_SWORD;
        
        assertTrue(afterHitItems > initialItems || isGameOver, "Hit did not increase card count and game did not end");
    }

    @Test
    void standEndsGameAndDealerPlays() {
        ensureActiveGame();

        clickSlot(player, BlackjackLayout.GAME_STAND);

        ItemStack playAgain = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.GAME_PLAY_AGAIN);
        assertNotNull(playAgain);
        assertEquals(Material.GOLDEN_SWORD, playAgain.getType(), "Game did not end");
    }

    private void ensureActiveGame() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 100));
        int attempts = 0;
        while (attempts < 10) {
            game.start(player);
            clickSlot(player, BlackjackLayout.BET_DEAL);
            
            ItemStack playAgain = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.GAME_PLAY_AGAIN);
            if (playAgain == null || playAgain.getType() != Material.GOLDEN_SWORD) {
                return;
            }
            attempts++;
        }
        fail("Could not start an active game after 10 attempts (too many Natural 21s?)");
    }

    @Test
    void quitToBettingWorks() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);
        clickSlot(player, BlackjackLayout.BET_DEAL);

        clickSlot(player, BlackjackLayout.GAME_QUIT);

        ItemStack dealButton = player.getOpenInventory().getTopInventory().getItem(BlackjackLayout.BET_DEAL);
        assertNotNull(dealButton);
        assertEquals(Material.ARROW, dealButton.getType(), "Should be back in betting GUI");
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
