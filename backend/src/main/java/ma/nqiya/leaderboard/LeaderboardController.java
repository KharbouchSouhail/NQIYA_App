package ma.nqiya.leaderboard;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import ma.nqiya.security.CurrentUser;
import ma.nqiya.user.Rank;
import ma.nqiya.user.User;
import ma.nqiya.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final UserRepository users;
    private final CurrentUser currentUser;

    public LeaderboardController(UserRepository users, CurrentUser currentUser) {
        this.users = users;
        this.currentUser = currentUser;
    }

    public record Entry(int position, Long userId, String displayName, Rank rank, int xp) {
    }

    public record Leaderboard(List<Entry> top, Entry me) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Leaderboard leaderboard(@RequestParam(defaultValue = "50") int limit) {
        AtomicInteger position = new AtomicInteger();
        List<Entry> top = users.findLeaderboard(PageRequest.of(0, Math.clamp(limit, 3, 100))).stream()
                .map(u -> toEntry(position.incrementAndGet(), u))
                .toList();
        Entry me = currentUser.idIfPresent()
                .flatMap(users::findById)
                .map(u -> toEntry((int) users.countWithMoreXp(u.getXp()) + 1, u))
                .orElse(null);
        return new Leaderboard(top, me);
    }

    private static Entry toEntry(int position, User u) {
        return new Entry(position, u.getId(), u.getDisplayName(), u.rank(), u.getXp());
    }
}
