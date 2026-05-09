package dev.casino.core;

import dev.casino.economy.EconomyManager;
import dev.casino.games.GameRegistry;
import dev.casino.gui.GuiManager;
import dev.casino.gui.GuiMenu;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Immutable container for all plugin services.
 * Passed to game constructors so they can access economy and GUI
 * without knowing about each other.
 */
public record PluginContext(
        JavaPlugin plugin,
        EconomyManager economy,
        GameRegistry games,
        GuiManager gui,
        GuiMenu mainCasinoMenu) {

    /**
     * Opens the root casino menu (the screen that lists every registered game).
     * Call this from any in-game GUI when the player chooses a global Back action.
     */
    public void openMainCasinoMenu(Player player) {
        gui.open(player, mainCasinoMenu);
    }
}
