package ma.nqiya.user;

import java.security.SecureRandom;
import java.util.Optional;

import ma.nqiya.common.exception.ApiException;
import ma.nqiya.spot.ParticipationRepository;
import ma.nqiya.user.UserDtos.MeResponse;
import ma.nqiya.user.UserDtos.RankProgress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    /** No 0/O/1/I so codes typed by hand are unambiguous. */
    private static final String TICKET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final ParticipationRepository participations;

    public UserService(UserRepository users, ParticipationRepository participations) {
        this.users = users;
        this.participations = participations;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return users.findByEmailIgnoreCase(email);
    }

    @Transactional
    public User findOrCreate(String email) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> {
            String local = email.substring(0, email.indexOf('@'));
            String name = local.replaceAll("[^\\p{L}\\p{N}._-]", "");
            if (name.length() < 2) {
                name = "Cleaner";
            }
            return users.save(new User(email, name.length() > 40 ? name.substring(0, 40) : name, newTicketCode()));
        });
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long id) {
        return toMe(get(id));
    }

    @Transactional
    public MeResponse updateProfile(Long id, String displayName) {
        User user = get(id);
        user.setDisplayName(displayName.trim());
        return toMe(user);
    }

    public MeResponse toMe(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(),
                user.getXp(), user.getCoins(), RankProgress.of(user.getXp()), user.getTicketCode(),
                user.getId() == null ? 0 : participations.countCompletedCleanups(user.getId()),
                user.getCreatedAt());
    }

    private String newTicketCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(TICKET_ALPHABET.charAt(RANDOM.nextInt(TICKET_ALPHABET.length())));
            }
            code = sb.toString();
        } while (users.existsByTicketCode(code));
        return code;
    }
}
