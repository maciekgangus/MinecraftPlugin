package dev.casino.games.derby;

import dev.casino.core.PluginContext;
import dev.casino.economy.GoldEconomyManager;
import dev.casino.games.DefaultGameRegistry;
import dev.casino.games.MainCasinoMenu;
import dev.casino.gui.DefaultGuiManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;

class HorseRacingGameTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private ServerMock server;
    private JavaPlugin plugin;
    private GoldEconomyManager economy;
    private DefaultGuiManager guiManager;
    private MainCasinoMenu mainMenu;
    private HorseRacingGame game;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server    = MockBukkit.mock();
        plugin    = MockBukkit.createMockPlugin();
        economy   = new GoldEconomyManager();
        guiManager = new DefaultGuiManager();
        var games  = new DefaultGameRegistry();
        mainMenu   = new MainCasinoMenu(games);
        var context = new PluginContext(plugin, economy, games, guiManager, mainMenu);
        game       = new HorseRacingGame(context);
        player     = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getNameReturnsRegistryKey() {
        assertEquals("horse_racing", game.getName());
    }

    @Test
    void getDisplayNameIsVirtualDerby() {
        assertEquals("Virtual Derby", PLAIN.serialize(game.getDisplayName()));
    }

    @Test
    void getIconUsesSaddle() {
        ItemStack icon = game.getIcon();
        assertEquals(Material.SADDLE, icon.getType());
    }

    @Test
    void startOpensBettingLayoutWithHorsesAndBackButton() {
        game.start(player);

        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(27, top.getSize());
        assertEquals(Material.DIAMOND_HORSE_ARMOR, top.getItem(11).getType());
        assertEquals(Material.GOLDEN_HORSE_ARMOR, top.getItem(13).getType());
        assertEquals(Material.IRON_HORSE_ARMOR, top.getItem(15).getType());
        assertEquals(Material.ARROW, top.getItem(22).getType());
    }

    @Test
    void betWithoutGoldDoesNotChangeBalance() {
        game.start(player);
        assertEquals(0, economy.getBalance(player));

        clickSlot(11);

        assertEquals(0, economy.getBalance(player));
    }

    @Test
    void clickBackOpensMainCasinoMenu() {
        game.start(player);

        clickSlot(22);

        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(54, top.getSize());
        assertEquals("Casino", PLAIN.serialize(player.getOpenInventory().title()));
    }

    @Test
    void betWithGoldStartsRaceView() {
        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 1));
        game.start(player);

        clickSlot(13);

        Inventory top = player.getOpenInventory().getTopInventory();
        assertEquals(Material.GOLDEN_HORSE_ARMOR, top.getItem(9).getType());
        assertEquals(0, economy.getBalance(player));
    }

    @Test
    void stopDoesNotThrow() {
        assertDoesNotThrow(() -> game.stop(player));
    }

    private void clickSlot(int slot) {
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL);
        guiManager.handleClick(event);
    }
}
