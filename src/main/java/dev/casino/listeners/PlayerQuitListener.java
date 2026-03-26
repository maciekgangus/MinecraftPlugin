package dev.casino.listeners;

import dev.casino.gui.GuiManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Cleans up the {@link GuiManager} mapping when a player disconnects. */
public final class PlayerQuitListener implements Listener {

    private final GuiManager guiManager;

    public PlayerQuitListener(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        guiManager.cleanup(event.getPlayer().getUniqueId());
    }
}
