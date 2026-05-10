package dev.casino.games.blackjack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void deckStartsFull() {
        Deck deck = new Deck();
        assertEquals(52, deck.size());
    }

    @Test
    void drawingReducesSize() {
        Deck deck = new Deck();
        deck.draw();
        assertEquals(51, deck.size());
    }

    @Test
    void deckReplenishesWhenEmpty() {
        Deck deck = new Deck();
        for (int i = 0; i < 52; i++) {
            deck.draw();
        }
        assertEquals(0, deck.size());
        
        Card card = deck.draw();
        assertNotNull(card);
        assertEquals(51, deck.size());
    }

    @Test
    void deckContainsCorrectCardCounts() {
        Deck deck = new Deck();
        int[] counts = new int[Card.values().length];
        for (int i = 0; i < 52; i++) {
            Card card = deck.draw();
            counts[card.ordinal()]++;
        }
        
        for (int count : counts) {
            assertEquals(4, count, "Each card type should appear exactly 4 times in a 52-card deck");
        }
    }
}
