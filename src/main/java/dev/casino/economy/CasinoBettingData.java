package dev.casino.economy;

import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class CasinoBettingData {
    private final Map<UUID, BettingData> blackjackStats = new ConcurrentHashMap<>();
    private final Map<UUID, BettingData> horseRacingStats = new ConcurrentHashMap<>();
    private final Map<UUID, BettingData> slotsStats = new ConcurrentHashMap<>();

    public BettingData getBlackjackStats(Player player) {
        return blackjackStats.computeIfAbsent(player.getUniqueId(), k -> new BettingData());
    }

    public BettingData getHorseRacingStats(Player player) {
        return horseRacingStats.computeIfAbsent(player.getUniqueId(), k -> new BettingData());
    }

    public BettingData getSlotsStats(Player player) {
        return slotsStats.computeIfAbsent(player.getUniqueId(), k -> new BettingData());
    }


    public Map<UUID, BettingData> getBlackjackStatsMap() {
        return blackjackStats;
    }


    public Map<UUID, BettingData> getHorseRacingStatsMap() {
        return horseRacingStats;
    }


    public Map<UUID, BettingData> getSlotsStatsMap() {
        return slotsStats;
    }


    public int getTotalBalance(UUID uuid) {
        int total = 0;
        BettingData bj = blackjackStats.get(uuid);
        if (bj != null) total += bj.getBalance();
        BettingData hr = horseRacingStats.get(uuid);
        if (hr != null) total += hr.getBalance();
        BettingData sl = slotsStats.get(uuid);
        if (sl != null) total += sl.getBalance();
        return total;
    }
}
