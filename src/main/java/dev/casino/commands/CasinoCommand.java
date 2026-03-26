package dev.casino.commands;

import dev.casino.gui.GuiManager;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Opens the main casino menu when a player runs {@code /casino}. */
public final class CasinoCommand implements CommandExecutor {

    private final GuiManager guiManager;
    private final GuiMenu mainMenu;

    public CasinoCommand(GuiManager guiManager, GuiMenu mainMenu) {
        this.guiManager = guiManager;
        this.mainMenu   = mainMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }
        guiManager.open(player, mainMenu);
        return true;
    }
}
