package ma.nqiya.user;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record RankProgress(Rank current, Rank next, int currentMinXp, Integer nextMinXp, int percent) {

        public static RankProgress of(int xp) {
            Rank current = Rank.forXp(xp);
            Rank next = current.next();
            if (next == null) {
                return new RankProgress(current, null, current.minXp(), null, 100);
            }
            int span = next.minXp() - current.minXp();
            int percent = (int) Math.floor(100.0 * (xp - current.minXp()) / span);
            return new RankProgress(current, next, current.minXp(), next.minXp(), percent);
        }
    }

    public record MeResponse(
            Long id,
            String email,
            String displayName,
            Role role,
            int xp,
            int coins,
            RankProgress rank,
            String ticketCode,
            long completedCleanups,
            Instant createdAt) {
    }

    /** What other users may see about someone. */
    public record PublicUser(Long id, String displayName, Rank rank, int xp) {

        public static PublicUser of(User user) {
            return new PublicUser(user.getId(), user.getDisplayName(), user.rank(), user.getXp());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 40)
            @Pattern(regexp = "^[\\p{L}\\p{N} ._'-]+$", message = "contains unsupported characters")
            String displayName) {
    }
}
