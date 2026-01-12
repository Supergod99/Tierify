package elocindev.tierify.forge.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Forge port of Fabric's PerfectLabelAnimator.
 *
 * Goals:
 *  - mathematically / palette-identical to Fabric
 *  - stars use their own pulsing base color (not tier gradients)
 *  - letters remain bold; stars are not bold
 */
public final class PerfectLabelAnimatorForge {
    private PerfectLabelAnimatorForge() {}

    private static final String WORD = "\u272fPerfect\u272f";

    private static final float TOTAL_PERIOD_MS = 8000.0f;

    private static final int TIER_COUNT = 5;
    private static final float TIER_SLOT_FRACTION = 1.0f / TIER_COUNT; // 0.2
    private static final float TIER_DURATION_MS = TOTAL_PERIOD_MS * TIER_SLOT_FRACTION; // 1600ms

    private static final float CROSSFADE_MS = 250.0f;
    private static final float CROSSFADE_FRACTION = CROSSFADE_MS / TIER_DURATION_MS;

    // How much the gradient shifts per character
    private static final float CHAR_WAVE_SPACING = 0.12f;

    // Rare
    private static final int[] RARE_COLORS = new int[] {
            rgb(80, 150, 255),
            rgb(0, 60, 160),
            rgb(120, 220, 255)
    };

    // Epic
    private static final int[] EPIC_COLORS = new int[] {
            rgb(180, 70, 255),
            rgb(100, 0, 180),
            rgb(230, 150, 255)
    };

    // Legendary
    private static final int[] LEGENDARY_COLORS = new int[] {
            rgb(255, 180, 0),
            rgb(255, 220, 80),
            rgb(255, 140, 0)
    };

    // Mythic
    private static final int[] MYTHIC_COLORS = new int[] {
            rgb(255, 60, 60),
            rgb(180, 0, 80),
            rgb(255, 120, 180)
    };

    // Perfect
    private static final int[] PERFECT_COLORS = new int[] {
            rgb(164, 0, 255),   // Electric Violet
            rgb(0, 245, 204),   // Radiant Teal
            rgb(230, 247, 255)  // Starlight Silver
    };

    private static final int STAR_BASE_COLOR = rgb(212, 240, 255); // #D4F0FF

    private static final float STAR_PULSE_MIN = 0.6f;
    private static final float STAR_PULSE_MAX = 1.6f;

    public static MutableComponent animatedLabel(long nowMs) {
        long t = (nowMs > 0L) ? nowMs : System.currentTimeMillis();

        if (WORD == null || WORD.isEmpty()) {
            return Component.empty();
        }

        int length = WORD.length();
        MutableComponent result = Component.empty();

        float cyclePhase = (TOTAL_PERIOD_MS <= 0.0f)
                ? 0.0f
                : (t % (long) TOTAL_PERIOD_MS) / TOTAL_PERIOD_MS;

        // Determine tier window + local phase
        int tierIndex = (int) (cyclePhase / TIER_SLOT_FRACTION);
        if (tierIndex >= TIER_COUNT) {
            tierIndex = TIER_COUNT - 1;
        }
        float tierStart = tierIndex * TIER_SLOT_FRACTION;
        float tierLocalPhase = (cyclePhase - tierStart) / TIER_SLOT_FRACTION;

        // Crossfade data
        int primaryTier = tierIndex;
        Integer secondaryTier = null;
        float primaryWeight = 1.0f;
        float secondaryWeight = 0.0f;

        if (tierLocalPhase < CROSSFADE_FRACTION) {
            // Fade in from previous tier
            secondaryTier = (tierIndex - 1 + TIER_COUNT) % TIER_COUNT;
            float tLocal = tierLocalPhase / CROSSFADE_FRACTION;
            secondaryWeight = clamp01(1.0f - tLocal);
            primaryWeight = clamp01(tLocal);
        } else if (tierLocalPhase > 1.0f - CROSSFADE_FRACTION) {
            // Fade out to next tier
            secondaryTier = (tierIndex + 1) % TIER_COUNT;
            float tLocal = (tierLocalPhase - (1.0f - CROSSFADE_FRACTION)) / CROSSFADE_FRACTION;
            primaryWeight = clamp01(1.0f - tLocal);
            secondaryWeight = clamp01(tLocal);
        }

        // Slow gradient drift
        float tierDrift = tierLocalPhase;

        // Star pulse
        float starPulse = 0.5f - 0.5f * (float) Math.cos(2.0 * Math.PI * cyclePhase);
        float starLum = STAR_PULSE_MIN + (STAR_PULSE_MAX - STAR_PULSE_MIN) * starPulse;

        char starChar = WORD.charAt(0);

        for (int i = 0; i < length; i++) {
            char c = WORD.charAt(i);

            boolean isStar = (i == 0 || i == length - 1) && c == starChar;

            int rgb;

            if (isStar) {
                rgb = scaleColor(STAR_BASE_COLOR, starLum);
            } else {
                float charPhase = (tierDrift + i * CHAR_WAVE_SPACING) % 1.0f;
                if (charPhase < 0.0f) charPhase += 1.0f;
                // Color from primary tier
                int primaryColor = getTierGradientColor(primaryTier, charPhase);

                if (secondaryTier != null && secondaryWeight > 0.0f) {
                    int secondaryColor = getTierGradientColor(secondaryTier, charPhase);
                    rgb = mixColor(primaryColor, secondaryColor, secondaryWeight);
                } else {
                    rgb = primaryColor;
                }
            }

            Style style = Style.EMPTY
                    .withColor(rgb)
                    .withBold(!isStar);

            result.append(Component.literal(String.valueOf(c)).setStyle(style));
        }

        return result;
    }

    private static int getTierGradientColor(int tierIndex, float t) {
        t = wrap01(t);

        int[] stops;
        switch (tierIndex) {
            case 0: stops = RARE_COLORS; break;
            case 1: stops = EPIC_COLORS; break;
            case 2: stops = LEGENDARY_COLORS; break;
            case 3: stops = MYTHIC_COLORS; break;
            case 4:
            default:
                stops = PERFECT_COLORS; break;
        }

        int c0 = stops[0];
        int c1 = stops[1];
        int c2 = stops[2];

        if (t < 0.5f) {
            return mixColor(c0, c1, t * 2.0f);
        } else {
            return mixColor(c1, c2, (t - 0.5f) * 2.0f);
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static int rgb(int r, int g, int b) {
        r &= 0xFF;
        g &= 0xFF;
        b &= 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static int scaleColor(int color, float lum) {
        int r = (int) (clamp01(((color >> 16) & 0xFF) * lum / 255f) * 255);
        int g = (int) (clamp01(((color >> 8) & 0xFF) * lum / 255f) * 255);
        int b = (int) (clamp01((color & 0xFF) * lum / 255f) * 255);
        return rgb(r, g, b);
    }

    private static int mixColor(int a, int b, float t) {
        t = clamp01(t);

        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;

        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;

        int r = Math.round(ar + (br - ar) * t);
        int g = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);

        return (r << 16) | (g << 8) | bl;
    }

    private static float wrap01(float v) {
        v %= 1.0f;
        if (v < 0f) v += 1.0f;
        return v;
    }
}
