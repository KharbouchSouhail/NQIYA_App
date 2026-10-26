package ma.nqiya.reward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ma.nqiya.reward.RewardDtos.RewardSummary;
import ma.nqiya.user.Rank;
import ma.nqiya.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RewardServiceTest {

    private RewardService service;

    @BeforeEach
    void setUp() {
        RewardEventRepository events = mock(RewardEventRepository.class);
        when(events.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new RewardService(events);
    }

    @Test
    void volunteerGetsBaseReward() {
        User user = user(0, 0);
        RewardSummary r = service.payCleanup(user, 1L, "Beach", false);
        assertThat(r.xpGained()).isEqualTo(100);
        // 100 coins + 100 coins Silver gift (crossed 100 XP)
        assertThat(r.coinsGained()).isEqualTo(200);
        assertThat(r.rankAfter()).isEqualTo(Rank.SILVER);
        assertThat(user.getCoins()).isEqualTo(200);
    }

    @Test
    void organizerGetsBonus() {
        User user = user(150, 0);
        RewardSummary r = service.payCleanup(user, 1L, "Park", true);
        assertThat(r.xpGained()).isEqualTo(150);
        assertThat(r.coinsGained()).isEqualTo(150);
        assertThat(r.rankedUp()).isFalse();
    }

    @Test
    void platinumDoublesCoins() {
        User user = user(1300, 0);
        RewardSummary r = service.payCleanup(user, 1L, "Street", false);
        assertThat(r.xpGained()).isEqualTo(100);
        assertThat(r.coinsGained()).isEqualTo(200);
    }

    @Test
    void diamondDoublesXpAndCoins() {
        User user = user(3000, 0);
        RewardSummary r = service.payCleanup(user, 1L, "Street", true);
        assertThat(r.xpGained()).isEqualTo(300);
        assertThat(r.coinsGained()).isEqualTo(300);
    }

    @Test
    void reachingDiamondPaysGift() {
        User user = user(2950, 0);
        RewardSummary r = service.payCleanup(user, 1L, "Port", false);
        assertThat(r.rankBefore()).isEqualTo(Rank.PLATINUM);
        assertThat(r.rankAfter()).isEqualTo(Rank.DIAMOND);
        assertThat(r.giftCoins()).isEqualTo(1000);
        assertThat(user.getCoins()).isEqualTo(200 + 1000);
    }

    private static User user(int xp, int coins) {
        User u = new User("a@b.ma", "Amine", "ABCDEFGH");
        ReflectionTestUtils.setField(u, "id", 7L);
        ReflectionTestUtils.setField(u, "xp", xp);
        ReflectionTestUtils.setField(u, "coins", coins);
        return u;
    }
}
