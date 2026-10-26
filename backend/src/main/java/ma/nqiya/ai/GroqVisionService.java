package ma.nqiya.ai;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import ma.nqiya.common.exception.ApiException;
import ma.nqiya.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Groq via its OpenAI-compatible {@code /chat/completions} endpoint, using a vision model in JSON mode.
 * Images are sent inline as base64 data URIs. Models are tried in order so a rate limit on one falls through.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "groq")
public class GroqVisionService implements AiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GroqVisionService.class);

    private final RestClient client;
    private final List<String> models;
    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    public GroqVisionService(AppProperties props) {
        this(props, buildClient(props.ai().groq()));
    }

    /** Visible for tests, which pass a client bound to a mock server. */
    GroqVisionService(AppProperties props, RestClient client) {
        this.client = client;
        this.models = props.ai().groq().models();
    }

    private static RestClient buildClient(AppProperties.Groq config) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new IllegalStateException("app.ai.groq.api-key (GROQ_API_KEY) is required when app.ai.provider=groq");
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(config.timeout()).build());
        factory.setReadTimeout(config.timeout());
        return RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .build();
    }

    @Override
    public BeforeAnalysis analyzeBefore(byte[] image, String mimeType) {
        JsonNode r = complete(List.of(
                text(VisionPrompts.BEFORE + "\n" + VisionPrompts.BEFORE_JSON_SHAPE),
                image(image, mimeType)));
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
        JsonNode r = complete(List.of(
                text(VisionPrompts.AFTER + "\n" + VisionPrompts.AFTER_JSON_SHAPE),
                text("BEFORE:"), image(before, beforeMime),
                text("AFTER:"), image(after, afterMime)));
        return new AfterVerdict(
                r.path("samePlace").asBoolean(false),
                r.path("clean").asBoolean(false),
                r.path("confidence").asDouble(0),
                r.path("reason").asString(""));
    }

    private JsonNode complete(List<Map<String, Object>> content) {
        for (String model : models) {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.2,
                    "max_completion_tokens", 512,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(Map.of("role", "user", "content", new ArrayList<>(content))));
            try {
                JsonNode response = client.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                String text = response == null ? null
                        : response.path("choices").path(0).path("message").path("content").asString(null);
                if (text == null || text.isBlank()) {
                    log.warn("Groq model {} returned no content, trying next", model);
                    continue;
                }
                return json.readTree(text);
            } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
                // same key for every model: no point trying the others
                log.error("Groq rejected the API key ({}). Check GROQ_API_KEY at https://console.groq.com/keys",
                        e.getStatusCode());
                break;
            } catch (RestClientException | tools.jackson.core.JacksonException e) {
                log.warn("Groq model {} failed ({}), trying next", model, e.getMessage());
            }
        }
        throw ApiException.unavailable("AI_UNAVAILABLE", "The photo check is busy right now, please try again in a minute");
    }

    private static Map<String, Object> text(String text) {
        return Map.of("type", "text", "text", text);
    }

    private static Map<String, Object> image(byte[] data, String mimeType) {
        String dataUri = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(data);
        return Map.of("type", "image_url", "image_url", Map.of("url", dataUri));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
