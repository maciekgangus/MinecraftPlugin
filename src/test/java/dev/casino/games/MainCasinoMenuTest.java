package dev.casino.games;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MainCasinoMenuTest {

    private ServerMock server;
    private DefaultGameRegistry registry;
    private MainCasinoMenu menu;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server   = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        registry = new DefaultGameRegistry();
        menu     = new MainCasinoMenu(registry);
        player   = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    // --- buildInventory ---

    @Test
    void emptyRegistryProducesInventoryWithNoItems() {
        Inventory inv = menu.buildInventory(player);
        for (int i = 0; i < inv.getSize(); i++) {
            assertNull(inv.getItem(i), "Slot " + i + " should be empty");
        }
    }

    @Test
    void gameIconAppearsAtCorrectSlot() {
        registry.register(stub("slots", Material.GOLD_INGOT));
        Inventory inv = menu.buildInventory(player);
        assertNotNull(inv.getItem(0));
        assertEquals(Material.GOLD_INGOT, inv.getItem(0).getType());
    }

    @Test
    void multipleGamesArePlacedInOrder() {
        registry.register(stub("slots",     Material.GOLD_INGOT));
        registry.register(stub("blackjack", Material.DIAMOND));
        Inventory inv = menu.buildInventory(player);
        assertEquals(Material.GOLD_INGOT, inv.getItem(0).getType());
        assertEquals(Material.DIAMOND,    inv.getItem(1).getType());
    }

    @Test
    void inventorySizeIs54() {
        assertEquals(54, menu.buildInventory(player).getSize());
    }

    // --- handleClick ---

    @Test
    void clickOnGameSlotStartsGame() {
        AtomicBoolean started = new AtomicBoolean(false);
        registry.register(stubWithStart("slots", Material.GOLD_INGOT, p -> started.set(true)));

        Inventory inv = menu.buildInventory(player);
        player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        menu.handleClick(event);

        assertTrue(started.get());
    }

    @Test
    void clickBeyondRegisteredGamesDoesNotThrow() {
        registry.register(stub("slots", Material.GOLD_INGOT));

        Inventory inv = menu.buildInventory(player);
        player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                10, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertDoesNotThrow(() -> menu.handleClick(event));
    }

    // --- getTitle / getSize ---

    @Test
    void getTitleReturnsNonNull() {
        assertNotNull(menu.getTitle());
    }

    @Test
    void getSizeReturns54() {
        assertEquals(54, menu.getSize());
    }

    // --- helpers ---

    private CasinoGame stub(String name, Material icon) {
        return stubWithStart(name, icon, p -> {});
    }

    private CasinoGame stubWithStart(String name, Material icon, java.util.function.Consumer<Player> onStart) {
        return new CasinoGame() {
            @Override public void start(Player p)       { onStart.accept(p); }
            @Override public void stop(Player p)        {}
            @Override public ItemStack getIcon()        { return new ItemStack(icon); }
            @Override public String getName()           { return name; }
            @Override public Component getDisplayName() { return Component.text(name); }
        };
    }
}
