package ma.nqiya.ai;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Offline stand-in for local development and tests: accepts every BEFORE photo and passes any AFTER photo
 * that is not a byte-for-byte copy of the BEFORE. Never use in production.
 */
@Service
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubVisionService implements AiVisionService {

    private static final Logger log = LoggerFactory.getLogger(StubVisionService.class);

    public StubVisionService() {
        log.warn("Using the STUB AI photo checker - set app.ai.provider=gemini for real verification");
    }

    @Override
    public BeforeAnalysis analyzeBefore(byte[] image, String mimeType) {
        return new BeforeAnalysis(true, "Litter spotted here",
                "Mixed waste reported by a neighbour. Bring gloves and a few bags.",
                "MEDIUM", "Mixed", "Stub checker: accepted without analysis.");
    }

    @Override
    public AfterVerdict verifyAfter(byte[] before, String beforeMime, byte[] after, String afterMime) {
        boolean identical = Arrays.equals(before, after);
        return new AfterVerdict(true, !identical, identical ? 0.99 : 0.5,
                identical ? "The AFTER photo is the same file as the BEFORE photo." : "Stub checker: accepted.");
    }
}
