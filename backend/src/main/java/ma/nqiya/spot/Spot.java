package ma.nqiya.spot;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ma.nqiya.common.exception.ApiException;
import ma.nqiya.user.User;

@Entity
@Table(name = "spot")
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status = SpotStatus.DIRTY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    private boolean anonymous;

    private boolean closed;

    @Column(name = "meeting_at")
    private Instant meetingAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @Column(name = "before_photo_id", nullable = false)
    private UUID beforePhotoId;

    @Column(name = "after_photo_id")
    private UUID afterPhotoId;

    @Column(name = "dirt_level")
    private String dirtLevel;

    @Column(name = "waste_type")
    private String wasteType;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "cleaned_at")
    private Instant cleanedAt;

    @Version
    private long version;

    protected Spot() {
    }

    public Spot(String title, String description, double latitude, double longitude, Visibility visibility,
                boolean anonymous, Instant meetingAt, User organizer, UUID beforePhotoId,
                String dirtLevel, String wasteType) {
        this.title = title;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.visibility = visibility;
        this.anonymous = anonymous;
        this.meetingAt = meetingAt;
        this.organizer = organizer;
        this.beforePhotoId = beforePhotoId;
        this.dirtLevel = dirtLevel;
        this.wasteType = wasteType;
    }

    public boolean isOrganizer(Long userId) {
        return organizer.getId().equals(userId);
    }

    public boolean isJoinable() {
        return visibility == Visibility.PUBLIC && !closed && status != SpotStatus.CLEAN;
    }

    /** First check-in flips the spot to "cleaning now". */
    public void startCleaning() {
        ensureOpen();
        if (status == SpotStatus.DIRTY) {
            status = SpotStatus.CLEANING;
            startedAt = Instant.now();
        }
    }

    public void markCleaned(UUID afterPhotoId) {
        ensureOpen();
        if (status != SpotStatus.CLEANING) {
            throw ApiException.conflict("NOT_CLEANING", "Check in at least one volunteer before finishing");
        }
        this.status = SpotStatus.CLEAN;
        this.afterPhotoId = afterPhotoId;
        this.cleanedAt = Instant.now();
    }

    public void ensureOpen() {
        if (closed) {
            throw ApiException.conflict("SPOT_CLOSED", "This spot was closed by a moderator");
        }
        if (status == SpotStatus.CLEAN) {
            throw ApiException.conflict("SPOT_CLEAN", "This spot is already clean");
        }
    }

    public void incrementParticipants() {
        participantCount++;
    }

    public void decrementParticipants() {
        participantCount = Math.max(0, participantCount - 1);
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public boolean isClosed() {
        return closed;
    }

    public Instant getMeetingAt() {
        return meetingAt;
    }

    public User getOrganizer() {
        return organizer;
    }

    public UUID getBeforePhotoId() {
        return beforePhotoId;
    }

    public UUID getAfterPhotoId() {
        return afterPhotoId;
    }

    public String getDirtLevel() {
        return dirtLevel;
    }

    public String getWasteType() {
        return wasteType;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCleanedAt() {
        return cleanedAt;
    }
}
