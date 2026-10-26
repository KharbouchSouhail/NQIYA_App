package ma.nqiya.security;

import java.util.Optional;

import ma.nqiya.common.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** Resolves the id of the authenticated user from the JWT subject. */
@Component
public class CurrentUser {

    public Long id() {
        return idIfPresent().orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Please log in"));
    }

    public Optional<Long> idIfPresent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(Long.valueOf(jwt.getSubject()));
        }
        return Optional.empty();
    }
}
