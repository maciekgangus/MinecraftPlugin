# Slots Game Redesign

**Date:** 2026-05-29
**Status:** Approved

## Problem

The existing `SlotsGame` is broken for production use:
- `globalSpinLock` blocks all players — only one spin at a time across the entire server
- Result determined by an external Python script via `spin.txt`/`result.txt` file IPC
- No per-player state isolation

## Goal

A fully self-contained, multi-player slots game in pure Java, driven by Chest GUI, with animated reels and a proper pay table.

---

## Architecture

### Files changed / created

```
games/slots/
  SlotsGame.java     ← CasinoGame implementation (full rewrite)
  SlotSymbol.java    ← enum: material, weight, 3x payout, 2x payout
  SlotSession.java   ← per-player mutable state
  SlotsLayout.java   ← GUI slot constants
```

No classes outside this package are touched.

### Key design choice

`GuiBuilder` builds the initial static layout. Animation updates the player's open inventory directly via `player.getOpenInventory().getTopInventory().setItem(slot, item)` on the main thread (inside `BukkitRunnable`). No inventory reopen during animation — zero flicker.

---

## GUI Layout (3 rows, 27 slots)

```
Row 0:  [ ][ ][R1][ ][R2][ ][R3][ ][ ]
Row 1:  [ ][−][ ][BET][ ][+][ ][SPIN][ ]
Row 2:  [ ][ ][ ][ ][ ][ ][ ][ ][BACK]
```

| Slot | Element |
|------|---------|
| 2 | Reel 1 |
| 4 | Reel 2 |
| 6 | Reel 3 |
| 10 | Bet decrease (−1), red wool |
| 13 | Bet display, gold ingot |
| 15 | Bet increase (+1), lime wool |
| 17 | SPIN button, lever |
| 26 | BACK, arrow |

- Min bet: 1 ingot. Max bet: min(player balance, 64).
- +/− rebuild the GUI via `context.gui().open()` (same pattern as blackjack betting screen).
- While `spinning == true`, SPIN click is a no-op (no global lock needed).

---

## Symbols & Pay Table

| Symbol | Material | Weight | 3-of-a-kind | 2-of-a-kind |
|--------|----------|--------|-------------|-------------|
| Diamond | `DIAMOND` | 5% | ×20 bet | ×3 bet |
| Gold | `GOLD_INGOT` | 15% | ×8 bet | ×2 bet |
| Emerald | `EMERALD` | 20% | ×4 bet | ×1 bet (refund) |
| Iron | `IRON_INGOT` | 25% | ×2 bet | — |
| Coal | `COAL` | 35% | ×1 bet (refund) | — |

Weights are used to build a weighted random pool. Result drawn at spin start before animation begins.

**Win conditions:**
- 3 of a kind → payout from table above
- 2 of a kind (any two positions) → payout only for Diamond / Gold / Emerald
- No match → bet lost

---

## Animation Flow

One `BukkitRunnable` per player, period = 2 ticks (~100 ms/frame).

```
Ticks  0–19  (0–2 s):    All 3 reels spin — random symbol each frame
Ticks 20–24  (2–2.5 s):  Reel 1 locks → shows result[0]; reels 2–3 still spin
Ticks 25–29  (2.5–3 s):  Reel 2 locks → shows result[1]; reel 3 still spins
Tick  30     (3 s):       Reel 3 locks → shows result[2] → resolveResult()
```

`resolveResult()`:
1. Evaluate win condition
2. `economy.deposit()` if win
3. Update `CasinoBettingData` and save stats async
4. Send chat message with outcome and amount
5. Play win/lose sound
6. Set `session.spinning = false`
7. Rebuild GUI (re-enables SPIN button)

---

## Per-Player Session (`SlotSession`)

```java
class SlotSession {
    int bet = 1;
    boolean spinning = false;
    @Nullable BukkitTask animationTask;
    @Nullable Inventory openInventory;       // reference for close-detection
    SlotSymbol[] result = new SlotSymbol[3]; // set at spin start
}
```

Sessions stored in `Map<UUID, SlotSession>` (ConcurrentHashMap) in `SlotsGame`.

---

## Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| Player closes inventory mid-spin | `SlotSession` stores the `Inventory` reference opened at spin start. Each animation tick checks `player.getOpenInventory().getTopInventory() == session.inventory`. If false (closed or switched), `task.cancel()` + bet refunded. No new listeners required. |
| Player has insufficient balance | SPIN click rejected with chat message, no state change |
| `stop()` called when no session exists | No-op (idempotent per `CasinoGame` contract) |
| `+` pressed at max bet (64 or balance) | Ignored silently |
