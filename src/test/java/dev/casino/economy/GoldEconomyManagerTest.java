package dev.casino.economy;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class GoldEconomyManagerTest {

    private ServerMock server;
    private GoldEconomyManager economy;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server  = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        economy = new GoldEconomyManager();
        player  = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- getBalance ---

    @Test
    void balanceIsZeroForEmptyInventory() {
        assertEquals(0, economy.getBalance(player));
    }

    @Test
    void balanceCountsIngots() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        assertEquals(5, economy.getBalance(player));
    }

    @Test
    void balanceConvertsNineNuggetsToOneIngot() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 9));
        assertEquals(1, economy.getBalance(player));
    }

    @Test
    void balanceFloorsPartialNuggets() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 8));
        assertEquals(0, economy.getBalance(player));
    }

    @Test
    void balanceConvertsBlockToNineIngots() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_BLOCK, 1));
        assertEquals(9, economy.getBalance(player));
    }

    @Test
    void balanceCombinesInventoryAndEnderchest() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        player.getEnderChest().addItem(new ItemStack(Material.GOLD_INGOT, 2));
        assertEquals(5, economy.getBalance(player));
    }

    // --- hasFunds ---

    @Test
    void hasFundsReturnsTrueWhenBalanceExactlyMatches() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        assertTrue(economy.hasFunds(player, 5));
    }

    @Test
    void hasFundsReturnsFalseWhenBalanceInsufficient() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 4));
        assertFalse(economy.hasFunds(player, 5));
    }

    // --- withdraw ---

    @Test
    void withdrawReturnsFalseAndChangesNothingWhenInsufficientFunds() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        assertFalse(economy.withdraw(player, 5));
        assertEquals(3, economy.getBalance(player));
    }

    @Test
    void withdrawRemovesExactIngotsFromInventory() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 10));
        assertTrue(economy.withdraw(player, 4));
        assertEquals(6, economy.getBalance(player));
    }

    @Test
    void withdrawConsumesNuggetsBeforeIngots() {
        // 9 nuggets + 1 ingot = 2 ingots total; withdraw 1 → should consume nuggets first
        player.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 9));
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        economy.withdraw(player, 1);
        int nuggets = sum(player.getInventory(), Material.GOLD_NUGGET);
        int ingots  = sum(player.getInventory(), Material.GOLD_INGOT);
        assertEquals(0, nuggets);
        assertEquals(1, ingots);
    }

    @Test
    void withdrawTakesFromInventoryBeforeEnderchest() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        player.getEnderChest().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        economy.withdraw(player, 5);
        assertEquals(0, sum(player.getInventory(), Material.GOLD_INGOT));
        assertEquals(5, sum(player.getEnderChest(), Material.GOLD_INGOT));
    }

    @Test
    void withdrawSpansInventoryAndEnderchestWhenNeeded() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        player.getEnderChest().addItem(new ItemStack(Material.GOLD_INGOT, 5));
        assertTrue(economy.withdraw(player, 7));
        assertEquals(1, economy.getBalance(player));
    }

    // --- deposit ---

    @Test
    void depositAddsIngotsToInventory() {
        economy.deposit(player, 5);
        assertEquals(5, sum(player.getInventory(), Material.GOLD_INGOT));
    }

    // --- helper ---

    private int sum(org.bukkit.inventory.Inventory inv, Material mat) {
        return inv.all(mat).values().stream().mapToInt(ItemStack::getAmount).sum();
    }
}
