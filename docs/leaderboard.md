# Leaderboard & Statistics

## Overview

The Casino Plugin features a real-time leaderboard system that tracks player performance across all games. All statistics are persisted to disk to ensure data is preserved across server restarts and crashes.

## User Guide

### Usage
**Command:** `/leaderboard`  
**Permission:** `leaderboard.view` (Default: true)

### Leaderboard Sections
The output is divided into four main sections:
1.  **ALL GAMES**: A combined ranking based on global net balance.
2.  **BLACKJACK**: Specific rankings for the Blackjack game.
3.  **HORSE RACING**: Specific rankings for the Virtual Derby.
4.  **SLOTS**: Specific rankings for the Slot Machine.

### Data Displayed
For each entry, the command displays:
- **Player Name**: Fetched via UUID (supports offline players).
- **Profit**: The net profit or loss (Gold Won - Gold Bet).
- **Won**: Total gold collected from wins.
- **Bet**: Total gold spent on entries/stakes.

**Visual Indicators:**
- Positive balances are shown in **GREEN**.
- Negative balances are shown in **RED**.

## Technical Implementation

### Persistence & Storage
All data is stored in the `plugins/CasinoPlugin/stats.yml` file using a structured YAML format. Statistics are indexed by **Player UUID** to ensure that data remains tied to the player even if they change their Minecraft username.

### Save Mechanisms
To guarantee data safety, the plugin employs two layers of saving:
1.  **Immediate Async Save**: After every "Bet" or "Win" event, the plugin triggers an asynchronous save to the disk.
2.  **Shutdown Save**: When the server stops gracefully, a final save is performed as a fallback.

### Performance & Safety
- **Thread Safety**: All file-writing operations are `synchronized` and performed on a background thread to prevent server lag.
- **Offline Support**: Stats are loaded for all players on startup, allowing the leaderboard to display data for players who are currently offline.

## Main Classes

- **Command Executor**: `src/main/java/dev/casino/commands/LeaderBoardCommand.java`
- **Manager**: `src/main/java/dev/casino/economy/StatsManager.java` (File I/O and serialization)
- **Data Source**: `src/main/java/dev/casino/economy/CasinoBettingData.java` (In-memory registry)
- **Data Structure**: `src/main/java/dev/casino/economy/BettingData.java` (Numeric stats container)
