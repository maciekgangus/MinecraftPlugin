package dev.casino.listeners;

import dev.casino.gui.GuiManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Routes {@link InventoryClickEvent}s to {@link GuiManager}. */
public final class GuiClickListener implements Listener {

    private final GuiManager guiManager;

    public GuiClickListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        guiManager.handleClick(event);
    }
}
