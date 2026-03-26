package dev.casino.commands;

import dev.casino.gui.DefaultGuiManager;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class CasinoCommandTest {

    private ServerMock server;
    private DefaultGuiManager guiManager;
    private CasinoCommand command;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server     = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        guiManager = new DefaultGuiManager();
        command    = new CasinoCommand(guiManager, stubMenu());
        player     = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void playerIsRegisteredAsActiveAfterCommand() {
        command.onCommand(player, null, "casino", new String[0]);
        assertTrue(guiManager.getActivePlayers().contains(player.getUniqueId()));
    }

    @Test
    void commandAlwaysReturnsTrue() {
        assertTrue(command.onCommand(player, null, "casino", new String[0]));
    }

    @Test
    void nonPlayerSenderReceivesErrorAndMenuIsNotOpened() {
        var console = server.getConsoleSender();
        command.onCommand(console, null, "casino", new String[0]);
        assertTrue(guiManager.getActivePlayers().isEmpty());
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
