package ma.nqiya.reward;

import java.time.Instant;

import ma.nqiya.user.Rank;

public final class RewardDtos {

    private RewardDtos() {
    }

    /** What one user received from a single payout; drives the celebration overlay. */
    public record RewardSummary(Long userId, int xpGained, int coinsGained, int giftCoins,
                                Rank rankBefore, Rank rankAfter, int totalXp, int totalCoins) {

        public boolean rankedUp() {
            return rankAfter != rankBefore;
        }
    }

    public record HistoryItem(Long id, RewardEvent.Type type, int xpDelta, int coinsDelta, Long spotId,
                              String description, Instant createdAt) {

        public static HistoryItem of(RewardEvent e) {
            return new HistoryItem(e.getId(), e.getType(), e.getXpDelta(), e.getCoinsDelta(), e.getSpotId(),
                    e.getDescription(), e.getCreatedAt());
        }
    }
}
