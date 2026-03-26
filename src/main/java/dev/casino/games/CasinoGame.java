package dev.casino.games;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Contract for every casino game.
 *
 * <p>Implement this interface and register in {@link GameRegistrar} to add a new game.
 * {@link #stop(Player)} must be idempotent: calling it when no session is active is safe.
 *
 * <p>{@code stop(Player)} is triggered by:
 * <ol>
 *   <li>PlayerQuitListener — when a player disconnects mid-game</li>
 *   <li>CasinoPlugin.onDisable() — iterates all active sessions via GuiManager</li>
 *   <li>The game itself — when its own end condition is reached</li>
 * </ol>
 */
public interface CasinoGame {

    /**
     * Starts a game session for the given player.
     * Implementations typically open a game-specific GUI.
     */
    void start(Player player);

    /**
     * Ends the game session for the given player.
     * Must be idempotent (safe to call with no active session).
     */
    void stop(Player player);

    /** Icon displayed in the main casino menu. Must not return {@code null}. */
    ItemStack getIcon();

    /** Unique snake_case identifier used as the registry key (e.g. {@code "slot_machine"}). */
    String getName();

    /** Display name rendered on the icon in the main casino menu. */
    Component getDisplayName();
}
