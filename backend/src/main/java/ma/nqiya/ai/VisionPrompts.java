package ma.nqiya.ai;

/** Prompts shared by every AI provider so they all judge photos the same way. */
final class VisionPrompts {

    private VisionPrompts() {
    }

    static final String BEFORE = """
            You are the photo gate of NQIYA, a Moroccan civic app where citizens report litter and informal
            dumping in public spaces so volunteers can clean them together.
            Decide whether this photo shows a real mess (litter, garbage, rubble, illegal dumping) in a public or
            shared outdoor space that a small group of volunteers could clean.
            Reject (isMess=false) selfies, screenshots, photos of screens, stock-looking images, clean places,
            indoor private rooms, or photos where no waste is visible.
            If it is a mess, write a short concrete English title (max 60 chars, e.g. "Plastic bottles along
            the beach path") and a helpful description (max 200 chars) of what volunteers will find and should
            bring. Rate dirtLevel as LOW, MEDIUM or HIGH and name the main wasteType (e.g. Plastic, Household
            waste, Rubble, Green waste, Mixed). Give a one-sentence reason for your verdict.
            """;

    static final String AFTER = """
            You verify cleanups for NQIYA. The first image is the BEFORE photo of a dirty spot, the second image
            is the AFTER photo submitted by the organizer once volunteers finished.
            samePlace: true only if both photos clearly show the same location (same landmarks, walls, ground,
            vegetation, street furniture) even if the angle or light differ slightly.
            clean: true only if the waste visible in BEFORE has been removed in AFTER (small residue is fine).
            confidence: your confidence between 0 and 1. reason: one short sentence for the volunteers.
            """;

    /** JSON-mode providers have no schema parameter, so the expected shape is spelled out in the prompt. */
    static final String BEFORE_JSON_SHAPE = """
            Answer with a single JSON object only, exactly in this shape:
            {"isMess": boolean, "title": string, "description": string, "dirtLevel": "LOW"|"MEDIUM"|"HIGH",
             "wasteType": string, "reason": string}
            """;

    static final String AFTER_JSON_SHAPE = """
            Answer with a single JSON object only, exactly in this shape:
            {"samePlace": boolean, "clean": boolean, "confidence": number, "reason": string}
            """;
}
