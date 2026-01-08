package elocindev.tierify.forge.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Forge port of Fabric's TierGradientAnimator (palette + math parity).
 *
 * Notes:
 * - Keeps the legacy tier key spelling "uncomon" (intentional).
 * - Legendary + Mythic remain bold.
 * - Uses a time-based sweep (System.currentTimeMillis) like Fabric; no tick hook required.
 */
public final class TierGradientAnimatorForge {

    private TierGradientAnimatorForge() {}

    // Common
    private static final int[][] COMMON_COLORS = new int[][]{
            {140, 140, 140},
            {90,  90,  90},
            {160, 160, 160}
    };

    // Uncommon ("uncomon" legacy spelling)
    private static final int[][] UNCOMMON_COLORS = new int[][]{
            {90,  200, 90},
            {0,   120, 0},
            {140, 255, 140}
    };

    // Rare
    private static final int[][] RARE_COLORS = new int[][]{
            {80,  150, 255},
            {0,   60,  160},
            {120, 220, 255}
    };

    // Epic
    private static final int[][] EPIC_COLORS = new int[][]{
            {180, 70,  255},
            {100, 0,   180},
            {230, 150, 255}
    };

    // Legendary
    private static final int[][] LEGENDARY_COLORS = new int[][]{
            {255, 180, 0},
            {255, 220, 80},
            {255, 140, 0}
    };

    // Mythic
    private static final int[][] MYTHIC_COLORS = new int[][]{
            {255, 60,  60},
            {180, 0,   80},
            {255, 120, 180}
    };

    /**
     * Forge-facing tier bucket mapping used by current client hooks.
     *
     * Indexes:
     * 0 = common
     * 1 = uncomon
     * 2 = rare
     * 3 = epic
     * 4 = legendary
     * 5 = mythic
     */
    public static int getTierFromId(String id) {
        if (id == null) return 0;
        String s = id.toLowerCase();
        if (s.contains("mythic")) return 5;
        if (s.contains("legendary")) return 4;
        if (s.contains("epic")) return 3;
        if (s.contains("rare")) return 2;
        // IMPORTANT: do NOT support "uncommon" here; only the legacy spelling.
        if (s.contains("uncomon")) return 1;
        return 0;
    }

    public static MutableComponent animate(Component base, int tierIndex) {
        if (base == null) return Component.empty();

        String raw = base.getString();
        if (raw.isEmpty()) return Component.empty();

        int[][] palette = getPaletteForTierIndex(tierIndex);

        int length = raw.length();
        MutableComponent result = Component.empty();

        long now = System.currentTimeMillis();
        double timeOffset = (now / 35L) % 100.0; // parity with Fabric

        boolean bold = (tierIndex == 4 || tierIndex == 5); // legendary + mythic

        for (int i = 0; i < length; i++) {
            char c = raw.charAt(i);

            // Keep whitespace unstyled (Fabric behavior)
            if (Character.isWhitespace(c)) {
                result.append(Component.literal(String.valueOf(c)));
                continue;
            }

            // Offset by time to make it flow horizontally (Fabric behavior)
            double basePos = (length == 1) ? 50.0 : (i * (100.0 / (length - 1)));
            double animatedPos = (basePos + timeOffset) % 100.0;

            int rgb = getColorFromGradient((int) animatedPos, palette);

            Style style = Style.EMPTY.withColor(rgb);
            if (bold) {
                style = style.withBold(true);
            }

            result.append(Component.literal(String.valueOf(c)).setStyle(style));
        }

        return result;
    }

    private static int[][] getPaletteForTierIndex(int tierIndex) {
        switch (tierIndex) {
            case 1:
                return UNCOMMON_COLORS;
            case 2:
                return RARE_COLORS;
            case 3:
                return EPIC_COLORS;
            case 4:
                return LEGENDARY_COLORS;
            case 5:
                return MYTHIC_COLORS;
            case 0:
            default:
                return COMMON_COLORS;
        }
    }

    /**
     * Matches Fabric's cyclic segmented gradient:
     * - Colors are treated as N segments across 0..100 (exclusive-ish)
     * - Interpolates within the segment, and wraps to the first color at the end.
     */
    private static int getColorFromGradient(int percentage, int[][] colors) {
        if (colors == null || colors.length == 0) {
            return rgb(255, 255, 255);
        }
        if (colors.length == 1) {
            int[] c = colors[0];
            return rgb(c[0], c[1], c[2]);
        }

        if (percentage < 0) percentage = 0;
        if (percentage > 100) percentage = 100;

        int segments = colors.length;
        double segmentLength = 100.0 / segments;

        int segmentIndex = (int) Math.floor(percentage / segmentLength);
        if (segmentIndex >= segments) segmentIndex = segments - 1;

        int nextIndex = (segmentIndex + 1) % segments; // wrap to first
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
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (r << 16) | (g << 8) | b;
    }
}
