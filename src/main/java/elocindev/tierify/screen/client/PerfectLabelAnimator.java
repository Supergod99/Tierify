package elocindev.tierify.screen.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class PerfectLabelAnimator {

    // Transcendent label text
    private static final String WORD = "✯Perfect✯";

    // Full mega-gradient loop duration (ms)
    private static final float PERIOD_MS = 4000.0f;

    // Horizontal spacing between characters along the gradient
    private static final float CHAR_SPACING = 0.10f;

    // --- Upper-tier gradients 

    // Rare deep blue → cyan pulse
    private static final int[][] RARE_COLORS = new int[][]{
            {80, 150, 255},
            {0, 60, 160},
            {120, 220, 255}
    };

    // Epic purple / magenta wave
    private static final int[][] EPIC_COLORS = new int[][]{
            {180, 70, 255},
            {100, 0, 180},
            {230, 150, 255}
    };

    // Legendary hot gold → amber
    private static final int[][] LEGENDARY_COLORS = new int[][]{
            {255, 180, 0},
            {255, 220, 80},
            {255, 140, 0}
    };

    // Mythic crimson → eldritch magenta
    private static final int[][] MYTHIC_COLORS = new int[][]{
            {255, 60, 60},
            {180, 0, 80},
            {255, 120, 180}
    };

    // Perfect: deep gold → white flame → deep cyan
    private static final int[][] PERFECT_COLORS = new int[][]{
            {224, 164, 20},   // deep gold
            {255, 255, 255},  // white
            {0, 183, 255}     // deep cyan
    };

    // Pre-flattened mega-gradient stop list:
    // Rare -> Epic -> Legendary -> Mythic -> Perfect
    // We will build this lazily once.
    private static int[] MEGA_COLORS = null;
    private static float[] SEGMENT_LENGTHS = null; // weights for each segment for smoother transitions


    public static void clientTick() {
        // no-op: animation uses System.currentTimeMillis()
    }

    public static MutableText getPerfectLabel() {
        String word = WORD;
        if (word == null || word.isEmpty()) {
            return Text.empty();
        }

        ensureMegaGradientBuilt();

        int length = word.length();
        MutableText result = Text.empty();

        long now = System.currentTimeMillis();
        float phase = (PERIOD_MS <= 0.0f)
                ? 0.0f
                : (now % (long) PERIOD_MS) / PERIOD_MS; // 0..1

        for (int i = 0; i < length; i++) {
            char c = word.charAt(i);

            if (Character.isWhitespace(c)) {
                result.append(Text.literal(String.valueOf(c)));
                continue;
            }

            // Is this one of the stars at the ends?
            boolean isStar = (i == 0 || i == length - 1) && (c == '✯');

            // Position of this character along the mega-gradient (0..1)
            float pos = phase + (i * CHAR_SPACING);
            pos = wrap01(pos);

            int rgb = sampleMegaGradient(pos);

            Style style = Style.EMPTY
                    .withColor(TextColor.fromRgb(rgb))
                    .withBold(!isStar); // letters bold, stars NOT bold

            result.append(Text.literal(String.valueOf(c)).setStyle(style));
        }

        return result;
    }

    // --- Mega Gradient Construction & Sampling ---

    /**
     * Build the mega-gradient color stops and segment weights.
     * We do:
     * Rare(3) -> Epic(3) -> Legendary(3) -> Mythic(3) -> Perfect(3)
     * with longer, smoother transitions between tiers.
     */
    private static void ensureMegaGradientBuilt() {
        if (MEGA_COLORS != null && SEGMENT_LENGTHS != null) {
            return;
        }

        // Flatten all tier arrays into one list of packed RGB ints
        int[][] all = new int[][]{
                RARE_COLORS[0], RARE_COLORS[1], RARE_COLORS[2],
                EPIC_COLORS[0], EPIC_COLORS[1], EPIC_COLORS[2],
                LEGENDARY_COLORS[0], LEGENDARY_COLORS[1], LEGENDARY_COLORS[2],
                MYTHIC_COLORS[0], MYTHIC_COLORS[1], MYTHIC_COLORS[2],
                PERFECT_COLORS[0], PERFECT_COLORS[1], PERFECT_COLORS[2]
        };

        MEGA_COLORS = new int[all.length];
        for (int i = 0; i < all.length; i++) {
            MEGA_COLORS[i] = rgbFromArray(all[i]);
        }

        // Segment lengths between each pair of stops.
        // We want NO harsh jumps, so:
        // - within a tier (0-1,1-2; 3-4,4-5; etc) get length 1.0
        // - transitions between tiers (2-3,5-6,8-9,11-12) get length 2.0
        int nSegments = MEGA_COLORS.length - 1;
        SEGMENT_LENGTHS = new float[nSegments];

        for (int i = 0; i < nSegments; i++) {
            // Tier indices:
            // Rare:      0,1,2
            // Epic:      3,4,5
            // Legendary: 6,7,8
            // Mythic:    9,10,11
            // Perfect:   12,13,14
            boolean crossTier =
                    (i == 2) ||  // Rare -> Epic
                    (i == 5) ||  // Epic -> Legendary
                    (i == 8) ||  // Legendary -> Mythic
                    (i == 11);   // Mythic -> Perfect

            SEGMENT_LENGTHS[i] = crossTier ? 2.0f : 1.0f;
        }
    }

    /**
     * Sample the mega-gradient at t in [0,1].
     */
    private static int sampleMegaGradient(float t) {
        t = clamp01(t);

        int nSegments = SEGMENT_LENGTHS.length;
        if (nSegments <= 0) {
            // Fallback: just return first color if something is off
            return (MEGA_COLORS != null && MEGA_COLORS.length > 0) ? MEGA_COLORS[0] : 0xFFFFFF;
        }

        // Total "length" of all segments
        float total = 0.0f;
        for (float seg : SEGMENT_LENGTHS) {
            total += seg;
        }

        float scaled = t * total;

        // Find which segment we land in
        float accum = 0.0f;
        for (int i = 0; i < nSegments; i++) {
            float segLen = SEGMENT_LENGTHS[i];
            float nextAccum = accum + segLen;

            if (scaled <= nextAccum || i == nSegments - 1) {
                float localT = (scaled - accum) / segLen;
                localT = clamp01(localT);
                int c1 = MEGA_COLORS[i];
                int c2 = MEGA_COLORS[i + 1];
                return mixColor(c1, c2, localT);
            }

            accum = nextAccum;
        }

        // Fallback (should not be reached)
        return MEGA_COLORS[MEGA_COLORS.length - 1];
    }

    // --- Utility helpers ---

    private static int rgbFromArray(int[] arr) {
        if (arr == null || arr.length < 3) {
            return 0xFFFFFF;
        }
        return rgb(arr[0], arr[1], arr[2]);
    }

    private static int rgb(int r, int g, int b) {
        r &= 0xFF;
        g &= 0xFF;
        b &= 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static int mixColor(int c1, int c2, float t) {
        t = clamp01(t);

        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) {
        return (v < 0f) ? 0f : (v > 1f) ? 1f : v;
    }

    private static float wrap01(float v) {
        v = v % 1.0f;
        if (v < 0f) v += 1.0f;
        return v;
    }
}
