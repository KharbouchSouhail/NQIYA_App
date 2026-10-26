package ma.nqiya.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import ma.nqiya.ai.AiVisionService.AfterVerdict;
import ma.nqiya.ai.AiVisionService.BeforeAnalysis;
import ma.nqiya.common.exception.ApiException;
import ma.nqiya.config.AppProperties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GroqVisionServiceTest {

    private static final String BASE = "https://api.groq.test/openai/v1";

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private GroqVisionService service(String... models) {
        AppProperties.Groq groq = new AppProperties.Groq("test-key", BASE, List.of(models), Duration.ofSeconds(5));
        AppProperties props = new AppProperties(null, null, null, new AppProperties.Ai("groq", groq, null),
                null, null, List.of());
        return new GroqVisionService(props, builder.build());
    }

    @Test
    void sendsImageAsDataUriInJsonModeAndParsesVerdict() {
        server.expect(requestTo(BASE + "/chat/completions"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("qwen/qwen3.8-27b"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url",
                        Matchers.startsWith("data:image/jpeg;base64,")))
                .andRespond(withSuccess(completion("""
                        {"isMess": true, "title": "Plastic on the beach", "description": "Bring bags",
                         "dirtLevel": "HIGH", "wasteType": "Plastic", "reason": "Lots of bottles"}"""),
                        MediaType.APPLICATION_JSON));

        BeforeAnalysis r = service("qwen/qwen3.8-27b").analyzeBefore(new byte[]{1, 2, 3}, "image/jpeg");

        assertThat(r.isMess()).isTrue();
        assertThat(r.title()).isEqualTo("Plastic on the beach");
        assertThat(r.dirtLevel()).isEqualTo("HIGH");
        server.verify();
    }

    @Test
    void fallsBackToNextModelWhenRateLimited() {
        server.expect(jsonPath("$.model").value("model-a"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(jsonPath("$.model").value("model-b"))
                .andExpect(jsonPath("$.messages[0].content.length()").value(5))
                .andRespond(withSuccess(completion("""
                        {"samePlace": true, "clean": false, "confidence": 0.8, "reason": "Bags still visible"}"""),
                        MediaType.APPLICATION_JSON));

        AfterVerdict v = service("model-a", "model-b").verifyAfter(new byte[]{1}, "image/jpeg", new byte[]{2}, "image/jpeg");

        assertThat(v.samePlace()).isTrue();
        assertThat(v.passed()).isFalse();
        server.verify();
    }

    @Test
    void allModelsFailingIsServiceUnavailable() {
        server.expect(requestTo(BASE + "/chat/completions")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service("only-model").analyzeBefore(new byte[]{1}, "image/png"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo("AI_UNAVAILABLE");
    }

    @Test
    void invalidKeyStopsWithoutTryingOtherModels() {
        server.expect(jsonPath("$.model").value("model-a")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service("model-a", "model-b").analyzeBefore(new byte[]{1}, "image/png"))
                .isInstanceOf(ApiException.class);
        server.verify(); // model-b was never called
    }

    /** Minimal OpenAI-style chat completion wrapping {@code content} as the assistant message. */
    private static String completion(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
        return """
                {"id":"x","object":"chat.completion","choices":[{"index":0,
                 "message":{"role":"assistant","content":"%s"},"finish_reason":"stop"}]}""".formatted(escaped);
    }
}
