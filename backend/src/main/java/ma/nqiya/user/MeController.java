package ma.nqiya.user;

import java.util.List;

import jakarta.validation.Valid;
import ma.nqiya.reward.RewardDtos.HistoryItem;
import ma.nqiya.reward.RewardService;
import ma.nqiya.security.CurrentUser;
import ma.nqiya.spot.SpotDtos.MyCleanup;
import ma.nqiya.spot.SpotService;
import ma.nqiya.user.UserDtos.MeResponse;
import ma.nqiya.user.UserDtos.UpdateProfileRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final RewardService rewardService;
    private final SpotService spotService;
    private final CurrentUser currentUser;

    public MeController(UserService userService, RewardService rewardService, SpotService spotService,
                        CurrentUser currentUser) {
        this.userService = userService;
        this.rewardService = rewardService;
        this.spotService = spotService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public MeResponse me() {
        return userService.me(currentUser.id());
    }

    @PatchMapping
    public MeResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser.id(), request.displayName());
    }

    @GetMapping("/history")
    public List<HistoryItem> history() {
        return rewardService.history(currentUser.id());
    }

    @GetMapping("/cleanups")
    public List<MyCleanup> cleanups() {
        return spotService.myCleanups(currentUser.id());
    }
}
