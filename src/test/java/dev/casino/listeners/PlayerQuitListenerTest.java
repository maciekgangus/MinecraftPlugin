package dev.casino.listeners;

import dev.casino.gui.DefaultGuiManager;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class PlayerQuitListenerTest {

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
    void quittingPlayerIsRemovedFromActiveMap() {
        guiManager.open(player, stubMenu());
        assertTrue(guiManager.getActivePlayers().contains(player.getUniqueId()));

        PlayerQuitEvent event = new PlayerQuitEvent(player, Component.empty(),
                PlayerQuitEvent.QuitReason.DISCONNECTED);
        new PlayerQuitListener(guiManager).onPlayerQuit(event);

        assertFalse(guiManager.getActivePlayers().contains(player.getUniqueId()));
    }

    @Test
    void quitForPlayerWithNoActiveMenuDoesNotThrow() {
        PlayerQuitEvent event = new PlayerQuitEvent(player, Component.empty(),
                PlayerQuitEvent.QuitReason.DISCONNECTED);
        assertDoesNotThrow(() -> new PlayerQuitListener(guiManager).onPlayerQuit(event));
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
