package elocindev.tierify.screen.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class PerfectLabelAnimator {

    // New label text: cosmic symmetrical brackets
    private static final String WORD = "✯Perfect✯";

    // Global breathing / hue cycle (~4 seconds)
    private static final float PERIOD_MS = 4000.0f;
    private static final float WAVE_SPACING = 0.12f;

    // Sweeping highlight streak (~2.6 seconds per pass)
    private static final float STREAK_PERIOD_MS = 2600.0f;
    private static final float STREAK_WIDTH = 0.28f;
    private static final float STREAK_INTENSITY = 0.45f;

    // Synchronized starburst pulse (~2.5 seconds)
    private static final float STARBURST_PERIOD_MS = 2500.0f;
    private static final float STARBURST_PULSE_INTENSITY = 0.60f;

    // Vertical "pillar of radiance"
    private static final float VERTICAL_OVERLAY_STRENGTH = 0.22f;

    // Permanent halo for stars (+ pulse)
    private static final float STAR_HALO_BASE = 0.35f;
    private static final float STAR_HALO_EXTRA_FROM_PULSE = 0.50f;

    // Bloom-style breathing
    private static final float BLOOM_INTENSITY = 0.55f;

    // Mild HSV drift (still used for depth)
    private static final float BASE_HUE = 0.12f;
    private static final float HUE_WARP_AMPLITUDE = 0.02f;
    private static final float SATURATION = 0.9f;
    private static final float MIN_VALUE = 0.75f;
    private static final float MAX_VALUE = 1.00f;

    // *** NEW: Deeper artifact-tier cosmic palette ***
    private static final int GOLD_COLOR = 0xE0A414;       // deep cosmic gold
    private static final int GOLD_PEAK_COLOR = 0xFFD36B;  // gold bloom (bright, warm)
    private static final int CYAN_COLOR = 0x00B7FF;       // deep starfall cyan
    private static final int WHITE_COLOR = 0xFFFFFF;      // white flame


    public static void clientTick() {
        // animation uses System.currentTimeMillis()
    }


    public static MutableText getPerfectLabel() {
        String word = WORD;
        if (word == null || word.isEmpty())
            return Text.empty();

        MutableText result = Text.empty();
        int length = word.length();
        if (length <= 0)
            return result;

        long now = System.currentTimeMillis();

        // Global 0..1 breathing phase
        float phase = (PERIOD_MS <= 0) ? 0f : (now % (long) PERIOD_MS) / PERIOD_MS;

        // Global 0..1 sweeping streak phase
        float streakPhase = (STREAK_PERIOD_MS <= 0) ? 0f : (now % (long) STREAK_PERIOD_MS) / STREAK_PERIOD_MS;

        // Global 0..1 synchronized starburst phase
        float starburstPhase = (STARBURST_PERIOD_MS <= 0) ? 0f : (now % (long) STARBURST_PERIOD_MS) / STARBURST_PERIOD_MS;

        // Smooth starburst curve (peaks sharply)
        float starburstPulse = 0.5f - 0.5f * (float) Math.cos(2.0 * Math.PI * starburstPhase);
        starburstPulse = starburstPulse * starburstPulse;

        // Global breathing bloom
        float breathe = 0.5f - 0.5f * (float) Math.cos(2.0 * Math.PI * phase);
        float value = MIN_VALUE + (MAX_VALUE - MIN_VALUE) * breathe;

        // Gentle HSV hue drift
        float hue = BASE_HUE + HUE_WARP_AMPLITUDE * (float) Math.sin(2.0 * Math.PI * phase);
        float saturation = SATURATION;

        int globalBaseRgb = hsvToRgb(hue, saturation, value);

        for (int i = 0; i < length; i++) {
            char c = word.charAt(i);

            boolean isStar = (i == 0 || i == length - 1) && c == '✯';

            if (Character.isWhitespace(c)) {
                result.append(Text.literal(String.valueOf(c)));
                continue;
            }

            // Local 0..1 wave offset
            float localPhase = (phase + i * WAVE_SPACING) % 1.0f;
            if (localPhase < 0)
                localPhase += 1.0f;

            // Tri-color cosmic gradient (deep gold → white flame → deep cyan)
            int triColor = getTriGradientColor(localPhase);

            // Blend tri-gradient with the golden HSV drift base
            int rgb = mixColor(triColor, globalBaseRgb, 0.35f);

            // Bloom breathing (push toward white)
            rgb = mixColor(rgb, WHITE_COLOR, BLOOM_INTENSITY * breathe);

            // Vertical radiance pillar
            float verticalFactor;
            if (length == 1)
                verticalFactor = 1f;
            else
                verticalFactor = (float) i / (float) (length - 1);

            verticalFactor = (verticalFactor - 0.5f) * 2f;
            float verticalCenter = 1f - Math.abs(verticalFactor);
            float pillarAmount = verticalCenter * VERTICAL_OVERLAY_STRENGTH;
            rgb = mixColor(rgb, WHITE_COLOR, pillarAmount);

            // Sweeping chromatic comet arc
            float pos = (length == 1) ? 0.5f : (float) i / (float) (length - 1);
            float dist = circularDistance(pos, streakPhase);

            if (dist < STREAK_WIDTH) {
                float streakT = 1f - (dist / STREAK_WIDTH);
                float streakAmt = streakT * STREAK_INTENSITY;

                int streakColor = getTriGradientColor(streakPhase);
                rgb = mixColor(rgb, streakColor, streakAmt);
                rgb = mixColor(rgb, WHITE_COLOR, streakAmt * 0.35f);
            }

            // *** Starburst + halo: deeper gold + white flare ***
            if (isStar) {
                float halo = STAR_HALO_BASE + STAR_HALO_EXTRA_FROM_PULSE * starburstPulse;
                halo = clamp01(halo);

                // Deep gold base halo
                rgb = mixColor(rgb, GOLD_COLOR, halo * 0.80f);
                // Gold bloom peak
                rgb = mixColor(rgb, GOLD_PEAK_COLOR, halo * 0.40f);
                // White flame flare
                rgb = mixColor(rgb, WHITE_COLOR, halo * 0.25f);
            }

            // *** Only change: stars NOT bold, letters bold ***
            Style style = Style.EMPTY
                    .withColor(TextColor.fromRgb(rgb))
                    .withBold(!isStar);

            result.append(Text.literal(String.valueOf(c)).setStyle(style));
        }

        return result;
    }


    // *** Tri-phase deeper cosmic gradient ***
    private static int getTriGradientColor(float t) {
        t = wrap01(t);

        if (t < (1f / 3f)) {
            float local = t * 3f;
            return mixColor(GOLD_COLOR, WHITE_COLOR, local);
        }
        else if (t < (2f / 3f)) {
            float local = (t - 1f / 3f) * 3f;
            return mixColor(WHITE_COLOR, CYAN_COLOR, local);
        }
        else {
            float local = (t - 2f / 3f) * 3f;
            return mixColor(CYAN_COLOR, GOLD_COLOR, local);
        }
    }


    private static int mixColor(int base, int add, float t) {
        t = clamp01(t);

        int r1 = (base >> 16) & 0xFF;
        int g1 = (base >> 8) & 0xFF;
        int b1 = base & 0xFF;

        int r2 = (add >> 16) & 0xFF;
        int g2 = (add >> 8) & 0xFF;
        int b2 = add & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }


    private static float circularDistance(float a, float b) {
        float d = Math.abs(a - b);
        return Math.min(d, 1f - d);
    }


    private static int hsvToRgb(float h, float s, float v) {
        h = wrap01(h);
        s = clamp01(s);
        v = clamp01(v);

        float c = v * s;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));

        float r1, g1, b1;
        if (hp < 1f)       { r1 = c; g1 = x; b1 = 0; }
        else if (hp < 2f)  { r1 = x; g1 = c; b1 = 0; }
        else if (hp < 3f)  { r1 = 0; g1 = c; b1 = x; }
        else if (hp < 4f)  { r1 = 0; g1 = x; b1 = c; }
        else if (hp < 5f)  { r1 = x; g1 = 0; b1 = c; }
        else               { r1 = c; g1 = 0; b1 = x; }

        float m = v - c;
        int r = (int) ((r1 + m) * 255f + 0.5f);
        int g = (int) ((g1 + m) * 255f + 0.5f);
        int b = (int) ((b1 + m) * 255f + 0.5f);

        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }


    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float wrap01(float v) {
        v %= 1f;
        if (v < 0f) v += 1f;
        return v;
    }
}



