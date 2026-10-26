package ma.nqiya.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

    @Test
    void mohammediaToCasablancaIsAbout24Km() {
        double km = GeoUtils.distanceKm(33.6861, -7.3829, 33.5731, -7.5898);
        assertThat(km).isCloseTo(23.0, within(2.0));
    }

    @Test
    void samePointIsZero() {
        assertThat(GeoUtils.distanceKm(33.7, -7.4, 33.7, -7.4)).isZero();
    }
}
