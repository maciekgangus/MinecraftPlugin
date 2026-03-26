package dev.casino.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe {@link GuiManager} backed by a {@link ConcurrentHashMap}. */
public final class DefaultGuiManager implements GuiManager {

    private final ConcurrentHashMap<UUID, GuiMenu> active = new ConcurrentHashMap<>();

    @Override
    public void open(Player player, GuiMenu menu) {
        active.put(player.getUniqueId(), menu);
        player.openInventory(menu.buildInventory(player));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        GuiMenu menu = active.get(player.getUniqueId());
        if (menu == null) return;
        event.setCancelled(true);
        menu.handleClick(event);
    }

    @Override
    public void cleanup(UUID playerId) {
        active.remove(playerId);
    }

    @Override
    public Set<UUID> getActivePlayers() {
        return Set.copyOf(active.keySet());
    }
}
