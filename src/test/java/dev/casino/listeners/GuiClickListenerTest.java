package dev.casino.listeners;

import dev.casino.gui.DefaultGuiManager;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class GuiClickListenerTest {

    private ServerMock server;
    private DefaultGuiManager guiManager;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server     = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        guiManager = new DefaultGuiManager();
        player     = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void clickEventIsRoutedToGuiManager() {
        guiManager.open(player, stubMenu());
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        new GuiClickListener(guiManager).onInventoryClick(event);

        assertTrue(event.isCancelled());
    }

    @Test
    void clickWithNoActiveMenuIsNotCancelled() {
        Inventory inv = server.createInventory(null, 9, Component.text("X"));
        player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        new GuiClickListener(guiManager).onInventoryClick(event);

        assertFalse(event.isCancelled());
    }

    // --- helper ---

    private GuiMenu stubMenu() {
        return new GuiMenu() {
            @Override public Inventory buildInventory(Player p) {
                return server.createInventory(null, 9, Component.text("Stub"));
            }
            @Override public void handleClick(InventoryClickEvent e) {}
            @Override public Component getTitle() { return Component.text("Stub"); }
            @Override public int getSize()        { return 9; }
        };
    }
}
