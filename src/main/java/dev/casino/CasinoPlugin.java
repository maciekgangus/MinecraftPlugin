package dev.casino;

import dev.casino.commands.CasinoCommand;
import dev.casino.commands.LeaderBoardCommand;
import dev.casino.core.PluginContext;
import dev.casino.economy.CasinoBettingData;
import dev.casino.economy.GoldEconomyManager;
import dev.casino.economy.StatsManager;
import dev.casino.games.DefaultGameRegistry;
import dev.casino.games.GameRegistrar;
import dev.casino.games.MainCasinoMenu;
import dev.casino.gui.DefaultGuiManager;
import dev.casino.listeners.GuiClickListener;
import dev.casino.listeners.PlayerQuitListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Composition root: constructs all services, wires dependencies, and registers
 * commands/listeners. Every dependency is injected via constructors — no statics.
 */
public final class CasinoPlugin extends JavaPlugin {

    private DefaultGuiManager guiManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        var economy  = new GoldEconomyManager();
        var betting  = new CasinoBettingData();
        var games    = new DefaultGameRegistry();
        guiManager   = new DefaultGuiManager();

        statsManager = new StatsManager(this, betting);
        statsManager.loadStats();

        var mainMenu = new MainCasinoMenu(games);
        var context  = new PluginContext(this, economy, betting, statsManager, games, guiManager, mainMenu);
        GameRegistrar.register(context);

        // Register command
        var casinoCommand = new CasinoCommand(guiManager, mainMenu);
        var cmd = getCommand("casino");
        if (cmd != null) cmd.setExecutor(casinoCommand);

        var leaderBoardCommand = new LeaderBoardCommand(context);
        var lbcmd = getCommand("leaderboard");
        if (lbcmd != null) lbcmd.setExecutor(leaderBoardCommand);

        // Register listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new GuiClickListener(guiManager), this);
        pm.registerEvents(new PlayerQuitListener(guiManager), this);

        getLogger().info("CasinoPlugin enabled — " + games.getAll().size() + " game(s) registered.");
    }

    @Override
    public void onDisable() {
        if (guiManager != null) {
            guiManager.getActivePlayers().forEach(guiManager::cleanup);
        }
        if (statsManager != null) {
            statsManager.saveStats();
        }
        getLogger().info("CasinoPlugin disabled.");
    }
}
