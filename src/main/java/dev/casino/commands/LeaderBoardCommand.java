package dev.casino.commands;

import dev.casino.core.PluginContext;
import dev.casino.economy.BettingData;
import dev.casino.economy.CasinoBettingData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public final class LeaderBoardCommand implements CommandExecutor {

    private final PluginContext context;

    public LeaderBoardCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return true;
        }

        CasinoBettingData bettingRegistry = context.betting();
        
        player.sendMessage(Component.text("Casino Leaderboard", NamedTextColor.GOLD, TextDecoration.BOLD));

        Set<UUID> allPlayersSet = new HashSet<>();
        allPlayersSet.addAll(bettingRegistry.getBlackjackStatsMap().keySet());
        allPlayersSet.addAll(bettingRegistry.getHorseRacingStatsMap().keySet());
        allPlayersSet.addAll(bettingRegistry.getSlotsStatsMap().keySet());

        if (allPlayersSet.isEmpty()) {
            player.sendMessage(Component.text("No betting data recorded yet.", NamedTextColor.GRAY));
            return true;
        }

        List<UUID> sortedAllPlayers = new ArrayList<>(allPlayersSet);
        sortedAllPlayers.sort((u1, u2) -> Integer.compare(bettingRegistry.getTotalBalance(u2), bettingRegistry.getTotalBalance(u1)));

        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("ALL GAMES", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (UUID uuid : sortedAllPlayers) {
            int totalWon = 0;
            int totalBet = 0;

            BettingData bj = bettingRegistry.getBlackjackStatsMap().get(uuid);
            BettingData hr = bettingRegistry.getHorseRacingStatsMap().get(uuid);
            BettingData sl = bettingRegistry.getSlotsStatsMap().get(uuid);

            if (bj != null) {
                totalWon += bj.getGoldWon();
                totalBet += bj.getGoldBet();
            }
            if (hr != null) {
                totalWon += hr.getGoldWon();
                totalBet += hr.getGoldBet();
            }
            if (sl != null) {
                totalWon += sl.getGoldWon();
                totalBet += sl.getGoldBet();
            }

            player.sendMessage(formatEntryLine(getName(uuid), totalWon, totalBet));
        }

        Map<UUID, BettingData> bjMap = bettingRegistry.getBlackjackStatsMap();
        displayGameResults(bjMap, "BLACKJACK", player);

        Map<UUID, BettingData> hrMap = bettingRegistry.getHorseRacingStatsMap();
        displayGameResults(hrMap, "HORSE RACING", player);

        Map<UUID, BettingData> slMap = bettingRegistry.getSlotsStatsMap();
        displayGameResults(slMap, "SLOTS", player);

        return true;
    }

    private String getName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return (name != null) ? name : "Unknown (" + uuid.toString().substring(0, 8) + ")";
    }

    private Component formatEntryLine(String name, int won, int bet) {
        int balance = won - bet;
        return Component.text()
            .append(Component.text("  " + name + ": ", NamedTextColor.WHITE))
            .append(Component.text("profit " + balance + " ", balance >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED))
            .append(Component.text("won " + won + " ", NamedTextColor.GREEN))
            .append(Component.text("bet " + bet, NamedTextColor.RED))
            .build();
    }

    private void displayGameResults(Map<UUID, BettingData> gameMap, String name, Player player) {
        if (!gameMap.isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD));

            List<Map.Entry<UUID, BettingData>> sorted = new ArrayList<>(gameMap.entrySet());
            sorted.sort((e1, e2) -> Integer.compare(e2.getValue().getBalance(), e1.getValue().getBalance()));

            for (Map.Entry<UUID, BettingData> entry : sorted) {
                BettingData data = entry.getValue();
                player.sendMessage(formatEntryLine(getName(entry.getKey()), data.getGoldWon(), data.getGoldBet()));
            }
        }
    }
}
