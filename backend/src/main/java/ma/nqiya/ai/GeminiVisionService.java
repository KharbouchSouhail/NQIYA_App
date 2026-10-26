package ma.nqiya.ai;

import java.net.http.HttpClient;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import ma.nqiya.common.exception.ApiException;
import ma.nqiya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Gemini via the REST {@code generateContent} endpoint with structured JSON output.
 * Models are tried in order so a quota error on one (free tier is ~20 requests/day/model) falls through to the next.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "gemini")
public class GeminiVisionService implements AiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionService.class);

    private static final Map<String, Object> BEFORE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "isMess", Map.of("type", "BOOLEAN"),
                    "title", Map.of("type", "STRING"),
                    "description", Map.of("type", "STRING"),
                    "dirtLevel", Map.of("type", "STRING", "enum", List.of("LOW", "MEDIUM", "HIGH")),
                    "wasteType", Map.of("type", "STRING"),
                    "reason", Map.of("type", "STRING")),
            "required", List.of("isMess", "title", "description", "dirtLevel", "wasteType", "reason"));

    private static final Map<String, Object> AFTER_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "samePlace", Map.of("type", "BOOLEAN"),
                    "clean", Map.of("type", "BOOLEAN"),
                    "confidence", Map.of("type", "NUMBER"),
                    "reason", Map.of("type", "STRING")),
            "required", List.of("samePlace", "clean", "confidence", "reason"));

    private final RestClient client;
    private final AppProperties.Gemini config;
    private final JsonMapper json = JsonMapper.builder().build();

    public GeminiVisionService(AppProperties props) {
        this.config = props.ai().gemini();
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalStateException("app.ai.gemini.api-key is required when app.ai.provider=gemini");
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(config.timeout()).build());
        factory.setReadTimeout(config.timeout());
        this.client = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .defaultHeader("x-goog-api-key", config.apiKey())
                .build();
    }

    @Override
    public BeforeAnalysis analyzeBefore(byte[] image, String mimeType) {
        JsonNode r = generate(List.of(text(VisionPrompts.BEFORE), inline(image, mimeType)), BEFORE_SCHEMA);
        return new BeforeAnalysis(
                r.path("isMess").asBoolean(false),
                truncate(r.path("title").asString(""), 80),
                truncate(r.path("description").asString(""), 400),
                r.path("dirtLevel").asString("MEDIUM"),
                truncate(r.path("wasteType").asString("Mixed"), 60),
                r.path("reason").asString(""));
    }

    @Override
    public AfterVerdict verifyAfter(byte[] before, String beforeMime, byte[] after, String afterMime) {
        JsonNode r = generate(List.of(text(VisionPrompts.AFTER), text("BEFORE:"), inline(before, beforeMime),
                text("AFTER:"), inline(after, afterMime)), AFTER_SCHEMA);
        return new AfterVerdict(
                r.path("samePlace").asBoolean(false),
                r.path("clean").asBoolean(false),
                r.path("confidence").asDouble(0),
                r.path("reason").asString(""));
    }

    private JsonNode generate(List<Map<String, Object>> parts, Map<String, Object> schema) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseSchema", schema));

        for (String model : config.models()) {
            try {
                JsonNode response = client.post()
                        .uri("/models/{model}:generateContent", model)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                String text = response == null ? null
                        : response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asString(null);
                if (text == null || text.isBlank()) {
                    log.warn("Gemini model {} returned no content, trying next", model);
                    continue;
                }
                return json.readTree(text);
            } catch (RestClientException | tools.jackson.core.JacksonException e) {
                log.warn("Gemini model {} failed ({}), trying next", model, e.getMessage());
            }
        }
        throw ApiException.unavailable("AI_UNAVAILABLE", "The photo check is busy right now, please try again in a minute");
    }

    private static Map<String, Object> text(String text) {
        return Map.of("text", text);
    }

    private static Map<String, Object> inline(byte[] data, String mimeType) {
        return Map.of("inlineData", Map.of("mimeType", mimeType, "data", Base64.getEncoder().encodeToString(data)));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
