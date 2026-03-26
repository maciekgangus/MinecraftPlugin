package dev.casino.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Central controller for all casino GUI screens.
 * Owns the Player→GuiMenu mapping and routes click events.
 */
public interface GuiManager {

    /**
     * Builds the menu inventory, registers the Player→GuiMenu mapping,
     * and opens the inventory for the player.
     * This is the ONLY way to open a casino GUI.
     */
    void open(Player player, GuiMenu menu);

    /**
     * Routes an InventoryClickEvent to the active GuiMenu for the player.
     * Primary guard: UUID map lookup. If no active casino menu → returns without cancelling.
     * If active menu found → cancels event and delegates to menu.
     */
    void handleClick(InventoryClickEvent event);

    /**
     * Removes the player from the active menu map.
     * Called by PlayerQuitListener and CasinoPlugin.onDisable().
     */
    void cleanup(UUID playerId);

    /** Returns a snapshot of all currently tracked player UUIDs. */
    Set<UUID> getActivePlayers();
}
