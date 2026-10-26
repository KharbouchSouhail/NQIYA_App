package ma.nqiya.spot;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ma.nqiya.user.User;

/** A user who joined a spot; {@code checkedInAt} is set when the organizer scans their QR ticket. */
@Entity
@Table(name = "participation")
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id")
    private Spot spot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    protected Participation() {
    }

    public Participation(Spot spot, User user) {
        this.spot = spot;
        this.user = user;
    }

    public boolean isCheckedIn() {
        return checkedInAt != null;
    }

    public void checkIn() {
        if (checkedInAt == null) {
            checkedInAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Spot getSpot() {
        return spot;
    }

    public User getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }
}
