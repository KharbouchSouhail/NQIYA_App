package ma.nqiya.reward;

import java.util.List;

import ma.nqiya.reward.RewardDtos.HistoryItem;
import ma.nqiya.reward.RewardDtos.RewardSummary;
import ma.nqiya.user.Rank;
import ma.nqiya.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The XP / coin economy. Must run inside the caller's transaction so a cleanup payout is all-or-nothing.
 */
@Service
public class RewardService {

    public static final int CLEANUP_XP = 100;
    public static final int CLEANUP_COINS = 100;
    public static final int ORGANIZER_XP = 50;
    public static final int ORGANIZER_COINS = 50;

    private final RewardEventRepository events;

    public RewardService(RewardEventRepository events) {
        this.events = events;
    }

    /**
     * Pays one checked-in participant for a verified cleanup, applying their rank multipliers
     * and any level-up gift they unlock along the way.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public RewardSummary payCleanup(User user, Long spotId, String spotTitle, boolean organizer) {
        Rank before = user.rank();
        int xp = CLEANUP_XP * before.xpMultiplier();
        int coins = CLEANUP_COINS * before.coinMultiplier();
        credit(user, RewardEvent.Type.CLEANUP, xp, coins, spotId, "Cleaned: " + spotTitle);

        if (organizer) {
            int bonusXp = ORGANIZER_XP * before.xpMultiplier();
            int bonusCoins = ORGANIZER_COINS * before.coinMultiplier();
            credit(user, RewardEvent.Type.ORGANIZER_BONUS, bonusXp, bonusCoins, spotId, "Organized: " + spotTitle);
            xp += bonusXp;
            coins += bonusCoins;
        }

        int gifts = grantRankUpGifts(user, before, spotId);
        return new RewardSummary(user.getId(), xp, coins + gifts, gifts, before, user.rank(),
                user.getXp(), user.getCoins());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void debitPurchase(User user, int cost, String what) {
        user.spendCoins(cost);
        events.save(new RewardEvent(user.getId(), RewardEvent.Type.PURCHASE, 0, -cost, null, "Bought: " + what));
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(Long userId) {
        return events.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream().map(HistoryItem::of).toList();
    }

    /** One gift per rank crossed, so jumping two ranks at once pays both gifts. */
    private int grantRankUpGifts(User user, Rank before, Long spotId) {
        int total = 0;
        for (Rank r = before.next(); r != null && user.getXp() >= r.minXp(); r = r.next()) {
            credit(user, RewardEvent.Type.RANK_UP_GIFT, 0, r.levelUpGift(), spotId, "Mabrouk! You're now " + label(r));
            total += r.levelUpGift();
        }
        return total;
    }

    private void credit(User user, RewardEvent.Type type, int xp, int coins, Long spotId, String description) {
        user.addXp(xp);
        user.addCoins(coins);
        events.save(new RewardEvent(user.getId(), type, xp, coins, spotId, description));
    }

    private static String label(Rank rank) {
        String n = rank.name();
        return n.charAt(0) + n.substring(1).toLowerCase();
    }
}
