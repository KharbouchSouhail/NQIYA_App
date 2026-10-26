package ma.nqiya.spot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ma.nqiya.ai.AiVisionService;
import ma.nqiya.ai.AiVisionService.AfterVerdict;
import ma.nqiya.ai.AiVisionService.BeforeAnalysis;
import ma.nqiya.common.GeoUtils;
import ma.nqiya.common.exception.ApiException;
import ma.nqiya.photo.Photo;
import ma.nqiya.photo.PhotoService;
import ma.nqiya.photo.PhotoService.UploadedImage;
import ma.nqiya.reward.RewardDtos.RewardSummary;
import ma.nqiya.reward.RewardService;
import ma.nqiya.spot.SpotDtos.CheckInResult;
import ma.nqiya.spot.SpotDtos.CreateSpotRequest;
import ma.nqiya.spot.SpotDtos.FinishResult;
import ma.nqiya.spot.SpotDtos.MyCleanup;
import ma.nqiya.spot.SpotDtos.ParticipantDto;
import ma.nqiya.spot.SpotDtos.ReportDraft;
import ma.nqiya.spot.SpotDtos.SpotDetail;
import ma.nqiya.spot.SpotDtos.SpotSummary;
import ma.nqiya.spot.SpotDtos.ViewerState;
import ma.nqiya.user.User;
import ma.nqiya.user.UserDtos.PublicUser;
import ma.nqiya.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spot lifecycle: report (AI gate) → publish → join → QR check-in → finish (AI before/after) → payout.
 * Slow AI calls are made outside database transactions.
 */
@Service
public class SpotService {

    private static final int MAX_RADIUS_KM = 10;
    private static final int MAP_LIMIT = 300;
    private static final Duration MAX_MEETING_AHEAD = Duration.ofDays(30);

    private final SpotRepository spots;
    private final ParticipationRepository participations;
    private final UserRepository users;
    private final PhotoService photoService;
    private final AiVisionService ai;
    private final RewardService rewardService;
    private final TransactionTemplate tx;

    public SpotService(SpotRepository spots, ParticipationRepository participations, UserRepository users,
                       PhotoService photoService, AiVisionService ai, RewardService rewardService,
                       TransactionTemplate tx) {
        this.spots = spots;
        this.participations = participations;
        this.users = users;
        this.photoService = photoService;
        this.ai = ai;
        this.rewardService = rewardService;
        this.tx = tx;
    }

    // ---------------------------------------------------------------- map

