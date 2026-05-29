package dev.casino.economy;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StatsManagerTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private CasinoBettingData bettingData;
    private StatsManager statsManager;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();
        bettingData = new CasinoBettingData();
        statsManager = new StatsManager(plugin, bettingData);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void saveAndLoadStatsWorks() {
        PlayerMock player = server.addPlayer();
        UUID uuid = player.getUniqueId();

        BettingData bjData = bettingData.getBlackjackStats(player);
        bjData.addBet(10);
        bjData.addWon(20);

        statsManager.saveStats();

        // Clear in-memory data
        CasinoBettingData newBettingData = new CasinoBettingData();
        StatsManager newStatsManager = new StatsManager(plugin, newBettingData);
        
        newStatsManager.loadStats();

        BettingData loadedBjData = newBettingData.getBlackjackStatsMap().get(uuid);
        assertNotNull(loadedBjData);
        assertEquals(10, loadedBjData.getGoldBet());
        assertEquals(20, loadedBjData.getGoldWon());
    }

    @Test
    void loadNonExistentStatsDoesNotCrash() {
        File statsFile = new File(plugin.getDataFolder(), "stats.yml");
        if (statsFile.exists()) {
            statsFile.delete();
        }

        assertDoesNotThrow(() -> statsManager.loadStats());
    }
}
