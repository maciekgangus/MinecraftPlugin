package dev.casino.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/** An inventory slot entry: an icon paired with an optional click handler. */
public final class GuiItem {

    private final ItemStack icon;
    private final Consumer<InventoryClickEvent> onClick;

    public GuiItem(ItemStack icon, Consumer<InventoryClickEvent> onClick) {
        this.icon    = icon;
        this.onClick = onClick;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void handleClick(InventoryClickEvent event) {
        if (onClick != null) onClick.accept(event);
    }
}
