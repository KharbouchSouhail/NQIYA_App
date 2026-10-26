package ma.nqiya.spot;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import ma.nqiya.security.CurrentUser;
import ma.nqiya.spot.SpotDtos.CheckInRequest;
import ma.nqiya.spot.SpotDtos.CheckInResult;
import ma.nqiya.spot.SpotDtos.CreateSpotRequest;
import ma.nqiya.spot.SpotDtos.FinishResult;
import ma.nqiya.spot.SpotDtos.ParticipantDto;
import ma.nqiya.spot.SpotDtos.ReportDraft;
import ma.nqiya.spot.SpotDtos.SpotDetail;
import ma.nqiya.spot.SpotDtos.SpotSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/spots")
public class SpotController {

    private final SpotService spotService;
    private final CurrentUser currentUser;

    public SpotController(SpotService spotService, CurrentUser currentUser) {
        this.spotService = spotService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<SpotSummary> list(@RequestParam(required = false) Set<SpotStatus> status,
                                  @RequestParam(required = false) Double lat,
                                  @RequestParam(required = false) Double lng,
                                  @RequestParam(required = false) Double radiusKm) {
        return spotService.list(status, lat, lng, radiusKm);
    }

    @GetMapping("/{id}")
    public SpotDetail detail(@PathVariable Long id) {
        return spotService.detail(id, currentUser.idIfPresent().orElse(null));
    }

    /** Step 1 of "Spot a mess": the AI gate on the live BEFORE photo. */
    @PostMapping(path = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReportDraft analyze(@RequestPart("photo") MultipartFile photo) {
        return spotService.analyzeBefore(currentUser.id(), photo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpotDetail create(@Valid @RequestBody CreateSpotRequest request) {
        return spotService.create(currentUser.id(), request);
    }

    @PostMapping("/{id}/join")
    public SpotDetail join(@PathVariable Long id) {
        return spotService.join(id, currentUser.id());
    }

    @DeleteMapping("/{id}/join")
    public SpotDetail leave(@PathVariable Long id) {
        return spotService.leave(id, currentUser.id());
    }

    @GetMapping("/{id}/participants")
    public List<ParticipantDto> participants(@PathVariable Long id) {
        return spotService.participants(id, currentUser.id());
    }

    @PostMapping("/{id}/check-in")
    public CheckInResult checkIn(@PathVariable Long id, @Valid @RequestBody CheckInRequest request) {
        return spotService.checkIn(id, currentUser.id(), request.ticketCode());
    }

    @PostMapping("/{id}/start")
    public SpotDetail start(@PathVariable Long id) {
        return spotService.start(id, currentUser.id());
    }

    @PostMapping(path = "/{id}/finish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FinishResult finish(@PathVariable Long id, @RequestPart("photo") MultipartFile photo) {
        return spotService.finish(id, currentUser.id(), photo);
    }
}
