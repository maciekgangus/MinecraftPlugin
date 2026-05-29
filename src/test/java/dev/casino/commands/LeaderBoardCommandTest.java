package dev.casino.commands;

import dev.casino.core.PluginContext;
import dev.casino.economy.CasinoBettingData;
import dev.casino.economy.GoldEconomyManager;
import dev.casino.economy.StatsManager;
import dev.casino.games.DefaultGameRegistry;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class LeaderBoardCommandTest {

    private ServerMock server;
    private CasinoBettingData betting;
    private LeaderBoardCommand command;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        var plugin = MockBukkit.createMockPlugin();
        var economy = new GoldEconomyManager();
        betting = new CasinoBettingData();
        var statsManager = new StatsManager(plugin, betting);
        var context = new PluginContext(plugin, economy, betting, statsManager, new DefaultGameRegistry(), null, null);
        command = new LeaderBoardCommand(context);
        player = server.addPlayer("Player1");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void nonPlayerSenderReceivesError() {
        var console = server.getConsoleSender();
        command.onCommand(console, null, "leaderboard", new String[0]);
        
        String message = PlainTextComponentSerializer.plainText().serialize(console.nextComponentMessage());
        assertEquals("Only players can use this command.", message);
    }

    @Test
    void noDataMessageDisplayed() {
        command.onCommand(player, null, "leaderboard", new String[0]);

        player.nextComponentMessage(); 
        
        String message = PlainTextComponentSerializer.plainText().serialize(player.nextComponentMessage());
        assertEquals("No betting data recorded yet.", message);
    }

    @Test
    void displaysStatsAndSorting() {
        PlayerMock player2 = server.addPlayer("Player2");
        
        // Player1: BlackJack Bet 10, Won 20 (Balance +10)
        var bj1 = betting.getBlackjackStats(player);
        bj1.addBet(10);
        bj1.addWon(20);
        
        // Player2: BlackJack Bet 10, Won 30 (Balance +20)
        var bj2 = betting.getBlackjackStats(player2);
        bj2.addBet(10);
        bj2.addWon(30);

        command.onCommand(player, null, "leaderboard", new String[0]);

        // Verify the correct order
        boolean foundP2 = false;
        boolean foundP1 = false;
        
        while (true) {
            var component = player.nextComponentMessage();
            if (component == null) break;
            String text = PlainTextComponentSerializer.plainText().serialize(component);
            
            if (text.contains("Player2") && !foundP2) {
                foundP2 = true;
                assertFalse(foundP1, "Player2 should appear before Player1");
            }
            if (text.contains("Player1")) {
                foundP1 = true;
            }
        }
        
        assertTrue(foundP1 && foundP2);
    }

    @Test
    void slotsDataIncludedInCombinedRanking() {
        // This test will currently fail because Slots is not in the initial set addition
        betting.getSlotsStats(player).addBet(5);
        betting.getSlotsStats(player).addWon(10);

        command.onCommand(player, null, "leaderboard", new String[0]);
        
        boolean foundAllGames = false;
        boolean foundPlayer = false;

        while (true) {
            var component = player.nextComponentMessage();
            if (component == null) break;
            String text = PlainTextComponentSerializer.plainText().serialize(component);
            if (text.equals("ALL GAMES")) foundAllGames = true;
            if (foundAllGames && text.contains("Player1") && text.contains("profit 5")) {
                foundPlayer = true;
            }
        }

        assertTrue(foundPlayer, "Player with only Slots data should be in the combined ranking");
    }
}
