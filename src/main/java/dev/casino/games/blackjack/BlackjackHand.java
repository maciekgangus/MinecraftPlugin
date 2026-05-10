package dev.casino.games.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlackjackHand {
    public static final int MAX_VALUE = 21;
    private final List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
    }

    public int getValue() {
        int total = 0;
        int aces = 0;

        for (Card card : cards) {
            total += card.getValue();
            if (card == Card.ACE) {
                aces++;
            }
        }

        while (total > MAX_VALUE && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }

    public boolean isBust() {
        return getValue() > MAX_VALUE;
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
    
    public int size() {
        return cards.size();
    }
}
