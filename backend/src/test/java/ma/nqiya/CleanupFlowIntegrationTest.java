package ma.nqiya;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * End-to-end happy path on a real PostgreSQL: login → report → publish → join → QR check-in → finish → payout → shop.
 * Uses the stub AI checker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.ai.provider=stub", "app.auth.dev-mode=true", "app.admin-emails=admin@nqiya.ma"})
class CleanupFlowIntegrationTest {

    private static final EmbeddedPostgres PG;

    static {
        try {
            PG = EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @AfterAll
    static void stop() throws IOException {
        PG.close();
    }

    @Value("${local.server.port}")
    int port;

    RestClient http;

    @BeforeEach
    void setUp() {
        http = RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void fullCleanupLoop() {
        String organizer = login("organizer@nqiya.ma");
        String volunteer = login("volunteer@nqiya.ma");

        // 1. Spot a mess: AI gate on the BEFORE photo
        Map<String, Object> draft = upload("/api/spots/analyze", organizer, new byte[]{1, 2, 3});
        assertThat(draft.get("accepted")).isEqualTo(true);

        // 2. Publish a public cleanup
        Map<String, Object> spot = http.post().uri("/api/spots").header("Authorization", "Bearer " + organizer)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("photoId", draft.get("photoId"), "title", "Plastic on the beach",
                        "latitude", 33.7166, "longitude", -7.3542, "accuracyMeters", 12,
                        "visibility", "PUBLIC", "anonymous", false,
                        "meetingAt", Instant.now().plus(1, ChronoUnit.DAYS).toString()))
                .retrieve().body(MAP);
        Long spotId = ((Number) spot.get("id")).longValue();
        assertThat(spot.get("status")).isEqualTo("DIRTY");

        // The map shows it to anonymous visitors within 10 km
        List<Map<String, Object>> nearby = http.get()
                .uri("/api/spots?lat=33.72&lng=-7.35&radiusKm=5&status=DIRTY").retrieve().body(LIST);
        assertThat(nearby).extracting(m -> ((Number) m.get("id")).longValue()).contains(spotId);

        // 3. Volunteer joins, organizer scans the volunteer's QR ticket
        post("/api/spots/" + spotId + "/join", volunteer, null);
        String ticket = (String) get("/api/me", volunteer).get("ticketCode");
        Map<String, Object> checkIn = post("/api/spots/" + spotId + "/check-in", organizer, Map.of("ticketCode", ticket));
        assertThat(checkIn.get("spotStatus")).isEqualTo("CLEANING");
        assertThat(((Number) checkIn.get("checkedInCount")).intValue()).isEqualTo(2);

        // Only the organizer may finish
        assertThatThrownBy(() -> upload("/api/spots/" + spotId + "/finish", volunteer, new byte[]{9, 9}))
                .isInstanceOf(HttpClientErrorException.Forbidden.class);

        // 4. Finish with the AFTER photo -> everyone checked in is paid
        Map<String, Object> finish = upload("/api/spots/" + spotId + "/finish", organizer, new byte[]{4, 5, 6});
        assertThat(finish.get("passed")).isEqualTo(true);
        assertThat(finish.get("paidVolunteers")).isEqualTo(2);

        Map<String, Object> organizerMe = get("/api/me", organizer);
        assertThat(organizerMe.get("xp")).isEqualTo(150);
        // 150 coins + 100 coins Silver level-up gift
        assertThat(organizerMe.get("coins")).isEqualTo(250);
        Map<String, Object> volunteerMe = get("/api/me", volunteer);
        assertThat(volunteerMe.get("xp")).isEqualTo(100);
        assertThat(volunteerMe.get("completedCleanups")).isEqualTo(1);

        // 5. Shop: buy an 80-coin voucher and redeem it
        List<Map<String, Object>> vouchers = http.get().uri("/api/shop/vouchers").retrieve().body(LIST);
        Object cheapest = vouchers.getFirst().get("id");
        Map<String, Object> purchase = post("/api/shop/vouchers/" + cheapest + "/buy", volunteer, null);
        assertThat(purchase.get("coinsLeft")).isEqualTo(200 - 80);
        Map<?, ?> item = (Map<?, ?>) purchase.get("item");
        Map<String, Object> redeemed = post("/api/wallet/" + item.get("id") + "/redeem", volunteer, null);
        assertThat(redeemed.get("status")).isEqualTo("USED");

        // Leaderboard puts the organizer first
        Map<String, Object> board = http.get().uri("/api/leaderboard").retrieve().body(MAP);
        List<?> top = (List<?>) board.get("top");
        assertThat(((Map<?, ?>) top.getFirst()).get("displayName")).isEqualTo("organizer");
    }

    @Test
    void wrongLoginCodeIsRejected() {
        http.post().uri("/api/auth/request-code").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", "someone@nqiya.ma")).retrieve().body(MAP);
        assertThatThrownBy(() -> http.post().uri("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", "someone@nqiya.ma", "code", "000000")).retrieve().body(MAP))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void adminEndpointsRequireAdminRole() {
        String user = login("plain@nqiya.ma");
        assertThatThrownBy(() -> get("/api/admin/stats", user)).isInstanceOf(HttpClientErrorException.Forbidden.class);
        String admin = login("admin@nqiya.ma");
        assertThat(get("/api/admin/stats", admin)).containsKey("users");
    }

    // ------------------------------------------------------------------ helpers

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {
    };

    private String login(String email) {
        Map<String, Object> sent = http.post().uri("/api/auth/request-code").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email)).retrieve().body(MAP);
        Map<String, Object> auth = http.post().uri("/api/auth/verify").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "code", sent.get("devCode"))).retrieve().body(MAP);
        return (String) auth.get("token");
    }

    private Map<String, Object> get(String uri, String token) {
        return http.get().uri(uri).header("Authorization", "Bearer " + token).retrieve().body(MAP);
    }

    private Map<String, Object> post(String uri, String token, Object body) {
        var req = http.post().uri(uri).header("Authorization", "Bearer " + token);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return req.retrieve().body(MAP);
    }

    private Map<String, Object> upload(String uri, String token, byte[] bytes) {
        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("photo", new org.springframework.http.HttpEntity<>(new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return "photo.jpg";
            }
        }, jpegHeaders()));
        return http.post().uri(uri).header("Authorization", "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA).body(parts).retrieve().body(MAP);
    }

    private static org.springframework.http.HttpHeaders jpegHeaders() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return headers;
    }
}
