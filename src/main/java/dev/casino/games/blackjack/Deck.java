package dev.casino.games.blackjack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Deck {
    private final List<Card> cards = new ArrayList<>();

    public Deck() {
        replenish();
    }

    public Card draw() {
        if (cards.isEmpty()) {
            replenish();
        }
        return cards.removeFirst();
    }

    private void replenish() {
        cards.clear();
        for (Card card : Card.values()) {
            for (int i = 0; i < 4; i++) {
                cards.add(card);
            }
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    public int size() {
        return cards.size();
    }
}
