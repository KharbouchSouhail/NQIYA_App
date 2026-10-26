package ma.nqiya.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;

import ma.nqiya.auth.AuthDtos.AuthResponse;
import ma.nqiya.auth.AuthDtos.RequestCodeResponse;
import ma.nqiya.common.exception.ApiException;
import ma.nqiya.config.AppProperties;
import ma.nqiya.security.JwtService;
import ma.nqiya.security.JwtService.IssuedToken;
import ma.nqiya.user.Role;
import ma.nqiya.user.User;
import ma.nqiya.user.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Passwordless login: email a 6-digit code, exchange it for a JWT. */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final LoginCodeRepository codes;
    private final UserService userService;
    private final MailService mailService;
    private final JwtService jwtService;
    private final AppProperties props;

    public AuthService(LoginCodeRepository codes, UserService userService, MailService mailService,
                       JwtService jwtService, AppProperties props) {
        this.codes = codes;
        this.userService = userService;
        this.mailService = mailService;
        this.jwtService = jwtService;
        this.props = props;
    }

    @Transactional
    public RequestCodeResponse requestCode(String rawEmail) {
        String email = normalize(rawEmail);
        AppProperties.Auth auth = props.auth();
        Instant windowStart = Instant.now().minus(auth.codeWindow());
        if (codes.countByEmailAndCreatedAtAfter(email, windowStart) >= auth.maxCodesPerWindow()) {
            throw ApiException.tooManyRequests("Too many codes requested, please wait a few minutes");
        }
        codes.consumeAllFor(email);

        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        codes.save(new LoginCode(email, hash(email, code), Instant.now().plus(auth.codeTtl())));
        boolean sent = mailService.sendLoginCode(email, code);
        return new RequestCodeResponse(sent, auth.devMode() ? code : null);
    }

    /**
     * Failed attempts are committed even though the method then throws, so brute-forcing is capped.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse verify(String rawEmail, String code) {
        String email = normalize(rawEmail);
        LoginCode loginCode = codes.findFirstByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(AuthService::invalidCode);
        if (!loginCode.isUsable(Instant.now(), props.auth().maxAttempts())) {
            throw invalidCode();
        }
        loginCode.registerAttempt();
        boolean matches = MessageDigest.isEqual(
                loginCode.getCodeHash().getBytes(StandardCharsets.US_ASCII),
                hash(email, code).getBytes(StandardCharsets.US_ASCII));
        if (!matches) {
            throw invalidCode();
        }
        loginCode.consume();

        boolean newUser = userService.findByEmail(email).isEmpty();
        User user = userService.findOrCreate(email);
        if (props.adminEmails().stream().anyMatch(email::equalsIgnoreCase) && !user.isAdmin()) {
            user.setRole(Role.ADMIN);
        }
        IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), newUser, userService.toMe(user));
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgeOldCodes() {
        codes.deleteOlderThan(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String email, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((email + ':' + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException invalidCode() {
        return ApiException.unauthorized("INVALID_CODE", "This code is wrong or expired");
    }
}
