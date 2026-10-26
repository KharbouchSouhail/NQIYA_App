package ma.nqiya.config;

import java.util.Arrays;
import java.util.List;

import ma.nqiya.reward.RewardService;
import ma.nqiya.user.Rank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, cacheable game rules so the client never hard-codes them. */
@RestController
public class ConfigController {

    private final AppProperties props;

    public ConfigController(AppProperties props) {
        this.props = props;
    }

    public record RankInfo(Rank rank, int minXp, int xpMultiplier, int coinMultiplier, int levelUpGift,
                           String tagline) {
    }

    public record Rewards(int cleanupXp, int cleanupCoins, int organizerXp, int organizerCoins) {
    }

    public record PublicConfig(List<RankInfo> ranks, Rewards rewards, int maxRadiusKm, int maxGpsAccuracyMeters,
                               String aiProvider) {
    }

    @GetMapping("/api/config")
    public PublicConfig config() {
        List<RankInfo> ranks = Arrays.stream(Rank.values())
                .map(r -> new RankInfo(r, r.minXp(), r.xpMultiplier(), r.coinMultiplier(), r.levelUpGift(), r.tagline()))
                .toList();
        return new PublicConfig(ranks,
                new Rewards(RewardService.CLEANUP_XP, RewardService.CLEANUP_COINS,
                        RewardService.ORGANIZER_XP, RewardService.ORGANIZER_COINS),
                10, 100, props.ai().provider());
    }
}
