package ma.nqiya.user;

/**
 * The five NQIYA ranks. Thresholds, multipliers and level-up gifts follow the product spec.
 */
public enum Rank {

    BRONZE(0, 1, 1, 0, "Every hero starts here."),
    SILVER(100, 1, 1, 100, "You clean up after the city. Respect."),
    GOLD(400, 1, 1, 250, "Spotless, and the whole neighbourhood talks about you."),
    PLATINUM(1200, 1, 2, 500, "Morocco's cleanup superhero."),
    DIAMOND(3000, 2, 2, 1000, "Legend status. The city is greener thanks to you.");

    private final int minXp;
    private final int xpMultiplier;
    private final int coinMultiplier;
    private final int levelUpGift;
    private final String tagline;

    Rank(int minXp, int xpMultiplier, int coinMultiplier, int levelUpGift, String tagline) {
        this.minXp = minXp;
        this.xpMultiplier = xpMultiplier;
        this.coinMultiplier = coinMultiplier;
        this.levelUpGift = levelUpGift;
        this.tagline = tagline;
    }

    public static Rank forXp(int xp) {
        Rank result = BRONZE;
        for (Rank r : values()) {
            if (xp >= r.minXp) {
                result = r;
            }
        }
        return result;
    }

    /** @return the next rank, or {@code null} at the top. */
    public Rank next() {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }

    public int minXp() {
        return minXp;
    }

    public int xpMultiplier() {
        return xpMultiplier;
    }

    public int coinMultiplier() {
        return coinMultiplier;
    }

    public int levelUpGift() {
        return levelUpGift;
    }

    public String tagline() {
        return tagline;
    }
}
