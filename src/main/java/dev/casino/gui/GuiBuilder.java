package dev.casino.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for {@link GuiMenu} instances.
 *
 * <p>Example usage:
 * <pre>{@code
 * GuiMenu menu = new GuiBuilder()
 *     .title(Component.text("Casino"))
 *     .size(6)
 *     .item(13, new GuiItem(icon, e -> openGame(player)))
 *     .build();
 * }</pre>
 */
public final class GuiBuilder {

    private Component title = Component.text("Casino");
    private int size = 54;
    private final Map<Integer, GuiItem> items = new HashMap<>();

    public GuiBuilder title(Component title) {
        this.title = title;
        return this;
    }

    /** @param rows number of inventory rows (1–6); converted to slot count internally. */
    public GuiBuilder size(int rows) {
        this.size = rows * 9;
        return this;
    }

    public GuiBuilder item(int slot, GuiItem item) {
        items.put(slot, item);
        return this;
    }

    public GuiMenu build() {
        final Component t  = title;
        final int       s  = size;
        final Map<Integer, GuiItem> snapshot = Map.copyOf(items);

        return new GuiMenu() {
            @Override
            public Inventory buildInventory(Player player) {
                Inventory inv = Bukkit.createInventory(null, s, t);
                snapshot.forEach((slot, guiItem) -> inv.setItem(slot, guiItem.getIcon()));
                return inv;
            }

            @Override
            public void handleClick(InventoryClickEvent event) {
                GuiItem item = snapshot.get(event.getSlot());
                if (item != null) item.handleClick(event);
            }

            @Override public Component getTitle() { return t; }
            @Override public int getSize()        { return s; }
        };
    }
}
