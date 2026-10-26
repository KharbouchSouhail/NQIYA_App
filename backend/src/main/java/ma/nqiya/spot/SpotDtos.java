package ma.nqiya.spot;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ma.nqiya.reward.RewardDtos.RewardSummary;
import ma.nqiya.user.UserDtos.PublicUser;

public final class SpotDtos {

    private SpotDtos() {
    }

    public static String photoUrl(UUID id) {
        return id == null ? null : "/api/photos/" + id;
    }

    /** Lightweight marker data for the map. */
    public record SpotSummary(Long id, String title, SpotStatus status, Visibility visibility, double latitude,
                              double longitude, int participantCount, Instant meetingAt, String dirtLevel,
                              String beforePhotoUrl, Double distanceKm) {
    }

    public record ViewerState(boolean authenticated, boolean organizer, boolean joined, boolean checkedIn,
                              boolean canJoin) {
    }

    public record SpotDetail(Long id, String title, String description, SpotStatus status, Visibility visibility,
                             boolean anonymous, boolean closed, double latitude, double longitude,
                             Instant meetingAt, PublicUser organizer, String beforePhotoUrl, String afterPhotoUrl,
                             String dirtLevel, String wasteType, int participantCount, long checkedInCount,
                             Instant createdAt, Instant startedAt, Instant cleanedAt, ViewerState viewer) {
    }

    /** Result of the AI photo gate on a BEFORE photo; the client then publishes with {@code photoId}. */
    public record ReportDraft(UUID photoId, boolean accepted, String title, String description, String dirtLevel,
                              String wasteType, String reason) {
    }

    public record CreateSpotRequest(
            @NotNull UUID photoId,
            @NotBlank @Size(max = 80) String title,
            @Size(max = 400) String description,
            @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @NotNull @PositiveOrZero @Max(value = 100, message = "GPS fix must be accurate to 100 m or better")
            Double accuracyMeters,
            @NotNull Visibility visibility,
            boolean anonymous,
            Instant meetingAt) {
    }

    public record CheckInRequest(@NotBlank @Size(max = 12) String ticketCode) {
    }

    public record CheckInResult(PublicUser volunteer, Instant checkedInAt, boolean alreadyCheckedIn,
                                SpotStatus spotStatus, long checkedInCount) {
    }

    public record ParticipantDto(PublicUser user, Instant joinedAt, Instant checkedInAt, boolean organizer) {
    }

    public record FinishResult(boolean passed, String reason, SpotDetail spot, RewardSummary myReward,
                               int paidVolunteers) {
    }

    public record MyCleanup(Long spotId, String title, SpotStatus status, boolean organizer, boolean checkedIn,
                            Instant joinedAt, Instant meetingAt, String beforePhotoUrl) {
    }
}
