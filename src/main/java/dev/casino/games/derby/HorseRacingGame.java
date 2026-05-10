package dev.casino.games.derby;

import dev.casino.core.PluginContext;
import dev.casino.economy.EconomyManager;
import dev.casino.games.CasinoGame;
import dev.casino.gui.GuiBuilder;
import dev.casino.gui.GuiItem;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Random;

public final class HorseRacingGame implements CasinoGame {

    private static final BetOption DIAMOND_FLASH = new BetOption(
        1,
        "Diamond Flash",
        Material.DIAMOND_HORSE_ARMOR,
        3,
        7,
        0.75
    );
    private static final BetOption GOLDEN_HORSESHOE = new BetOption(
        2,
        "Golden Horseshoe",
        Material.GOLDEN_HORSE_ARMOR,
        2,
        6,
        0.55
    );
    private static final BetOption IRON_HOOF = new BetOption(
        3,
        "Iron Hoof",
        Material.IRON_HORSE_ARMOR,
        1,
        5,
        0.35
    );

    private final PluginContext context;
    private final Random random = new Random();

    public HorseRacingGame(PluginContext context) {
        this.context = context;
    }

    private GuiMenu buildBettingMenu(Player player) {
        EconomyManager eco = context.economy();
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Back to all games"));
            back.setItemMeta(backMeta);
        }
        return new GuiBuilder()
            .title(Component.text("Pick a winner"))
            .size(3)
            .item(11, new GuiItem(createHorseBetIcon(DIAMOND_FLASH),
                e -> placeBet(player, eco, DIAMOND_FLASH)))
            .item(13, new GuiItem(createHorseBetIcon(GOLDEN_HORSESHOE),
                e -> placeBet(player, eco, GOLDEN_HORSESHOE)))
            .item(15, new GuiItem(createHorseBetIcon(IRON_HOOF),
                e -> placeBet(player, eco, IRON_HOOF)))
            .item(22, new GuiItem(back, e -> context.openMainCasinoMenu(player)))
            .build();
    }

    @Override
    public void start(Player player) {
        context.gui().open(player, buildBettingMenu(player));
    }

    private ItemStack createHorseBetIcon(BetOption option) {
        ItemStack icon = new ItemStack(option.icon());
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(option.name()));
            meta.lore(List.of(
                Component.text("Cost: " + option.cost() + " gold"),
                Component.text("Payout: " + option.payout() + " gold"),
                Component.text("Win chance: " + toPercent(option.moveChance()) + "%")
            ));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    private int toPercent(double chance) {
        return (int) Math.round(chance * 100);
    }

    private void placeBet(Player player, EconomyManager eco, BetOption selectedHorse) {
        if (!eco.hasFunds(player, selectedHorse.cost())) {
            player.sendMessage(Component.text("You don't have enough gold for this horse!"));
            return;
        }
        eco.withdraw(player, selectedHorse.cost());
        player.sendMessage(Component.text(
            "The race begins! You bet on " + selectedHorse.name() + "."
        ));
        Inventory inv = player.getOpenInventory().getTopInventory();
        inv.clear();

        ItemStack horse1 = new ItemStack(Material.DIAMOND_HORSE_ARMOR);
        ItemStack horse2 = new ItemStack(Material.GOLDEN_HORSE_ARMOR);
        ItemStack horse3 = new ItemStack(Material.IRON_HORSE_ARMOR);
        int[] positions = {0, 0, 0};

        inv.setItem(0, horse1);
        inv.setItem(9, horse2);
        inv.setItem(18, horse3);

        new BukkitRunnable() {
            @Override
            public void run() {
                inv.setItem(positions[0], null);
                inv.setItem(positions[1] + 9, null);
                inv.setItem(positions[2] + 18, null);

                if (random.nextDouble() < DIAMOND_FLASH.moveChance()) {
                    positions[0]++;
                }
                if (random.nextDouble() < GOLDEN_HORSESHOE.moveChance()) {
                    positions[1]++;
                }
                if (random.nextDouble() < IRON_HOOF.moveChance()) {
                    positions[2]++;
                }

                inv.setItem(positions[0], horse1);
                inv.setItem(positions[1] + 9, horse2);
                inv.setItem(positions[2] + 18, horse3);

                int winner = 0;
                if (positions[0] >= 8) {
                    winner = 1;
                } else if (positions[1] >= 8) {
                    winner = 2;
                } else if (positions[2] >= 8) {
                    winner = 3;
                }

                if (winner != 0) {
                    cancel();
                    if (winner == selectedHorse.number()) {
                        player.sendMessage(Component.text(
                            "Your horse won! You receive " + selectedHorse.payout() + " gold."
                        ));
                        eco.deposit(player, selectedHorse.payout());
                    } else {
                        player.sendMessage(Component.text("Horse #" + winner + " won. Better luck next time."));
                    }
                    context.gui().open(player, buildBettingMenu(player));
                }
            }
        }.runTaskTimer(context.plugin(), 10L, 10L);
    }

    @Override
    public void stop(Player player) {
    }

    @Override
    public ItemStack getIcon() {
        ItemStack icon = new ItemStack(Material.SADDLE);
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.displayName(getDisplayName());
            icon.setItemMeta(meta);
        }
        return icon;
    }

    @Override
    public String getName() {
        return "horse_racing";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Virtual Derby");
    }

    private record BetOption(
        int number,
        String name,
        Material icon,
        int cost,
        int payout,
        double moveChance
    ) {}
}
