package dev.casino.games.slots;

import dev.casino.core.PluginContext;
import dev.casino.economy.BettingData;
import dev.casino.games.CasinoGame;
import dev.casino.gui.GuiBuilder;
import dev.casino.gui.GuiItem;
import dev.casino.gui.GuiMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SlotsGame implements CasinoGame {

    private final PluginContext context;
    private final String triggerFileName = "spin.txt";
    private final String resultFileName = "result.txt";
    private final Material[] symbols = {Material.DIAMOND, Material.GOLD_INGOT, Material.IRON_INGOT, Material.DIRT};

    // Ekonomia (kwoty w sztabkach - ingots)
    private final int costPerSpin = 1;
    private final int reward = 5;

    private boolean globalSpinLock = false;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public SlotsGame(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start(Player player) {
        GuiMenu menu = new GuiBuilder()
                .title(Component.text("Slot Machine - " + costPerSpin + " Ingot"))
                .size(3) // 3 rzędy (27 slotów)
                .item(11, new GuiItem(new ItemStack(Material.PAPER), null)) // Bęben 1
                .item(13, new GuiItem(new ItemStack(Material.PAPER), null)) // Bęben 2
                .item(15, new GuiItem(new ItemStack(Material.PAPER), null)) // Bęben 3
                .item(22, new GuiItem(new ItemStack(Material.LEVER), event -> handleSpinClick(player))) // Dźwignia
                .item(26, new GuiItem(new ItemStack(Material.BARRIER), event -> context.openMainCasinoMenu(player))) // Powrót
                .build();

        context.gui().open(player, menu);
    }

    @Override
    public void stop(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            globalSpinLock = false;
        }
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Material.NOTE_BLOCK);
    }

    @Override
    public String getName() {
        return "slots";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Slot Machine").color(NamedTextColor.GOLD);
    }

    private void handleSpinClick(Player player) {
        if (globalSpinLock) {
            player.sendMessage(Component.text("The machine is currently processing another spin!").color(NamedTextColor.RED));
            return;
        }

        if (!context.economy().hasFunds(player, costPerSpin)) {
            player.sendMessage(Component.text("You need at least " + costPerSpin + " gold ingot to play!").color(NamedTextColor.RED));
            return;
        }

        Inventory topInv = player.getOpenInventory().getTopInventory();
        if (topInv.getSize() != 27) return;

        // Pobranie opłaty
        context.economy().withdraw(player, costPerSpin);
        context.betting().getSlotsStats(player).addBet(costPerSpin);
        context.statsManager().saveStatsAsync();
        globalSpinLock = true;

        File triggerFile = new File(context.plugin().getDataFolder().getParentFile().getParentFile(), triggerFileName);
        try {
            triggerFile.createNewFile();
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

            BukkitTask task = new BukkitRunnable() {
                int tick = 0;
                final Random random = new Random();

                @Override
                public void run() {
                    tick++;
                    topInv.setItem(11, new ItemStack(symbols[random.nextInt(symbols.length)]));
                    topInv.setItem(13, new ItemStack(symbols[random.nextInt(symbols.length)]));
                    topInv.setItem(15, new ItemStack(symbols[random.nextInt(symbols.length)]));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

                    if (tick % 10 == 0) {
                        File resultFile = new File(context.plugin().getDataFolder().getParentFile().getParentFile(), resultFileName);
                        if (resultFile.exists()) {
                            stopAnimationAndShowResult(player, resultFile, topInv);
                            this.cancel();
                        }
                    }

                    if (tick > 200) {
                        globalSpinLock = false;
                        activeTasks.remove(player.getUniqueId());
                        this.cancel();
                    }
                }
            }.runTaskTimer(context.plugin(), 0L, 3L);

            activeTasks.put(player.getUniqueId(), task);

        } catch (IOException e) {
            e.printStackTrace();
            globalSpinLock = false;
        }
    }

    private void stopAnimationAndShowResult(Player player, File resultFile, Inventory topInv) {
        activeTasks.remove(player.getUniqueId());
        try {
            String content = Files.readString(resultFile.toPath()).trim();
            String[] parts = content.split(",");

            for (int i = 0; i < 3; i++) {
                final int index = i;
                Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
                    Material resultMat = Material.valueOf(parts[index]);
                    int slot = (index == 0) ? 11 : (index == 1) ? 13 : 15;
                    topInv.setItem(slot, new ItemStack(resultMat));
                    player.playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1f, 1.2f);

                    if (index == 2) {
                        finalizeGame(player, parts[3]);
                    }
                }, i * 10L);
            }

            resultFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
            globalSpinLock = false;
        }
    }

    private void finalizeGame(Player player, String outcome) {
        globalSpinLock = false;
        if (outcome.equals("WIN")) {
            player.sendTitle("§6★ WIN ★", "§eJackpot!", 5, 40, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

            // Wydanie nagrody
            context.economy().deposit(player, reward);
            context.betting().getSlotsStats(player).addWon(reward);
            context.statsManager().saveStatsAsync();
            player.sendMessage(Component.text("You won " + reward + " gold ingots!").color(NamedTextColor.GREEN));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 0.5f);
        }
    }
}