package elocindev.tierify.screen.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class TierGradientAnimator {

    // animation here uses System.currentTimeMillis().
    private static int tick = 0;
    private static final int INTERVAL = 2;

    // Gradient anchor colors per tier

    // Common: steel greys
    private static final int[][] COMMON_COLORS = new int[][]{
            {140, 140, 140},
            {90, 90, 90},
            {160, 160, 160}
    };

    // Uncommon: green shimmer
    private static final int[][] UNCOMMON_COLORS = new int[][]{
            {90, 200, 90},
            {0, 120, 0},
            {140, 255, 140}
    };

    // Rare: deep blue → cyan pulse
    private static final int[][] RARE_COLORS = new int[][]{
            {80, 150, 255},
            {0, 60, 160},
            {120, 220, 255}
    };

    // Epic: purple / magenta wave
    private static final int[][] EPIC_COLORS = new int[][]{
            {180, 70, 255},
            {100, 0, 180},
            {230, 150, 255}
    };

    // Legendary: hot gold → amber
    private static final int[][] LEGENDARY_COLORS = new int[][]{
            {255, 180, 0},
            {255, 220, 80},
            {255, 140, 0}
    };

    // Mythic: crimson → eldritch magenta
    private static final int[][] MYTHIC_COLORS = new int[][]{
            {255, 60, 60},
            {180, 0, 80},
            {255, 120, 180}
    };

    public static void clientTick() {

        tick++;
        if (tick >= INTERVAL) {
            tick = 0;
        }
    }

    // Infer tier from the attribute id string 
    public static String getTierFromId(String id) {
        if (id == null) return "common";
        String lower = id.toLowerCase();
        if (lower.contains("mythic")) return "mythic";
        if (lower.contains("legendary")) return "legendary";
        if (lower.contains("epic")) return "epic";
        if (lower.contains("rare")) return "rare";
        if (lower.contains("uncomon")) return "uncomon";
        return "common";
    }


    public static MutableText animate(MutableText base, String tier) {
        if (base == null) {
            return Text.empty();
        }

        String raw = base.getString();
        if (raw.isEmpty()) {
            return base.copy();
        }

        int[][] palette = getPaletteForTier(tier);

        int length = raw.length();
        MutableText result = Text.empty();

        // Time-based offset so gradient flows over the word
        long now = System.currentTimeMillis();
        double timeOffset = (now / 75L) % 100;  // 0..99

        // For each character in the label, compute a gradient position + color
        for (int i = 0; i < length; i++) {
            char c = raw.charAt(i);

            // Skip coloring spaces; just append a plain space to keep spacing nice
            if (Character.isWhitespace(c)) {
                result.append(Text.literal(String.valueOf(c)));
                continue;
            }

            // offset by time to make it flow horizontally.
            double basePos = (length == 1) ? 50.0 : (i * (100.0 / (length - 1)));
            double animatedPos = (basePos + timeOffset) % 100.0;

            int rgb = getColorFromGradient((int) animatedPos, palette);

            Style style = Style.EMPTY.withColor(TextColor.fromRgb(rgb));

            // Legendary/Mythic modifiers: bold
            if ("legendary".equals(tier) || "mythic".equals(tier)) {
                style = style.withBold(true);
            }

            MutableText letter = Text.literal(String.valueOf(c)).setStyle(style);
            result.append(letter);
        }

        return result;
    }

    // Pick the palette based on tier name
    private static int[][] getPaletteForTier(String tier) {
        if (tier == null) return COMMON_COLORS;
        switch (tier.toLowerCase()) {
            case "uncomon":
                return UNCOMMON_COLORS;
            case "rare":
                return RARE_COLORS;
            case "epic":
                return EPIC_COLORS;
            case "legendary":
                return LEGENDARY_COLORS;
            case "mythic":
                return MYTHIC_COLORS;
            case "common":
            default:
                return COMMON_COLORS;
        }
    }

    private static int getColorFromGradient(int percentage, int[][] colors) {
        if (colors == null || colors.length == 0) {
            return rgb(255, 255, 255);
        }
        if (colors.length == 1) {
            int[] c = colors[0];
            return rgb(c[0], c[1], c[2]);
        }

        // Clamp percentage just in case
        if (percentage < 0) percentage = 0;
        if (percentage > 100) percentage = 100;

        int segments = colors.length;
        double segmentLength = 100.0 / segments;
        
        int segmentIndex = (int) Math.floor(percentage / segmentLength);
        if (segmentIndex >= segments) segmentIndex = segments - 1;
        
        int nextIndex = (segmentIndex + 1) % segments;  // Wrap to first color
        
        double localStart = segmentIndex * segmentLength;
        double t = (percentage - localStart) / segmentLength;
        
        int[] c1 = colors[segmentIndex];
        int[] c2 = colors[nextIndex];

        int r = lerp(c1[0], c2[0], t);
        int g = lerp(c1[1], c2[1], t);
        int b = lerp(c1[2], c2[2], t);

        return rgb(r, g, b);
    }

    private static int lerp(int a, int b, double t) {
        return a + (int) Math.round((b - a) * t);
    }

    private static int rgb(int r, int g, int b) {
        // Clamp to 0..255 just to be safe
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }
}
