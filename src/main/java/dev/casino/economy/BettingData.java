package dev.casino.economy;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class BettingData {
    private int goldBet = 0;
    private int goldWon = 0;

    public void addBet(int amount) {
        this.goldBet += amount;
    }

    public void addWon(int amount) {
        this.goldWon += amount;
    }

    public int getBalance() {
        return goldWon - goldBet;
    }
}
