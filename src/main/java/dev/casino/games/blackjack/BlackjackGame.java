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

    private static final int INITIAL_BET = 1;
    private static final int DEALER_THRESHOLD = 17;

    public BlackjackGame(PluginContext context) {
        this.context = context;
    }

    @Override
    public void start(Player player) {
        stop(player);
        openBettingGui(player, INITIAL_BET);
    }

    private void openBettingGui(Player player, int currentBet) {
        GuiBuilder builder = new GuiBuilder()
                .title(Component.text("Place Your Bet", NamedTextColor.DARK_GRAY))
                .size(6);

        builder.item(BlackjackLayout.BET_DECREASE, new GuiItem(createItem(Material.RED_WOOL, Component.text("Decrease Bet (-1) " + currentBet + " Gold", NamedTextColor.RED)),
                e -> {
                    if (currentBet > 1) {
                        openBettingGui(player, currentBet - 1);
                    }
                }));

        builder.item(BlackjackLayout.BET_DISPLAY, new GuiItem(createItem(Material.GOLD_INGOT, Component.text("Current Bet: " + currentBet + " Gold", NamedTextColor.GOLD)), null));

        builder.item(BlackjackLayout.BET_INCREASE, new GuiItem(createItem(Material.LIME_WOOL, Component.text("Increase Bet (+1) " + currentBet + " Gold", NamedTextColor.GREEN)),
                e -> {
                    if (context.economy().getBalance(player) > currentBet) {
                        openBettingGui(player, currentBet + 1);
                    } else {
                        player.sendMessage(Component.text("You don't have enough gold to increase the bet!", NamedTextColor.RED));
                    }
                }));

        builder.item(BlackjackLayout.BET_DEAL, new GuiItem(createItem(Material.GOLDEN_SWORD, Component.text("PLAY", NamedTextColor.AQUA, TextDecoration.BOLD)),
                e -> initializeGame(player, currentBet)));

        builder.item(BlackjackLayout.RULE_BOOK, new GuiItem(createItem(Material.BOOK, Component.text("How to Play", NamedTextColor.AQUA)),
                e -> {
                    player.getInventory().addItem(createInstructionsBook());
                    player.sendMessage(Component.text("You received the Blackjack Guide!", NamedTextColor.GREEN));
                }));

        builder.item(BlackjackLayout.GAME_QUIT, new GuiItem(createItem(Material.ARROW, Component.text("BACK")), e -> context.openMainCasinoMenu(player)));

        context.gui().open(player, builder.build());
    }

    private void initializeGame(Player player, int bet) {
        if (!context.economy().withdraw(player, bet)) {
            player.sendMessage(Component.text("You don't have enough gold!", NamedTextColor.RED));
            return;
        }

        context.betting().getBlackjackStats(player).addBet(bet);
        context.statsManager().saveStatsAsync();

        BlackjackSession session = new BlackjackSession(bet);

        // Initial deal
        session.playerHand.addCard(session.deck.draw());
        session.playerHand.addCard(session.deck.draw());
        session.dealerHand.addCard(session.deck.draw());
        session.dealerHand.addCard(session.deck.draw());

        sessions.put(player.getUniqueId(), session);

        // Check for Dealer Natural 21 (Push Rule)
        if (session.dealerHand.getValue() == BlackjackHand.MAX_VALUE) {
            context.economy().deposit(player, bet);
            context.betting().getBlackjackStats(player).addWon(bet);
            context.statsManager().saveStatsAsync();
            endGame(player, session, "Dealer has 21! It's a draw.");
        } else {
            openGameGui(player, session);
        }
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

    private void openGameGui(Player player, BlackjackSession session) {
        GuiBuilder builder = new GuiBuilder()
                .title(Component.text("Blackjack Table", NamedTextColor.DARK_GRAY))
                .size(6);

        // Dealer's Hand (Row 1: Slots 0-8)
        renderHand(builder, session.dealerHand, BlackjackLayout.DEALER_HAND_START, !session.isGameOver);

        // Player's Hand (Row 3: Slots 18-26, Row 4: 27-35)
        renderHand(builder, session.playerHand, BlackjackLayout.PLAYER_HAND_START, false);

        // Controls (Row 5)
        if (!session.isGameOver) {
            builder.item(BlackjackLayout.GAME_HIT, new GuiItem(createItem(Material.LIME_WOOL, Component.text("HIT", NamedTextColor.GREEN, TextDecoration.BOLD)),
                    e -> handleHit(player)));
            builder.item(BlackjackLayout.GAME_STAND, new GuiItem(createItem(Material.RED_WOOL, Component.text("STAND", NamedTextColor.RED, TextDecoration.BOLD)),
                    e -> handleStand(player)));
        }

        builder.item(BlackjackLayout.GAME_BET_DISPLAY, new GuiItem(createItem(Material.GOLD_INGOT, Component.text("Current Bet: " + session.bet + " Gold", NamedTextColor.GOLD)), null));

        if (session.isGameOver) {
            builder.item(BlackjackLayout.GAME_PLAY_AGAIN, new GuiItem(createItem(Material.GOLDEN_SWORD, Component.text("Play Again", NamedTextColor.YELLOW)),
                    e -> initializeGame(player, session.bet)));
            builder.item(BlackjackLayout.GAME_QUIT, new GuiItem(createItem(Material.ARROW, Component.text("Change Bet", NamedTextColor.RED)),
                    e -> {
                        stop(player);
                        openBettingGui(player, session.bet);
                    }));
        } else {
            builder.item(BlackjackLayout.GAME_QUIT, new GuiItem(createItem(Material.ARROW, Component.text("Quit to Betting", NamedTextColor.GRAY)),
                    e -> {
                        stop(player);
                        openBettingGui(player, session.bet);
                    }));
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
                    Component.text("The Basics:\n- Deck: 52 standard cards\n- Hand Layout:\n- Dealer Hand: Top of the screen\n- Player Hand: Middle of the screen\n\nControls:\n- HIT (Lime Wool): Take a card.\n- STAND (Red Wool): End turn.\n\nOne dealer card stays hidden until you Stand."),
                    Component.text("Card Values:\n\n2: Coal Block\n3: Copper Block\n4: Iron Block\n5: Lapis Block\n6: Redstone Block\n7: Netherite Quartz Ore\n8: Gold Block\n9: Emerald Block\n10: Diamond"),
                    Component.text("Card Values pt. 2:\n\nJack (10): Deepslate Diamond Ore\nQueen (10): Diamond Ore\nKing (10): Diamond Block\nAce (11 or 1): Netherite Block"),
                    Component.text("The Ace:\n\nThe Ace is special. It counts as 11 unless your card total would make you bust (hand value higher than 21), in which case it counts as 1."),
                    Component.text("Dealer Rules:\nThe Dealer must keep hitting until their total is 17 or higher.\n\nIf the Dealer busts, you win!\n\nIf the Dealer's starting hand has a value of 21, the game is a Push (Tie)."),
                    Component.text("Payouts:\n- Win: 2x Bet\n- Push (Tie): 1x Bet (Refund)\n- Bust/Loss: 0 Gold\n\nGood luck!")
            );
            book.setItemMeta(meta);
        }
        return book;
    }

    private void renderHand(GuiBuilder builder, BlackjackHand hand, int rowStart, boolean hideSecondCard) {
        List<Card> cards = hand.getCards();
        for (int i = 0; i < cards.size(); i++) {
            int rowOffset = (i / 9) * 9;
            int currentRowStart = rowStart + rowOffset;
            int indexInRow = i % 9;
            int startOfThisRow = (i / 9) * 9;
            int totalInThisRow = Math.min(9, cards.size() - startOfThisRow);

            int slot = BlackjackLayout.getCardSlot(currentRowStart, totalInThisRow, indexInRow);
            if (hideSecondCard && i == 1) {
                builder.item(slot, new GuiItem(createItem(Material.GRAY_STAINED_GLASS, Component.text("Hidden Card", NamedTextColor.GRAY)), null));
                break;
            }
            builder.item(slot, new GuiItem(createCardItem(cards.get(i)), null));
        }
    }

    private void handleHit(Player player) {
        BlackjackSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isGameOver) return;

        session.playerHand.addCard(session.deck.draw());
        if (session.playerHand.isBust()) {
            endGame(player, session, "Bust! You lose.");
        } else {
            openGameGui(player, session);
        }
    }

    private void handleStand(Player player) {
        BlackjackSession session = sessions.get(player.getUniqueId());
        if (session == null || session.isGameOver) return;

        while (session.dealerHand.getValue() < DEALER_THRESHOLD) {
            session.dealerHand.addCard(session.deck.draw());
        }

        int playerTotal = session.playerHand.getValue();
        int dealerTotal = session.dealerHand.getValue();

        if (session.dealerHand.isBust()) {
            int win = session.bet * 2;
            context.economy().deposit(player, win);
            context.betting().getBlackjackStats(player).addWon(win);
            context.statsManager().saveStatsAsync();
            endGame(player, session, "Dealer Bust! You win " + win + " gold.");
        } else if (playerTotal > dealerTotal) {
            int win = session.bet * 2;
            context.economy().deposit(player, win);
            context.betting().getBlackjackStats(player).addWon(win);
            context.statsManager().saveStatsAsync();
            endGame(player, session, "You win " + win + " gold!");
        } else if (playerTotal < dealerTotal) {
            endGame(player, session, "Dealer wins.");
        } else {
            context.economy().deposit(player, session.bet);
            context.betting().getBlackjackStats(player).addWon(session.bet);
            context.statsManager().saveStatsAsync();
            endGame(player, session, "Push (Tie). Money back.");
        }
    }

    private void endGame(Player player, BlackjackSession session, String message) {
        session.isGameOver = true;
        player.sendMessage(Component.text(message, NamedTextColor.YELLOW));
        openGameGui(player, session);
    }

    private ItemStack createCardItem(Card card) {
        return createItem(card.getMaterial(), Component.text("Card: " + card.getDisplayName(), NamedTextColor.WHITE));
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
        final Deck deck = new Deck();
        final BlackjackHand playerHand = new BlackjackHand();
        final BlackjackHand dealerHand = new BlackjackHand();
        final int bet;
        boolean isGameOver = false;

        BlackjackSession(int bet) {
            this.bet = bet;
        }
    }
}
