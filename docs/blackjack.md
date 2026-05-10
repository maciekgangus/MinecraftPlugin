# Blackjack Game Documentation

## Overview

`BlackjackGame` is a chest-GUI casino game registered in the main `/casino` menu.
Players compete against a virtual dealer to reach a hand value as close to 21 as possible without "busting" (exceeding 21).

## Player Flow

1. Run `/casino`.
2. Click `Blackjack`.
3. **Betting Screen:** Use `Increase (+1)` and `Decrease (-1)` buttons to set your stake (min 1 gold).
4. Click `PLAY` to start.
5. **Game Screen:** Use `HIT` to take another card or `STAND` to end your turn.
6. **Dealer Turn:** Once the player stands, the dealer draws cards until they reach at least 17.
7. **Resolution:** View results, then click `Play Again` (same bet) or `Change Bet` (return to betting screen).

## Betting Options

- **Minimum Bet:** 1 Gold Ingot.
- **Maximum Bet:** Limited only by the player's current gold balance.
- **Adjustment:** Players can adjust their bet in 1-gold increments before each game.

## Game Mechanics

- **The Deck:** A standard 52-card deck is used. It is automatically replenished and reshuffled whenever it runs out of cards.
- **Card Values:** 
    - Number cards (2-10) are worth their face value.
    - Face cards (Jack, Queen, King) are worth 10.
    - Aces are worth 11, but automatically count as 1 if the hand would otherwise bust.
- **Dealer Rules:** The dealer must hit on any total below 17 and must stand on 17 or higher.
- **Natural 21:** If the dealer's initial two cards total 21, the game is a Push (Tie), and the player's bet is refunded.

## Economy Integration

- **Entry:** The selected bet is withdrawn from the player's inventory/Enderchest when `PLAY` is clicked.
- **Payouts:**
    - **Win:** 2x the bet amount.
    - **Push (Tie):** 1x the bet amount (Refund).
    - **Loss/Bust:** 0.

## Main Classes

- **Game Controller:** `src/main/java/dev/casino/games/blackjack/BlackjackGame.java`
- **Logic & Components:**
    - `Deck.java`: Manages the 52-card collection.
    - `Card.java`: Enum defining values and Minecraft materials.
    - `BlackjackHand.java`: Calculates values and handles Ace logic.
    - `BlackjackLayout.java`: Defines the GUI grid and slot indices.
- **Registration:** `src/main/java/dev/casino/games/GameRegistrar.java`
- **Tests:**
    - `src/test/java/dev/casino/games/blackjack/BlackjackGameTest.java`
    - `src/test/java/dev/casino/games/blackjack/BlackjackHandTest.java`
    - `src/test/java/dev/casino/games/blackjack/DeckTest.java`
