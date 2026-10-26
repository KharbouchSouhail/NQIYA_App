package ma.nqiya.admin;

import java.time.Instant;

import ma.nqiya.common.exception.ApiException;
import ma.nqiya.common.web.PageResponse;
import ma.nqiya.spot.Spot;
import ma.nqiya.spot.SpotRepository;
import ma.nqiya.spot.SpotStatus;
import ma.nqiya.spot.Visibility;
import ma.nqiya.user.Rank;
import ma.nqiya.user.Role;
import ma.nqiya.user.User;
import ma.nqiya.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Moderation: close / reopen / delete spots, manage admins. Every action is written to the audit log. */
@Service
public class AdminService {

    private static final int PAGE_SIZE = 30;

    private final SpotRepository spots;
    private final UserRepository users;
    private final AdminActionRepository actions;

    public AdminService(SpotRepository spots, UserRepository users, AdminActionRepository actions) {
        this.spots = spots;
        this.users = users;
        this.actions = actions;
    }

    public record AdminSpot(Long id, String title, SpotStatus status, Visibility visibility, boolean closed,
                            String organizerEmail, int participantCount, Instant createdAt) {
    }

    public record AdminUser(Long id, String email, String displayName, Role role, Rank rank, int xp, int coins,
                            Instant createdAt) {
    }

    public record ActionLog(Long id, String adminEmail, String action, String targetType, Long targetId,
                            String details, Instant createdAt) {
    }

    public record Stats(long users, long dirty, long cleaning, long clean) {
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        return new Stats(users.count(), spots.countByStatus(SpotStatus.DIRTY),
                spots.countByStatus(SpotStatus.CLEANING), spots.countByStatus(SpotStatus.CLEAN));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSpot> spots(int page) {
        return PageResponse.of(spots.findAllByOrderByCreatedAtDesc(PageRequest.of(page, PAGE_SIZE)),
                s -> new AdminSpot(s.getId(), s.getTitle(), s.getStatus(), s.getVisibility(), s.isClosed(),
                        s.getOrganizer().getEmail(), s.getParticipantCount(), s.getCreatedAt()));
    }

    @Transactional
    public void setSpotClosed(Long adminId, Long spotId, boolean closed, String reason) {
        Spot spot = spots.findById(spotId).orElseThrow(() -> ApiException.notFound("Spot"));
        spot.setClosed(closed);
        log(adminId, closed ? "CLOSE_SPOT" : "REOPEN_SPOT", "SPOT", spotId, reasonOr(reason, spot.getTitle()));
    }

    @Transactional
    public void deleteSpot(Long adminId, Long spotId, String reason) {
        Spot spot = spots.findById(spotId).orElseThrow(() -> ApiException.notFound("Spot"));
        log(adminId, "DELETE_SPOT", "SPOT", spotId, reasonOr(reason, spot.getTitle()));
        spots.delete(spot);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUser> users(String query, int page) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        var result = query == null || query.isBlank()
                ? users.findAll(pageable)
                : users.findByEmailContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query, pageable);
        return PageResponse.of(result, u -> new AdminUser(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(),
                u.rank(), u.getXp(), u.getCoins(), u.getCreatedAt()));
    }

    @Transactional
    public void setRole(Long adminId, Long userId, Role role) {
        if (adminId.equals(userId) && role != Role.ADMIN) {
            throw ApiException.conflict("SELF_DEMOTION", "You can't remove your own admin rights");
        }
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        user.setRole(role);
        log(adminId, role == Role.ADMIN ? "GRANT_ADMIN" : "REVOKE_ADMIN", "USER", userId, user.getEmail());
    }

    @Transactional(readOnly = true)
    public PageResponse<ActionLog> log(int page) {
        return PageResponse.of(actions.findAllByOrderByCreatedAtDesc(PageRequest.of(page, PAGE_SIZE)),
                a -> new ActionLog(a.getId(), a.getAdminEmail(), a.getAction(), a.getTargetType(), a.getTargetId(),
                        a.getDetails(), a.getCreatedAt()));
    }

    private void log(Long adminId, String action, String targetType, Long targetId, String details) {
        User admin = users.findById(adminId).orElseThrow(() -> ApiException.notFound("User"));
        actions.save(new AdminAction(adminId, admin.getEmail(), action, targetType, targetId,
                details.length() > 300 ? details.substring(0, 300) : details));
    }

    private static String reasonOr(String reason, String fallback) {
        return reason == null || reason.isBlank() ? fallback : reason.trim();
    }
}
