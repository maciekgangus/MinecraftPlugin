package dev.casino.economy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class StatsManager {

    private final JavaPlugin plugin;
    private final CasinoBettingData bettingData;
    private final File statsFile;
    private FileConfiguration statsConfig;

    public StatsManager(JavaPlugin plugin, CasinoBettingData bettingData) {
        this.plugin = plugin;
        this.bettingData = bettingData;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    }

    public void loadStats() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!statsFile.exists()) {
            return;
        }

        statsConfig = YamlConfiguration.loadConfiguration(statsFile);

        loadMap("blackjack", bettingData.getBlackjackStatsMap());
        loadMap("horse-racing", bettingData.getHorseRacingStatsMap());
        loadMap("slots", bettingData.getSlotsStatsMap());
    }

    private void loadMap(String section, Map<UUID, BettingData> map) {
        var sectionConfig = statsConfig.getConfigurationSection(section);
        if (sectionConfig == null) return;

        for (String key : sectionConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int bet = sectionConfig.getInt(key + ".bet");
                int won = sectionConfig.getInt(key + ".won");
                map.put(uuid, new BettingData(bet, won));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to load stat for UUID: " + key);
            }
        }
    }

    public void saveStatsAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveStats);
    }

    public synchronized void saveStats() {
        FileConfiguration config = new YamlConfiguration();

        saveMapToConfig(config, "blackjack", bettingData.getBlackjackStatsMap());
        saveMapToConfig(config, "horse-racing", bettingData.getHorseRacingStatsMap());
        saveMapToConfig(config, "slots", bettingData.getSlotsStatsMap());

        try {
            if (!statsFile.getParentFile().exists()) {
                statsFile.getParentFile().mkdirs();
            }
            config.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats to " + statsFile, e);
        }
    }

    private void saveMapToConfig(FileConfiguration config, String section, Map<UUID, BettingData> map) {
        for (Map.Entry<UUID, BettingData> entry : map.entrySet()) {
            String path = section + "." + entry.getKey().toString();
            config.set(path + ".bet", entry.getValue().getGoldBet());
            config.set(path + ".won", entry.getValue().getGoldWon());
        }
    }
}
