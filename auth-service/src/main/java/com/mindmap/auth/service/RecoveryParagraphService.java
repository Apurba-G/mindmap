package com.mindmap.auth.service;

import com.mindmap.auth.security.Argon2PasswordEncoder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * Service responsible for generating human-readable, grammatically structured recovery paragraphs
 * with high entropy (100+ bits), normalising them, and hashing/verifying them with Argon2id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryParagraphService {

    private final Argon2PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private final List<String> fullWordList = new ArrayList<>();

    // Curated grammatical word banks ensuring readability
    private static final String[] ADJECTIVES = {
            "ancient", "amber", "arctic", "autumn", "azure", "bold", "brave", "bright",
            "brisk", "bronze", "calm", "cedar", "clever", "cloudy", "copper", "coral",
            "crisp", "crystal", "daring", "dawn", "distant", "dusty", "eager", "early",
            "earthy", "echoing", "elder", "elegant", "emerald", "faint", "fair", "famous",
            "fearless", "festive", "firm", "floral", "flowing", "forest", "fragrant",
            "frosty", "frozen", "gentle", "gilded", "glacial", "gleaming", "golden", "graceful",
            "grand", "green", "harbor", "hardy", "harmonic", "heavy", "hidden", "hollow",
            "honest", "humble", "indigo", "inner", "ivory", "jade", "jovial", "keen",
            "kindred", "lasting", "light", "lively", "loyal", "lunar", "magic", "mellow",
            "misty", "modern", "modest", "morning", "mountain", "mystic", "narrow", "native",
            "nimble", "noble", "north", "novel", "oak", "ocean", "olive", "onyx",
            "open", "orange", "pacific", "patient", "peaceful", "polar", "proud", "pure",
            "quiet", "radiant", "rapid", "rare", "resilient", "rich", "rising", "robust",
            "rosy", "rough", "round", "royal", "rugged", "rustic", "sacred", "sandy",
            "scarlet", "scenic", "serene", "shaded", "shadow", "sharp", "shining", "silent",
            "silken", "silver", "simple", "sincere", "slender", "smooth", "solar", "solid",
            "spring", "stable", "steady", "stellar", "stone", "stout", "strong", "subtle",
            "summer", "sunny", "swift", "tender", "tidal", "timber", "timeless", "tranquil",
            "true", "twilight", "unbroken", "valiant", "vast", "velvet", "vibrant", "vintage",
            "violet", "vivid", "warm", "watchful", "weathered", "wild", "willow", "winding",
            "winter", "wise", "wooden", "worthy", "yellow", "youthful", "zealous"
    };

    private static final String[] NOUNS = {
            "acorn", "anchor", "anthem", "archer", "archive", "arrow", "artist", "asteroid",
            "aurora", "avenue", "badge", "beacon", "breeze", "bridge", "brook", "cabin",
            "canyon", "castle", "cedar", "chalet", "channel", "circuit", "citadel", "cliff",
            "cloud", "compass", "canyon", "crater", "creek", "crown", "crystal", "current",
            "delta", "desert", "diamond", "diver", "dolphin", "dragon", "drum", "dune",
            "eagle", "echo", "ember", "falcon", "feather", "fjord", "flame", "forest",
            "fossil", "fountain", "fox", "galaxy", "gallery", "garden", "garland", "gateway",
            "geyser", "glacier", "glade", "glen", "globe", "grove", "harbor", "harvest",
            "haven", "hawk", "hearth", "helmet", "hero", "horizon", "hound", "island",
            "jaguar", "journey", "jungle", "keeper", "kingdom", "knight", "lagoon", "lantern",
            "legend", "leopard", "lighthouse", "lily", "lion", "lodge", "meadow", "meteor",
            "mirror", "monarch", "monument", "moon", "mountain", "museum", "navigator", "nest",
            "oasis", "ocean", "orbit", "orchard", "owl", "palace", "panther", "passage",
            "pathway", "peak", "pebble", "pelican", "phoenix", "pillar", "pilot", "pioneer",
            "planet", "plateau", "plover", "portal", "prairie", "prism", "pyramid", "quarry",
            "ranger", "raven", "reef", "refuge", "ridge", "river", "rocket", "sailor",
            "sanctuary", "sapphire", "scout", "sculptor", "sentinel", "shadow", "shelter", "shield",
            "ship", "shore", "siren", "solace", "sparrow", "sphere", "spiral", "spring",
            "stadium", "statue", "steeple", "stone", "stream", "summit", "sunrise", "sunset",
            "swallow", "swan", "temple", "timber", "torch", "tower", "trail", "tribute",
            "tunnel", "valley", "vanguard", "vessel", "village", "violin", "voyage", "waterfall",
            "wave", "willow", "wind", "wing", "wolf", "zenith"
    };

    private static final String[] NOUNS_PLURAL = {
            "adventures", "anchors", "beacons", "breezes", "bridges", "brooks", "canyons",
            "castles", "citadels", "cliffs", "clouds", "crystals", "currents", "diamonds",
            "dolphins", "eagles", "echoes", "falcons", "feathers", "fjords", "forests",
            "galaxies", "gardens", "glaciers", "harbors", "havens", "horizons", "islands",
            "journeys", "kingdoms", "knights", "lanterns", "legends", "meadows", "meteors",
            "monuments", "mountains", "oceans", "pathways", "peaks", "pillars", "pioneers",
            "planets", "prisms", "pyramids", "ravens", "ridges", "rivers", "sanctuaries",
            "shadows", "shelters", "shields", "sparrows", "statues", "stones", "streams",
            "summits", "temples", "towers", "travelers", "valleys", "voyages", "whispers"
    };

    private static final String[] VERBS_PAST = {
            "admired", "anchored", "approached", "awakened", "balanced", "bordered", "brightened",
            "built", "circled", "cleared", "climbed", "conquered", "crossed", "danced",
            "dawned", "descended", "discovered", "echoed", "embraced", "emerged", "encircled",
            "entered", "escaped", "explored", "flowed", "followed", "forged", "formed",
            "found", "gathered", "glided", "glowed", "graced", "greeted", "guarded",
            "guided", "harbored", "held", "honored", "illuminated", "joined", "journeyed",
            "kindled", "landed", "led", "lifted", "lighted", "lingered", "marked",
            "matched", "mirrored", "navigated", "noticed", "observed", "opened", "passed",
            "paved", "protected", "pursued", "reached", "reflected", "rescued", "rested",
            "returned", "revealed", "roamed", "safeguarded", "sailed", "scanned", "searched",
            "secured", "settled", "shielded", "sparkled", "spanned", "surrounded", "tracked",
            "traveled", "traversed", "uncovered", "unfolded", "visited", "wandered", "watched",
            "weathered", "welcomed", "witnessed"
    };

    private static final String[] VERBS_PRESENT = {
            "advances", "anchors", "awakens", "balances", "blooms", "brightens", "circles",
            "climbs", "crosses", "dances", "descends", "discovers", "echoes", "embraces",
            "emerges", "encircles", "enters", "explores", "flows", "follows", "forges",
            "glides", "glows", "guides", "illuminates", "journeys", "lingers", "navigates",
            "observes", "protects", "radiates", "reflects", "resonates", "reveals", "roams",
            "safeguards", "shines", "surrounds", "thrives", "traverses", "wanders", "watches"
    };

    private static final String[] VERBS_BASE = {
            "admire", "anchor", "awaken", "balance", "bloom", "brighten", "build",
            "circle", "climb", "cross", "dance", "discover", "echo", "embrace",
            "emerge", "explore", "follow", "forge", "glide", "glow", "guide",
            "illuminate", "inspire", "journey", "navigate", "observe", "protect", "radiate",
            "reflect", "resonate", "reveal", "safeguard", "shine", "thrive", "travel",
            "uncover", "unfold", "wander", "watch", "witness"
    };

    private static final String[] PREPOSITIONS = {
            "above", "across", "against", "along", "amidst", "around", "before", "behind",
            "beneath", "beside", "between", "beyond", "inside", "near", "past", "through",
            "toward", "under", "upon", "within", "without"
    };

    private static final String[] ADVERBS = {
            "boldly", "bravely", "brightly", "calmly", "carefully", "clearly", "constantly",
            "daringly", "eagerly", "elegantly", "faithfully", "firmly", "freely", "gently",
            "gladly", "gracefully", "happily", "keenly", "patiently", "peacefully", "promptly",
            "proudly", "quietly", "radiantly", "rapidly", "safely", "serenely", "silently",
            "smoothly", "softly", "steadily", "subtly", "swiftly", "warmly", "wisely"
    };

    @PostConstruct
    public void init() {
        loadFullWordList();
    }

    private void loadFullWordList() {
        try {
            ClassPathResource resource = new ClassPathResource("recovery-words.txt");
            if (resource.exists()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            fullWordList.add(trimmed);
                        }
                    }
                }
                log.info("Loaded {} recovery words from recovery-words.txt", fullWordList.size());
            } else {
                log.warn("recovery-words.txt not found on classpath, using inline dictionaries.");
            }
        } catch (Exception e) {
            log.warn("Could not load recovery-words.txt from classpath ({}), using inline words.", e.getMessage());
        }
    }

    /**
     * Generates a 40-60 word grammatically valid recovery paragraph with >100 bits of entropy.
     *
     * @return generated recovery paragraph string
     */
    public String generateParagraph() {
        // We generate 4 sentences using distinct grammatical templates
        List<Integer> templateIndices = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11));
        Collections.shuffle(templateIndices, secureRandom);

        StringBuilder paragraph = new StringBuilder();
        int sentenceCount = 4;

        for (int i = 0; i < sentenceCount; i++) {
            int templateId = templateIndices.get(i);
            String sentence = buildSentence(templateId);
            if (i > 0) {
                paragraph.append(" ");
            }
            paragraph.append(sentence);
        }

        return paragraph.toString();
    }

    private String buildSentence(int templateId) {
        return switch (templateId) {
            case 0 -> String.format("The %s %s %s %s the %s %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(VERBS_PAST), pick(PREPOSITIONS), pick(ADJECTIVES), pick(NOUNS));
            case 1 -> String.format("Every %s %s %s %s when a %s %s arrives.",
                    pick(ADJECTIVES), pick(NOUNS), pick(ADVERBS), pick(VERBS_PRESENT), pick(ADJECTIVES), pick(NOUNS));
            case 2 -> String.format("A %s %s will %s %s each %s %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(VERBS_BASE), pick(PREPOSITIONS), pick(ADJECTIVES), pick(NOUNS));
            case 3 -> String.format("While the %s %s %s, another %s %s stayed %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(VERBS_PAST), pick(ADJECTIVES), pick(NOUNS), pick(ADJECTIVES));
            case 4 -> String.format("No %s %s can %s without a %s %s nearby.",
                    pick(ADJECTIVES), pick(NOUNS), pick(VERBS_BASE), pick(ADJECTIVES), pick(NOUNS));
            case 5 -> String.format("The %s %s %s %s across the %s %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(ADVERBS), pick(VERBS_PAST), pick(ADJECTIVES), pick(NOUNS));
            case 6 -> String.format("Whenever a %s %s %s, the %s %s listens %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(VERBS_PRESENT), pick(ADJECTIVES), pick(NOUNS), pick(ADVERBS));
            case 7 -> String.format("Two %s %s %s %s beside the %s %s.",
                    pick(ADJECTIVES), pick(NOUNS_PLURAL), pick(ADVERBS), pick(VERBS_PAST), pick(ADJECTIVES), pick(NOUNS));
            case 8 -> String.format("A %s %s and a %s %s %s together %s the %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(ADJECTIVES), pick(NOUNS), pick(VERBS_PAST), pick(PREPOSITIONS), pick(NOUNS));
            case 9 -> String.format("Because the %s %s was %s, the %s %s decided to %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(ADJECTIVES), pick(ADJECTIVES), pick(NOUNS), pick(VERBS_BASE));
            case 10 -> String.format("In the %s %s, a %s %s %s %s the %s.",
                    pick(ADJECTIVES), pick(NOUNS), pick(ADJECTIVES), pick(NOUNS), pick(VERBS_PAST), pick(PREPOSITIONS), pick(NOUNS));
            default -> String.format("The %s %s %s %s until the %s %s appeared.",
                    pick(ADJECTIVES), pick(NOUNS), pick(ADVERBS), pick(VERBS_PAST), pick(ADJECTIVES), pick(NOUNS));
        };
    }

    private String pick(String[] words) {
        int index = secureRandom.nextInt(words.length);
        return words[index];
    }

    /**
     * Normalizes a recovery paragraph by lowercasing, stripping punctuation, and collapsing whitespaces.
     *
     * @param raw the raw input paragraph
     * @return normalized text
     */
    public String normalizeParagraph(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Hashes the normalized recovery paragraph using Argon2id.
     *
     * @param rawParagraph raw user paragraph
     * @return Argon2id hash string
     */
    public String hashParagraph(String rawParagraph) {
        String normalized = normalizeParagraph(rawParagraph);
        return passwordEncoder.encode(normalized);
    }

    /**
     * Verifies a raw recovery paragraph against an Argon2id stored hash.
     *
     * @param rawParagraph user supplied paragraph
     * @param storedHash Argon2id hash in database
     * @return true if valid match, false otherwise
     */
    public boolean verifyParagraph(String rawParagraph, String storedHash) {
        if (rawParagraph == null || storedHash == null) {
            return false;
        }
        String normalized = normalizeParagraph(rawParagraph);
        return passwordEncoder.matches(normalized, storedHash);
    }
}
