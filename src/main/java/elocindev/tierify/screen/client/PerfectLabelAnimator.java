package elocindev.tierify.screen.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class PerfectLabelAnimator {

    // The new label text
    private static final String WORD = "✦Perfect✦";

    // Animation timing
    private static final float PERIOD_MS = 4000.0f;     // breathing + hue warp
    private static final float WAVE_SPACING = 0.12f;    // per-letter phase shift
    private static final float STREAK_PERIOD_MS = 2600.0f;  // sweeping highlight

    // Color palette (cosmic aqua -> near white)
    private static final float BASE_HUE = 0.52f;          // teal/aqua
    private static final float HUE_WARP_AMPLITUDE = 0.015f;
    private static final float MIN_VALUE = 0.70f;         // always bright
    private static final float MAX_VALUE = 1.00f;
    private static final float SATURATION = 0.30f;        // desaturated -> white-ish


    private static final float VERTICAL_OVERLAY_STRENGTH = 0.18f;

    // How bright the sweeping highlight is
    private static final float STREAK_INTENSITY = 0.35f;
    private static final float STREAK_WIDTH = 0.28f;  // wide smooth beam


    public static void clientTick() {

    }


    public static MutableText getPerfectLabel() {

        String word = WORD;
        if (word == null || word.isEmpty()) {
            return Text.empty();
        }

        MutableText result = Text.empty();
        int length = word.length();

        long now = System.currentTimeMillis();

        // breathing animation
        float phase = (PERIOD_MS <= 0.0f)
                ? 0f
                : (now % (long) PERIOD_MS) / PERIOD_MS;

        //  sweeping streak
        float streakPhase = (STREAK_PERIOD_MS <= 0.0f)
                ? 0f
                : (now % (long) STREAK_PERIOD_MS) / STREAK_PERIOD_MS;

        for (int i = 0; i < length; i++) {
            char c = word.charAt(i);

            if (Character.isWhitespace(c)) {
                result.append(Text.literal(String.valueOf(c)));
                continue;
            }

            float localPhase = (phase + i * WAVE_SPACING) % 1.0f;

            // Smooth breathing pulse
            float breathe = 0.5f - 0.5f * (float) Math.cos(2 * Math.PI * localPhase);
            float value = MIN_VALUE + (MAX_VALUE - MIN_VALUE) * breathe;

            // Slight hue drift over cycle
            float hue = BASE_HUE +
                    HUE_WARP_AMPLITUDE * (float) Math.sin(2.0 * Math.PI * phase);

            float saturation = SATURATION;

            // Convert to RGB 
            int rgb = hsvToRgb(hue, saturation, value);

            // Vertical gradient overlay 
            // Gives top a little extra light
            float verticalFactor = (float) i / (float) (length - 1);
            verticalFactor = (verticalFactor - 0.5f) * 2f;  
            float overlay = 1f - Math.abs(verticalFactor); 
            overlay *= VERTICAL_OVERLAY_STRENGTH;

            rgb = mixColor(rgb, 0xFFFFFF, overlay);

            // Sweeping highlight streak 
            float pos = (float) i / (float) (length - 1);  
            float dist = Math.abs(pos - streakPhase);

            if (dist < STREAK_WIDTH) {
                float intensity = (1f - (dist / STREAK_WIDTH)) * STREAK_INTENSITY;
                rgb = mixColor(rgb, 0xFFFFFF, intensity);
            }

            Style style = Style.EMPTY
                    .withColor(TextColor.fromRgb(rgb))
                    .withBold(true);

            result.append(Text.literal(String.valueOf(c)).setStyle(style));
        }

        return result;
    }


    // blend between colors
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

        return (r << 16) | (g << 8) | b;
    }


    private static int hsvToRgb(float h, float s, float v) {
        h = wrap01(h);
        s = clamp01(s);
        v = clamp01(v);

        float c = v * s;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));

        float r1, g1, b1;
        if (hp < 1f) { r1 = c; g1 = x; b1 = 0; }
        else if (hp < 2f) { r1 = x; g1 = c; b1 = 0; }
        else if (hp < 3f) { r1 = 0; g1 = c; b1 = x; }
        else if (hp < 4f) { r1 = 0; g1 = x; b1 = c; }
        else if (hp < 5f) { r1 = x; g1 = 0; b1 = c; }
        else { r1 = c; g1 = 0; b1 = x; }

        float m = v - c;
        int r = (int) ((r1 + m) * 255f);
        int g = (int) ((g1 + m) * 255f);
        int b = (int) ((b1 + m) * 255f);

        return (r << 16) | (g << 8) | b;
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


