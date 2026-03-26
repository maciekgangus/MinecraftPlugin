package dev.casino.games;

import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The root casino menu — displays all registered games as clickable icons.
 *
 * <p>Clicking a game icon calls {@link CasinoGame#start(Player)} for that game.
 * The slot index directly maps to the registration order in {@link GameRegistry}.
 */
public final class MainCasinoMenu implements GuiMenu {

    private static final int SIZE = 54;
    private static final Component TITLE = Component.text("Casino");

    private final GameRegistry registry;

    public MainCasinoMenu(GameRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Inventory buildInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        int slot = 0;
        for (CasinoGame game : registry.getAll()) {
            if (slot >= SIZE) break;
            ItemStack icon = game.getIcon().clone();
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(game.getDisplayName());
                icon.setItemMeta(meta);
            }
            inv.setItem(slot++, icon);
        }
        return inv;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        List<CasinoGame> games = new ArrayList<>(registry.getAll());
        if (slot < 0 || slot >= games.size()) return;
        Player player = (Player) event.getWhoClicked();
        games.get(slot).start(player);
    }

    @Override public Component getTitle() { return TITLE; }
    @Override public int getSize()        { return SIZE; }
}
