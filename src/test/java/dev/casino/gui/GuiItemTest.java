package dev.casino.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GuiItemTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
        MockBukkit.createMockPlugin();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void getIconReturnsSuppliedItem() {
        ItemStack icon = new ItemStack(Material.GOLD_INGOT);
        GuiItem item = new GuiItem(icon, null);
        assertSame(icon, item.getIcon());
    }

    @Test
    void handleClickInvokesCallback() {
        AtomicBoolean fired = new AtomicBoolean(false);
        GuiItem item = new GuiItem(new ItemStack(Material.GOLD_INGOT), e -> fired.set(true));
        item.handleClick(null);
        assertTrue(fired.get());
    }

    @Test
    void handleClickWithNullCallbackDoesNothing() {
        GuiItem item = new GuiItem(new ItemStack(Material.GOLD_INGOT), null);
        assertDoesNotThrow(() -> item.handleClick(null));
    }
}
