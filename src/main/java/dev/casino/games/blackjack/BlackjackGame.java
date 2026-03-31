package dev.casino.games.blackjack;

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
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class BlackjackGame implements CasinoGame {

    private final PluginContext context;
    private final Map<UUID, BlackjackSession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final int INITIAL_BET = 1;
    private static final int DEALER_THRESHOLD = 17;
    private static final int MAX_HAND_VALUE = 21;

    private static final Map<Integer, Material> MATERIAL_MAP = Map.ofEntries(
            Map.entry(2, Material.COAL_BLOCK),
            Map.entry(3, Material.COPPER_BLOCK),
            Map.entry(4, Material.IRON_BLOCK),
            Map.entry(5, Material.LAPIS_BLOCK),
            Map.entry(6, Material.REDSTONE_BLOCK),
            Map.entry(7, Material.QUARTZ_BLOCK),
            Map.entry(8, Material.GOLD_BLOCK),
            Map.entry(9, Material.EMERALD_BLOCK),
            Map.entry(10, Material.DIAMOND_BLOCK),
            Map.entry(11, Material.NETHERITE_BLOCK)
    );

    public BlackjackGame(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start(Player player) {
        if (!context.economy().withdraw(player, INITIAL_BET)) {
            player.sendMessage(Component.text("You don't have enough gold (1 ingot required)!", NamedTextColor.RED));
            return;
        }

        BlackjackSession session = new BlackjackSession();
        // Initial deal
        session.playerHand.add(drawCard());
        session.playerHand.add(drawCard());
        session.dealerHand.add(drawCard());
        session.dealerHand.add(drawCard());

        sessions.put(player.getUniqueId(), session);

        // Check for Dealer Natural 21 (Push Rule)
        if (calculateTotal(session.dealerHand) == MAX_HAND_VALUE) {
            context.economy().deposit(player, INITIAL_BET);
            endGame(player, session, "Dealer has 21! It's a draw.");
        }

        openGameGui(player, session);
    }

    @Override
    public void stop(Player player) {
        sessions.remove(player.getUniqueId());
    }

    @Override
    public ItemStack getIcon() {
        return createItem(Material.WHITE_BANNER, Component.text("Blackjack", NamedTextColor.GOLD));
    }

    @Override
    public String getName() {
        return "blackjack";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Blackjack", NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    private int drawCard() {
        return random.nextInt(2, 12); // 2 to 11
    }

    private void openGameGui(Player player, BlackjackSession session) {
        GuiBuilder builder = new GuiBuilder()
                .title(Component.text("Blackjack Table", NamedTextColor.DARK_GRAY))
                .size(6);

        // Dealer's Hand (Row 1: Slots 0-8)
        int dealerRowStart = 0;
        if (session.isGameOver) {
            for (int i = 0; i < session.dealerHand.size(); i++) {
                int slot = getSymmetricalSlot(dealerRowStart, i, session.dealerHand.size());
                builder.item(slot, new GuiItem(createCardItem(session.dealerHand.get(i)), null));
            }
        } else {
            builder.item(3, new GuiItem(createCardItem(session.dealerHand.get(0)), null));
            builder.item(5, new GuiItem(createItem(Material.GRAY_STAINED_GLASS, Component.text("Hidden Card", NamedTextColor.GRAY)), null));
        }

        // Player's Hand (Row 3: Slots 18-26)
        int playerRowStart = 18;
        for (int i = 0; i < session.playerHand.size(); i++) {
            int slot = getSymmetricalSlot(playerRowStart, i, session.playerHand.size());
            builder.item(slot, new GuiItem(createCardItem(session.playerHand.get(i)), null));
        }

        // Controls (Row 5)
        if (!session.isGameOver) {
            builder.item(39, new GuiItem(createItem(Material.LIME_WOOL, Component.text("HIT", NamedTextColor.GREEN, TextDecoration.BOLD)),
                    e -> handleHit(player, session)));
            builder.item(41, new GuiItem(createItem(Material.RED_WOOL, Component.text("STAND", NamedTextColor.RED, TextDecoration.BOLD)),
                    e -> handleStand(player, session)));
        }

        // Status (Row 6)
        builder.item(48, new GuiItem(createItem(Material.BOOK, Component.text("How to Play", NamedTextColor.AQUA)),
                e -> {
                    player.getInventory().addItem(createInstructionsBook());
                    player.sendMessage(Component.text("You received the Blackjack Guide!", NamedTextColor.GREEN));
                }));

        builder.item(49, new GuiItem(createItem(Material.GOLD_INGOT, Component.text("Current Bet: " + INITIAL_BET + " Gold", NamedTextColor.GOLD)), null));

        if (session.isGameOver) {
            builder.item(50, new GuiItem(createItem(Material.ARROW, Component.text("Play Again", NamedTextColor.YELLOW)),
                    e -> start(player)));
        }

        context.gui().open(player, builder.build());
    }

    private ItemStack createInstructionsBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.title(Component.text("Blackjack Guide", NamedTextColor.GOLD));
            meta.author(Component.text("Casino Management"));

            meta.addPages(
                    Component.text("Welcome to the Casino's Blackjack Game!\n\nBlackjack is a game of skill and luck where you compete against the Dealer.\n\nThe goal is to have a hand value closer to 21 than the dealer, without going over (Busting)."),
                    Component.text("The Basics\n- Entry Fee: 1 Gold Ingot\n\nControls:\n- HIT (Lime Wool): Take a card.\n- STAND (Red Wool): End turn.\n\nOne dealer card stays hidden until you Stand."),
                    Component.text("Card Values:\n2: Coal Block\n3: Copper Block\n4: Iron Block\n5: Lapis Block\n6: Redstone Block\n7: Quartz Block\n8: Gold Block\n9: Emerald Block\n10: Diamond Block\nAce: Netherite Block"),
                    Component.text("\nThe Ace:\nThe Ace is special. It counts as 11 unless your card total would make you bust (hand value higher than 21), in which case it counts as 1."),
                    Component.text("Dealer Rules:\nThe Dealer must keep hitting until their total is 17 or higher.\n\nIf the Dealer busts, you win!\n\nIf the Dealer's starting hand has a value of 21, the game is a Push (Tie)."),
                    Component.text("Payouts:\n- Win: 2 Gold Ingots\n- Push (Tie): 1 Gold Ingot (Refund)\n- Bust/Loss: 0 Gold\n\nGood luck!")
            );
            book.setItemMeta(meta);
        }
        return book;
    }

    private int getSymmetricalSlot(int rowStart, int index, int total) {
        int center = rowStart + 4;
        return center - (total - 1) + (index * 2);
    }

    private void handleHit(Player player, BlackjackSession session) {
        session.playerHand.add(drawCard());
        if (calculateTotal(session.playerHand) > MAX_HAND_VALUE) {
            endGame(player, session, "Bust! You lose.");
        } else {
            openGameGui(player, session);
        }
    }

    private void handleStand(Player player, BlackjackSession session) {
        while (calculateTotal(session.dealerHand) < DEALER_THRESHOLD) {
            session.dealerHand.add(drawCard());
        }

        int playerTotal = calculateTotal(session.playerHand);
        int dealerTotal = calculateTotal(session.dealerHand);

        if (dealerTotal > MAX_HAND_VALUE) {
            context.economy().deposit(player, INITIAL_BET * 2);
            endGame(player, session, "Dealer Bust! You win 2 gold.");
        } else if (playerTotal > dealerTotal) {
            context.economy().deposit(player, INITIAL_BET * 2);
            endGame(player, session, "You win 2 gold!");
        } else if (playerTotal < dealerTotal) {
            endGame(player, session, "Dealer wins.");
        } else {
            context.economy().deposit(player, INITIAL_BET);
            endGame(player, session, "Push (Tie). Money back.");
        }
    }

    private void endGame(Player player, BlackjackSession session, String message) {
        session.isGameOver = true;
        player.sendMessage(Component.text(message, NamedTextColor.YELLOW));
        openGameGui(player, session);
    }

    private int calculateTotal(List<Integer> hand) {
        int total = 0;
        int aces = 0;

        for (int card : hand) {
            total += card;
            if (card == 11) aces++;
        }

        // Reduce Aces from 11 to 1 if busting
        while (total > MAX_HAND_VALUE && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    private ItemStack createCardItem(int value) {
        Material material = MATERIAL_MAP.getOrDefault(value, Material.PAPER);
        String displayValue = value == 11 ? "Ace (11 or 1)" : String.valueOf(value);
        return createItem(material, Component.text("Card: " + displayValue, NamedTextColor.WHITE));
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

    private static class BlackjackSession {
        final List<Integer> playerHand = new ArrayList<>();
        final List<Integer> dealerHand = new ArrayList<>();
        boolean isGameOver = false;
    }
}
