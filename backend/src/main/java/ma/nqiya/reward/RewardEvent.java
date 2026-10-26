package ma.nqiya.reward;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Immutable ledger line: every XP or coin movement is recorded here and shown in the history feed. */
@Entity
@Table(name = "reward_event")
public class RewardEvent {

    public enum Type { CLEANUP, ORGANIZER_BONUS, RANK_UP_GIFT, PURCHASE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "xp_delta")
    private int xpDelta;

    @Column(name = "coins_delta")
    private int coinsDelta;

    @Column(name = "spot_id")
    private Long spotId;

    @Column(nullable = false)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected RewardEvent() {
    }

    public RewardEvent(Long userId, Type type, int xpDelta, int coinsDelta, Long spotId, String description) {
        this.userId = userId;
        this.type = type;
        this.xpDelta = xpDelta;
        this.coinsDelta = coinsDelta;
        this.spotId = spotId;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Type getType() {
        return type;
    }

    public int getXpDelta() {
        return xpDelta;
    }

    public int getCoinsDelta() {
        return coinsDelta;
    }

    public Long getSpotId() {
        return spotId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
