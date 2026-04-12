package dev.casino.games;

import dev.casino.core.PluginContext;
import dev.casino.games.derby.HorseRacingGame;

/**
 * Single point of truth for game registration.
 *
 * <p><strong>To add a new game:</strong>
 * <ol>
 *   <li>Implement {@link CasinoGame} in its own package under {@code dev.casino.games}.</li>
 *   <li>Add one line inside {@link #register}: {@code context.games().register(new YourGame(context));}</li>
 * </ol>
 * <p>Inside your game GUIs, call {@code context.openMainCasinoMenu(player)} for a Back-to-all-games action.
 * No other class needs to be modified.
 */
public final class GameRegistrar {

    private GameRegistrar() {}

    /**
     * Registers all casino games.
     * Called once from {@code CasinoPlugin.onEnable()}.
     */
    public static void register(PluginContext context) {
        context.games().register(new HorseRacingGame(context));
        // ── Add new games below ───────────────────────────────────────────
        // context.games().register(new SlotsGame(context));
        // context.games().register(new BlackjackGame(context));
        // ─────────────────────────────────────────────────────────────────
    }
}
