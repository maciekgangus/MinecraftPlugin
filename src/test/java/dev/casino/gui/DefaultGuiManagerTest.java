package dev.casino.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DefaultGuiManagerTest {

    private ServerMock server;
    private DefaultGuiManager manager;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server  = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        manager = new DefaultGuiManager();
        player  = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- open ---

    @Test
    void openRegistersPlayerAsActive() {
        manager.open(player, stubMenu(null));
        assertTrue(manager.getActivePlayers().contains(player.getUniqueId()));
    }

    @Test
    void openCallsBuildInventory() {
        AtomicBoolean built = new AtomicBoolean(false);
        manager.open(player, new GuiMenu() {
            @Override public Inventory buildInventory(Player p) {
                built.set(true);
                return server.createInventory(null, 9, Component.text("T"));
            }
            @Override public void handleClick(InventoryClickEvent e) {}
            @Override public Component getTitle()  { return Component.text("T"); }
            @Override public int getSize()         { return 9; }
        });
        assertTrue(built.get());
    }

    // --- handleClick ---

    @Test
    void handleClickDoesNothingWhenNoActiveMenu() {
        Inventory inv = server.createInventory(null, 9, Component.text("X"));
        player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        manager.handleClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void handleClickCancelsEventAndDelegatesToMenu() {
        AtomicBoolean delegated = new AtomicBoolean(false);
        manager.open(player, stubMenu(() -> delegated.set(true)));
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        manager.handleClick(event);

        assertTrue(event.isCancelled());
        assertTrue(delegated.get());
    }

    // --- cleanup ---

    @Test
    void cleanupRemovesPlayer() {
        manager.open(player, stubMenu(null));
        manager.cleanup(player.getUniqueId());
        assertFalse(manager.getActivePlayers().contains(player.getUniqueId()));
    }

    @Test
    void cleanupUnknownUuidDoesNotThrow() {
        assertDoesNotThrow(() -> manager.cleanup(UUID.randomUUID()));
    }

    // --- getActivePlayers ---

    @Test
    void getActivePlayersIsSnapshot() {
        manager.open(player, stubMenu(null));
        Set<UUID> snapshot = manager.getActivePlayers();
        manager.cleanup(player.getUniqueId());
        assertTrue(snapshot.contains(player.getUniqueId()),
                "Snapshot should be independent of subsequent changes");
    }

    @Test
    void getActivePlayersIsUnmodifiable() {
        assertThrows(UnsupportedOperationException.class,
                () -> manager.getActivePlayers().add(UUID.randomUUID()));
    }

    // --- helper ---

    private GuiMenu stubMenu(Runnable onHandleClick) {
        return new GuiMenu() {
            @Override public Inventory buildInventory(Player p) {
                return server.createInventory(null, 9, Component.text("Stub"));
            }
            @Override public void handleClick(InventoryClickEvent e) {
                if (onHandleClick != null) onHandleClick.run();
            }
            @Override public Component getTitle() { return Component.text("Stub"); }
            @Override public int getSize()        { return 9; }
        };
    }
}
