package dev.casino.games;

import dev.casino.core.PluginContext;
import dev.casino.economy.CasinoBettingData;
import dev.casino.economy.GoldEconomyManager;
import dev.casino.economy.StatsManager;
import dev.casino.games.blackjack.BlackjackLayout;
import dev.casino.games.slots.SlotsLayout;
import dev.casino.gui.DefaultGuiManager;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AllGamesIntegrationTest {

    private static final List<String> EXPECTED_GAME_NAMES = List.of(
            "horse_racing", "blackjack", "slots"
    );

    private ServerMock server;
    private PluginContext context;
    private DefaultGameRegistry registry;
    private MainCasinoMenu mainMenu;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        JavaPlugin plugin = MockBukkit.createMockPlugin();
        var economy = new GoldEconomyManager();
        var betting = new CasinoBettingData();
        registry = new DefaultGameRegistry();
        var guiManager = new DefaultGuiManager();
        var statsManager = new StatsManager(plugin, betting);
        mainMenu = new MainCasinoMenu(registry);
        context = new PluginContext(
                plugin, economy, betting, statsManager, registry, guiManager, mainMenu);
        GameRegistrar.register(context);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void gameRegistrarRegistersAllGames() {
        assertEquals(EXPECTED_GAME_NAMES.size(), registry.getAll().size());
        assertEquals(EXPECTED_GAME_NAMES,
                registry.getAll().stream().map(CasinoGame::getName).toList());
    }

    @Test
    void everyRegisteredGameIsFindableByName() {
        for (String name : EXPECTED_GAME_NAMES) {
            assertTrue(registry.findByName(name).isPresent(), "Missing game: " + name);
        }
    }

    @Test
    void mainMenuShowsAllGameIconsInRegistrationOrder() {
        Inventory menu = mainMenu.buildInventory(player);

        assertEquals(Material.SADDLE, menu.getItem(0).getType());
        assertEquals(Material.WHITE_BANNER, menu.getItem(1).getType());
        assertEquals(Material.NOTE_BLOCK, menu.getItem(2).getType());
    }

    @Test
    void openMainCasinoMenuViaContextShowsAllGames() {
        context.openMainCasinoMenu(player);

        Inventory menu = player.getOpenInventory().getTopInventory();
        assertEquals(54, menu.getSize());
        assertEquals(Material.SADDLE, menu.getItem(0).getType());
        assertEquals(Material.WHITE_BANNER, menu.getItem(1).getType());
        assertEquals(Material.NOTE_BLOCK, menu.getItem(2).getType());
    }

    @Test
    void clickingHorseRacingOpensDerbyGui() {
        clickMainMenuSlot(0);

        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(27, top.getSize());
        assertEquals(Material.DIAMOND_HORSE_ARMOR, top.getItem(11).getType());
    }

    @Test
    void clickingBlackjackOpensBettingGui() {
        clickMainMenuSlot(1);

        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(54, top.getSize());
        assertNotNull(top.getItem(BlackjackLayout.BET_DISPLAY));
    }

    @Test
    void clickingSlotsOpensSlotMachineGui() {
        clickMainMenuSlot(2);

        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(27, top.getSize());
        assertNotNull(top.getItem(SlotsLayout.SPIN));
    }

    private void clickMainMenuSlot(int slot) {
        Inventory menu = mainMenu.buildInventory(player);
        player.openInventory(menu);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL);
        mainMenu.handleClick(event);
    }
}
