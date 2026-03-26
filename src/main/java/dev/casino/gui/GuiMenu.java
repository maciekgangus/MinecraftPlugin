package dev.casino.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * A single screen in the casino GUI system.
 *
 * <p>Always open menus via {@link GuiManager#open(Player, GuiMenu)} —
 * never call {@code player.openInventory()} directly.
 */
public interface GuiMenu {

    /**
     * Builds and returns the Inventory for this screen.
     * Called once per open by {@link GuiManager}. Must not rely on previous state.
     */
    Inventory buildInventory(Player player);

    /**
     * Handles a click routed from {@link GuiManager}.
     * The event is already cancelled before this method is called.
     */
    void handleClick(InventoryClickEvent event);

    /** Title displayed in the inventory title bar. */
    Component getTitle();

    /** Inventory size: multiple of 9, between 9 and 54 inclusive. */
    int getSize();
}