    @Transactional(readOnly = true)
    public List<SpotSummary> list(Set<SpotStatus> statuses, Double lat, Double lng, Double radiusKm) {
        Set<SpotStatus> wanted = statuses == null || statuses.isEmpty() ? EnumSet.allOf(SpotStatus.class) : statuses;
        if (lat == null || lng == null) {
            return spots.findOpenByStatus(wanted, PageRequest.of(0, MAP_LIMIT)).stream()
                    .map(s -> toSummary(s, null)).toList();
        }
        double radius = Math.clamp(radiusKm == null ? MAX_RADIUS_KM : radiusKm, 1, MAX_RADIUS_KM);
        double dLat = GeoUtils.latDelta(radius);
        double dLng = GeoUtils.lngDelta(radius, lat);
        return spots.findInBox(wanted, lat - dLat, lat + dLat, lng - dLng, lng + dLng).stream()
                .map(s -> toSummary(s, GeoUtils.distanceKm(lat, lng, s.getLatitude(), s.getLongitude())))
                .filter(s -> s.distanceKm() <= radius)
                .sorted(Comparator.comparingDouble(SpotSummary::distanceKm))
                .limit(MAP_LIMIT)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpotDetail detail(Long spotId, Long viewerId) {
        Spot spot = getOpenOrOwned(spotId, viewerId);
        return toDetail(spot, viewerId);
    }

    // ---------------------------------------------------------------- report

    /** Runs the AI gate on a live BEFORE photo. Accepted photos are stored so publishing can reference them. */
    public ReportDraft analyzeBefore(Long userId, MultipartFile file) {
        UploadedImage image = photoService.read(file);
        BeforeAnalysis analysis = ai.analyzeBefore(image.data(), image.contentType());
        if (!analysis.isMess()) {
            return new ReportDraft(null, false, null, null, null, null,
                    blankTo(analysis.reason(), "This photo doesn't look like a mess to clean."));
        }
        Photo photo = new Photo(userId, Photo.Kind.BEFORE, image.contentType(), image.data());
        photo.recordVerdict(true, analysis.title(), analysis.description(),
                normalizeLevel(analysis.dirtLevel()), analysis.wasteType());
        photoService.save(photo);
        return new ReportDraft(photo.getId(), true, analysis.title(), analysis.description(),
                photo.getDirtLevel(), analysis.wasteType(), analysis.reason());
    }

    @Transactional
    public SpotDetail create(Long userId, CreateSpotRequest req) {
        User organizer = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
        Photo photo = photoService.get(req.photoId());
        if (!photo.getOwnerId().equals(userId) || photo.getKind() != Photo.Kind.BEFORE) {
            throw ApiException.forbidden("This photo can't be used for a report");
        }
        if (!photo.isAiAccepted() || photo.isUsed()) {
            throw ApiException.badRequest("PHOTO_NOT_VERIFIED", "Take a new photo: this one wasn't verified or was already used");
        }

        Instant meetingAt = null;
        if (req.visibility() == Visibility.PUBLIC) {
            meetingAt = req.meetingAt();
            Instant now = Instant.now();
            if (meetingAt == null || meetingAt.isBefore(now.minus(Duration.ofMinutes(5)))
                    || meetingAt.isAfter(now.plus(MAX_MEETING_AHEAD))) {
                throw ApiException.badRequest("MEETING_TIME", "Pick a meeting time within the next 30 days");
            }
        }

        photo.markUsed();
        Spot spot = spots.save(new Spot(req.title().trim(), trimToNull(req.description()), req.latitude(),
                req.longitude(), req.visibility(), req.visibility() == Visibility.PRIVATE && req.anonymous(),
                meetingAt, organizer, photo.getId(), photo.getDirtLevel(), photo.getWasteType()));
        participations.save(new Participation(spot, organizer));
        spot.incrementParticipants();
        return toDetail(spot, userId);
    }

    // ---------------------------------------------------------------- join / check-in

    @Transactional
    public SpotDetail join(Long spotId, Long userId) {
        Spot spot = getOpen(spotId);
        if (!spot.isJoinable()) {
            throw ApiException.conflict("NOT_JOINABLE", "This cleanup can't be joined");
        }
        if (!participations.existsBySpotIdAndUserId(spotId, userId)) {
            User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
            participations.save(new Participation(spot, user));
            spot.incrementParticipants();
        }
        return toDetail(spot, userId);
    }

    @Transactional
    public SpotDetail leave(Long spotId, Long userId) {
        Spot spot = getOpen(spotId);
        Participation p = participations.findBySpotIdAndUserId(spotId, userId)
                .orElseThrow(() -> ApiException.notFound("Participation"));
        if (spot.isOrganizer(userId)) {
            throw ApiException.conflict("ORGANIZER_CANNOT_LEAVE", "The organizer can't leave their own cleanup");
        }
        if (p.isCheckedIn()) {
            throw ApiException.conflict("ALREADY_CHECKED_IN", "You're already checked in on site");
        }
        participations.delete(p);
        spot.decrementParticipants();
        return toDetail(spot, userId);
    }

    /** The organizer scans a volunteer's personal QR ticket (or types its code). */
    @Transactional
    public CheckInResult checkIn(Long spotId, Long organizerId, String rawTicketCode) {
        Spot spot = getOpen(spotId);
        requireOrganizer(spot, organizerId);
        String code = rawTicketCode.trim().toUpperCase(Locale.ROOT);
        User volunteer = users.findByTicketCode(code)
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_TICKET", "No volunteer has this ticket code"));

        Participation participation = participations.findBySpotIdAndUserId(spotId, volunteer.getId())
                .orElseGet(() -> {
                    if (!spot.isJoinable()) {
                        throw ApiException.conflict("NOT_JOINABLE", "This is a solo cleanup");
                    }
                    spot.incrementParticipants();
                    return participations.save(new Participation(spot, volunteer));
                });
        boolean already = participation.isCheckedIn();
        participation.checkIn();
        checkInOrganizer(spot);
        spot.startCleaning();
        return new CheckInResult(PublicUser.of(volunteer), participation.getCheckedInAt(), already,
                spot.getStatus(), participations.countBySpotIdAndCheckedInAtIsNotNull(spotId));
    }

    /** The organizer starts the cleanup on site (the only way to start a solo cleanup). */
    @Transactional
    public SpotDetail start(Long spotId, Long organizerId) {
        Spot spot = getOpen(spotId);
        requireOrganizer(spot, organizerId);
        checkInOrganizer(spot);
        spot.startCleaning();
        return toDetail(spot, organizerId);
    }

    @Transactional(readOnly = true)
    public List<ParticipantDto> participants(Long spotId, Long viewerId) {
        Spot spot = getOpenOrOwned(spotId, viewerId);
        if (spot.getVisibility() == Visibility.PRIVATE && !spot.isOrganizer(viewerId)) {
            return List.of();
        }
        return participations.findBySpotIdOrderByJoinedAtAsc(spotId).stream()
                .map(p -> new ParticipantDto(PublicUser.of(p.getUser()), p.getJoinedAt(), p.getCheckedInAt(),
                        spot.isOrganizer(p.getUser().getId())))
                .toList();
    }

    // ---------------------------------------------------------------- finish

    /**
     * Organizer submits the AFTER photo. The AI compares it with the BEFORE photo; on pass every checked-in
     * participant is paid in one transaction. On fail nothing changes and the organizer can retry.
     */
    public FinishResult finish(Long spotId, Long organizerId, MultipartFile file) {
        UploadedImage after = photoService.read(file);
        Photo before = tx.execute(status -> {
            Spot spot = getOpen(spotId);
            requireOrganizer(spot, organizerId);
            if (spot.getStatus() != SpotStatus.CLEANING) {
                throw ApiException.conflict("NOT_CLEANING", "Start the cleanup and check volunteers in first");
            }
            Photo p = photoService.get(spot.getBeforePhotoId());
            p.getData(); // initialise the lazy bytes inside the transaction
            return p;
        });

        AfterVerdict verdict = ai.verifyAfter(before.getData(), before.getContentType(), after.data(),
                after.contentType());
        if (!verdict.passed()) {
            String reason = !verdict.samePlace()
                    ? "This doesn't look like the same place. " + verdict.reason()
                    : "It still looks dirty. " + verdict.reason();
            return new FinishResult(false, reason.trim(), detail(spotId, organizerId), null, 0);
        }

        return tx.execute(status -> {
            Spot spot = getOpen(spotId);
            Photo afterPhoto = new Photo(organizerId, Photo.Kind.AFTER, after.contentType(), after.data());
            afterPhoto.recordVerdict(true, null, verdict.reason(), null, null);
            afterPhoto.markUsed();
            photoService.save(afterPhoto);
            spot.markCleaned(afterPhoto.getId());

            List<RewardSummary> payouts = new ArrayList<>();
            for (Participation p : participations.findCheckedIn(spotId)) {
                payouts.add(rewardService.payCleanup(p.getUser(), spotId, spot.getTitle(),
                        spot.isOrganizer(p.getUser().getId())));
            }
            RewardSummary mine = payouts.stream().filter(r -> r.userId().equals(organizerId)).findFirst().orElse(null);
            return new FinishResult(true, "Saha! Spot cleaned. " + verdict.reason(), toDetail(spot, organizerId),
                    mine, payouts.size());
        });
    }

    @Transactional(readOnly = true)
    public List<MyCleanup> myCleanups(Long userId) {
        return participations.findByUser(userId).stream()
                .map(p -> {
                    Spot s = p.getSpot();
                    return new MyCleanup(s.getId(), s.getTitle(), s.getStatus(), s.isOrganizer(userId),
                            p.isCheckedIn(), p.getJoinedAt(), s.getMeetingAt(), SpotDtos.photoUrl(s.getBeforePhotoId()));
                })
                .toList();
    }

    // ---------------------------------------------------------------- helpers

    private void checkInOrganizer(Spot spot) {
        participations.findBySpotIdAndUserId(spot.getId(), spot.getOrganizer().getId())
                .ifPresent(Participation::checkIn);
    }

    private Spot getOpen(Long spotId) {
        Spot spot = spots.findById(spotId).orElseThrow(() -> ApiException.notFound("Spot"));
        if (spot.isClosed()) {
            throw ApiException.notFound("Spot");
        }
        return spot;
    }

    /** Closed spots stay visible to their organizer so they understand what happened. */
    private Spot getOpenOrOwned(Long spotId, Long viewerId) {
        Spot spot = spots.findById(spotId).orElseThrow(() -> ApiException.notFound("Spot"));
        if (spot.isClosed() && (viewerId == null || !spot.isOrganizer(viewerId))) {
            throw ApiException.notFound("Spot");
        }
        return spot;
    }

    private static void requireOrganizer(Spot spot, Long userId) {
        if (!spot.isOrganizer(userId)) {
            throw ApiException.forbidden("Only the organizer can do this");
        }
    }

    private SpotSummary toSummary(Spot s, Double distanceKm) {
        return new SpotSummary(s.getId(), s.getTitle(), s.getStatus(), s.getVisibility(), s.getLatitude(),
                s.getLongitude(), s.getParticipantCount(), s.getMeetingAt(), s.getDirtLevel(),
                SpotDtos.photoUrl(s.getBeforePhotoId()),
                distanceKm == null ? null : Math.round(distanceKm * 100) / 100.0);
    }

    SpotDetail toDetail(Spot s, Long viewerId) {
        boolean organizer = viewerId != null && s.isOrganizer(viewerId);
        Participation mine = viewerId == null ? null
                : participations.findBySpotIdAndUserId(s.getId(), viewerId).orElse(null);
        ViewerState viewer = new ViewerState(viewerId != null, organizer, mine != null,
                mine != null && mine.isCheckedIn(), viewerId != null && mine == null && s.isJoinable());
        PublicUser organizerDto = s.isAnonymous() && !organizer ? null : PublicUser.of(s.getOrganizer());
        return new SpotDetail(s.getId(), s.getTitle(), s.getDescription(), s.getStatus(), s.getVisibility(),
                s.isAnonymous(), s.isClosed(), s.getLatitude(), s.getLongitude(), s.getMeetingAt(), organizerDto,
                SpotDtos.photoUrl(s.getBeforePhotoId()), SpotDtos.photoUrl(s.getAfterPhotoId()), s.getDirtLevel(),
                s.getWasteType(), s.getParticipantCount(),
                participations.countBySpotIdAndCheckedInAtIsNotNull(s.getId()),
                s.getCreatedAt(), s.getStartedAt(), s.getCleanedAt(), viewer);
    }

    private static String normalizeLevel(String level) {
        if (level == null) {
            return "MEDIUM";
        }
        String l = level.toUpperCase(Locale.ROOT);
        return Set.of("LOW", "MEDIUM", "HIGH").contains(l) ? l : "MEDIUM";
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String blankTo(String s, String fallback) {
        return s == null || s.isBlank() ? fallback : s;
    }
}
