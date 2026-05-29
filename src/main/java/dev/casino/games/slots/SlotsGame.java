package dev.casino.games.slots;

import dev.casino.core.PluginContext;
import dev.casino.games.CasinoGame;
import dev.casino.gui.GuiBuilder;
import dev.casino.gui.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlotsGame implements CasinoGame {

    private static final int MAX_BET = 64;
    private static final int MIN_BET = 1;

    private final PluginContext context;
    private final Map<UUID, SlotSession> sessions = new ConcurrentHashMap<>();
    private final Random rng = new Random();

    public SlotsGame(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start(Player player) {
        stop(player);
        SlotSession session = new SlotSession();
        sessions.put(player.getUniqueId(), session);
        openSlotGui(player, session);
    }

    @Override
    public void stop(Player player) {
        SlotSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        if (session.animationTask != null) {
            session.animationTask.cancel();
            if (session.spinning) {
                context.economy().deposit(player, session.bet);
            }
        }
    }

    @Override
    public ItemStack getIcon() {
        return createItem(Material.NOTE_BLOCK, Component.text("Slot Machine", NamedTextColor.GOLD));
    }

    @Override
    public String getName() { return "slots"; }

    @Override
    public Component getDisplayName() {
        return Component.text("Slot Machine", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    void openSlotGui(Player player, SlotSession session) {
        GuiBuilder builder = new GuiBuilder()
                .title(Component.text("Slot Machine", NamedTextColor.DARK_GRAY))
                .size(3)
                .item(SlotsLayout.REEL_1, new GuiItem(reelItem(null), null))
                .item(SlotsLayout.REEL_2, new GuiItem(reelItem(null), null))
                .item(SlotsLayout.REEL_3, new GuiItem(reelItem(null), null))
                .item(SlotsLayout.BET_DECREASE, new GuiItem(
                        createItem(Material.RED_WOOL,
                                Component.text("Bet -1  (Current: " + session.bet + ")", NamedTextColor.RED)),
                        e -> {
                            if (!session.spinning && session.bet > MIN_BET) {
                                session.bet--;
                                openSlotGui(player, session);
                            }
                        }))
                .item(SlotsLayout.BET_DISPLAY, new GuiItem(
                        createItem(Material.GOLD_INGOT,
                                Component.text("Bet: " + session.bet + " Gold", NamedTextColor.GOLD)), null))
                .item(SlotsLayout.BET_INCREASE, new GuiItem(
                        createItem(Material.LIME_WOOL,
                                Component.text("Bet +1  (Current: " + session.bet + ")", NamedTextColor.GREEN)),
                        e -> {
                            if (!session.spinning && session.bet < MAX_BET
                                    && context.economy().getBalance(player) > session.bet) {
                                session.bet++;
                                openSlotGui(player, session);
                            }
                        }))
                .item(SlotsLayout.SPIN, new GuiItem(
                        createItem(Material.LEVER,
                                session.spinning
                                        ? Component.text("SPINNING...", NamedTextColor.GRAY, TextDecoration.BOLD)
                                        : Component.text("SPIN!", NamedTextColor.YELLOW, TextDecoration.BOLD)),
                        e -> handleSpin(player, session)))
                .item(SlotsLayout.BACK, new GuiItem(
                        createItem(Material.ARROW, Component.text("BACK", NamedTextColor.GRAY)),
                        e -> {
                            stop(player);
                            context.openMainCasinoMenu(player);
                        }));

        context.gui().open(player, builder.build());
        session.openInventory = player.getOpenInventory().getTopInventory();
    }

    private void handleSpin(Player player, SlotSession session) {
        if (session.spinning) return;
        if (!context.economy().hasFunds(player, session.bet)) {
            player.sendMessage(Component.text("Not enough gold to spin!", NamedTextColor.RED));
            return;
        }
        // animation implemented in Task 3
    }

    private ItemStack reelItem(SlotSymbol symbol) {
        if (symbol == null) {
            return createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        }
        return createItem(symbol.getMaterial(), Component.text(symbol.name(), NamedTextColor.WHITE));
    }

    private ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
