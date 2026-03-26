package dev.casino.core;

import dev.casino.economy.EconomyManager;
import dev.casino.games.GameRegistry;
import dev.casino.gui.GuiManager;
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
        GuiManager gui) {}
