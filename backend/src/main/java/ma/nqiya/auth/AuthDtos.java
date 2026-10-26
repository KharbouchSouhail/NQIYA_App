package ma.nqiya.auth;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ma.nqiya.user.UserDtos.MeResponse;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RequestCodeRequest(@NotBlank @Email @Size(max = 254) String email) {
    }

    /** @param devCode only filled when {@code app.auth.dev-mode} is on */
    public record RequestCodeResponse(boolean sent, String devCode) {
    }

    public record VerifyCodeRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits") String code) {
    }

    public record AuthResponse(String token, Instant expiresAt, boolean newUser, MeResponse user) {
    }
}
