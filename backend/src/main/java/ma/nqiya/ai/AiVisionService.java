package ma.nqiya.ai;

/**
 * Photo understanding used as a trust gate. Verdicts are always computed on the server;
 * anything the client claims about a photo is ignored.
 */
public interface AiVisionService {

    /** Checks that a BEFORE photo really shows a mess, and drafts a title/description for it. */
    BeforeAnalysis analyzeBefore(byte[] image, String mimeType);

    /** Checks that the AFTER photo shows the same place as the BEFORE photo and that it is now clean. */
    AfterVerdict verifyAfter(byte[] before, String beforeMime, byte[] after, String afterMime);

    record BeforeAnalysis(boolean isMess, String title, String description, String dirtLevel, String wasteType,
                          String reason) {
    }

    record AfterVerdict(boolean samePlace, boolean clean, double confidence, String reason) {

        public boolean passed() {
            return samePlace && clean;
        }
    }
}
