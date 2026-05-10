package dev.casino.games.blackjack;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlackjackHandTest {

    @Test
    void emptyHandHasZeroValue() {
        BlackjackHand hand = new BlackjackHand();
        assertEquals(0, hand.getValue());
        assertFalse(hand.isBust());
    }

    @Test
    void singleCardValue() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.FIVE);
        assertEquals(5, hand.getValue());
    }

    @Test
    void multipleCardsValue() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.TWO);
        hand.addCard(Card.THREE);
        hand.addCard(Card.KING);
        assertEquals(15, hand.getValue());
    }

    @Test
    void aceAsEleven() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.FIVE);
        hand.addCard(Card.ACE);
        assertEquals(16, hand.getValue());
    }

    @Test
    void aceAsOneWhenBusting() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.TEN);
        hand.addCard(Card.KING);
        hand.addCard(Card.ACE);
        assertEquals(21, hand.getValue());
        assertFalse(hand.isBust());
    }

    @Test
    void multipleAces() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.ACE);
        hand.addCard(Card.ACE);
        assertEquals(12, hand.getValue());
    }

    @Test
    void complexAceScenario() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.ACE);
        hand.addCard(Card.NINE);
        hand.addCard(Card.ACE);
        assertEquals(21, hand.getValue());
        
        hand.addCard(Card.ACE);
        assertEquals(12, hand.getValue());
    }

    @Test
    void bustLogic() {
        BlackjackHand hand = new BlackjackHand();
        hand.addCard(Card.TEN);
        hand.addCard(Card.JACK);
        hand.addCard(Card.TWO);
        assertEquals(22, hand.getValue());
        assertTrue(hand.isBust());
    }
}
