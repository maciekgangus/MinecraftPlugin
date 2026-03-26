package dev.casino;

import dev.casino.commands.CasinoCommand;
import dev.casino.core.PluginContext;
import dev.casino.economy.GoldEconomyManager;
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

    @Override
    public void onEnable() {
        var economy  = new GoldEconomyManager();
        var games    = new DefaultGameRegistry();
        guiManager   = new DefaultGuiManager();

        var context  = new PluginContext(this, economy, games, guiManager);
        GameRegistrar.register(context);

        var mainMenu = new MainCasinoMenu(games);

        // Register command
        var casinoCommand = new CasinoCommand(guiManager, mainMenu);
        var cmd = getCommand("casino");
        if (cmd != null) cmd.setExecutor(casinoCommand);

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
        getLogger().info("CasinoPlugin disabled.");
    }
}
