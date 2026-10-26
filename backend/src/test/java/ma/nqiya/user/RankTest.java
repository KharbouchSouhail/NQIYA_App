package ma.nqiya.user;

import static org.assertj.core.api.Assertions.assertThat;

import ma.nqiya.user.UserDtos.RankProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RankTest {

    @ParameterizedTest
    @CsvSource({"0,BRONZE", "99,BRONZE", "100,SILVER", "399,SILVER", "400,GOLD", "1200,PLATINUM", "2999,PLATINUM",
            "3000,DIAMOND", "99999,DIAMOND"})
    void rankForXp(int xp, Rank expected) {
        assertThat(Rank.forXp(xp)).isEqualTo(expected);
    }

    @Test
    void progressTowardsNextRank() {
        RankProgress p = RankProgress.of(250);
        assertThat(p.current()).isEqualTo(Rank.SILVER);
        assertThat(p.next()).isEqualTo(Rank.GOLD);
        assertThat(p.percent()).isEqualTo(50);
    }

    @Test
    void diamondIsTheTop() {
        RankProgress p = RankProgress.of(5000);
        assertThat(p.next()).isNull();
        assertThat(p.percent()).isEqualTo(100);
    }
}
