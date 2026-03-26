package dev.casino.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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

class GuiBuilderTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.createMockPlugin();
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void buildReturnsMenuWithConfiguredSize() {
        GuiMenu menu = new GuiBuilder().size(3).title(Component.text("Test")).build();
        assertEquals(27, menu.getSize());
    }

    @Test
    void buildReturnsMenuWithConfiguredTitle() {
        Component title = Component.text("Casino");
        GuiMenu menu = new GuiBuilder().size(1).title(title).build();
        assertEquals(title, menu.getTitle());
    }

    @Test
    void buildInventoryHasItemAtConfiguredSlot() {
        ItemStack icon = new ItemStack(Material.GOLD_INGOT);
        GuiMenu menu = new GuiBuilder()
                .size(1)
                .title(Component.text("T"))
                .item(4, new GuiItem(icon, null))
                .build();

        Inventory inv = menu.buildInventory(player);

        assertNotNull(inv.getItem(4));
        assertEquals(Material.GOLD_INGOT, inv.getItem(4).getType());
    }

    @Test
    void buildInventorySlotsWithoutItemsAreEmpty() {
        GuiMenu menu = new GuiBuilder().size(1).title(Component.text("T")).build();
        Inventory inv = menu.buildInventory(player);
        assertNull(inv.getItem(0));
    }

    @Test
    void handleClickRoutesToCorrectSlotCallback() {
        AtomicBoolean fired = new AtomicBoolean(false);
        GuiMenu menu = new GuiBuilder()
                .size(1)
                .title(Component.text("T"))
                .item(2, new GuiItem(new ItemStack(Material.STONE), e -> fired.set(true)))
                .build();

        Inventory inv = menu.buildInventory(player);
        player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                2, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        menu.handleClick(event);

        assertTrue(fired.get());
    }

    @Test
    void handleClickOnEmptySlotDoesNothing() {
        GuiMenu menu = new GuiBuilder().size(1).title(Component.text("T")).build();
        Inventory inv = menu.buildInventory(player);
        player.openInventory(inv);
        InventoryClickEvent event = new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                0, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertDoesNotThrow(() -> menu.handleClick(event));
    }

    @Test
    void multipleCallsToBuildInventoryAreIndependent() {
        GuiMenu menu = new GuiBuilder().size(1).title(Component.text("T")).build();
        Inventory inv1 = menu.buildInventory(player);
        Inventory inv2 = menu.buildInventory(player);
        assertNotSame(inv1, inv2);
    }
}
