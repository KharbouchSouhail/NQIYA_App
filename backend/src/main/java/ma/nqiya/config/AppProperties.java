package ma.nqiya.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed application settings, bound from the {@code app.*} namespace.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Auth auth,
        Mail mail,
        Ai ai,
        Cors cors,
        Uploads uploads,
        List<String> adminEmails) {

    public AppProperties {
        adminEmails = adminEmails == null ? List.of() : adminEmails;
    }

    public record Jwt(String secret, Duration ttl, String issuer) {
    }

    /**
     * @param devMode when true the login code is logged and echoed in the API response (never enable in production)
     */
    public record Auth(boolean devMode, Duration codeTtl, int maxAttempts, int maxCodesPerWindow, Duration codeWindow) {
    }

    public record Mail(String from) {
    }

    /**
     * @param provider {@code groq} or {@code gemini} for a real model, {@code stub} for an offline checker used in
     *                 development
     */
    public record Ai(String provider, Groq groq, Gemini gemini) {
    }

    public record Groq(String apiKey, String baseUrl, List<String> models, Duration timeout) {
    }

    public record Gemini(String apiKey, String baseUrl, List<String> models, Duration timeout) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Uploads(long maxBytes) {
    }
}
