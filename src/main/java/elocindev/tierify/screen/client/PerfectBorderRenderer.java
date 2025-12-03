package elocindev.tierify.screen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.api.BorderTemplate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class PerfectBorderRenderer {

    // 4 second loop, matches the Perfect label pacing
    private static final long TINT_PERIOD_MS = 4000L;
    // ~3 second glow pulse
    private static final long GLOW_PERIOD_MS = 3000L;

    // Perfect palette: deep gold → white → deep cyan
    private static final int GOLD_COLOR = 0xE0A414;
    private static final int WHITE_COLOR = 0xFFFFFF;
    private static final int CYAN_COLOR  = 0x00B7FF;

    /**
     * Renders animated tint + glow overlay for the Perfect border.
     *
     * This assumes:
     * - The caller already pushed the matrix stack and translated to the correct Z.
     * - The base border pieces have already been drawn.
     *
     * @param context       DrawContext
     * @param borderTemplate The BorderTemplate (used to get index + texture)
     * @param x             tooltip X (n)
     * @param y             tooltip Y (o)
     * @param width         tooltip width (l)
     * @param height        tooltip height (m)
     */
    public static void renderPerfectBorderOverlay(DrawContext context,
                                                  BorderTemplate borderTemplate,
                                                  int x, int y,
                                                  int width, int height) {
        // Only apply to the Perfect border 
        if (borderTemplate.getIndex() != 6) {
            return;
        }

        long time = System.currentTimeMillis();

        // Recompute border row / half 
        int border = borderTemplate.getIndex();
        int secondHalf = border > 7 ? 1 : 0;
        if (border > 7) {
            border -= 8;
        }

        Identifier texture = borderTemplate.getIdentifier();


        // Animated Tint Layer

        float phase = (time % TINT_PERIOD_MS) / (float) TINT_PERIOD_MS; // 0..1

        int tintColor = samplePerfectTint(phase);
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;

        float tintStrength = 0.15f; // subtle

        RenderSystem.setShaderColor(
                lerp(1.0f, r, tintStrength),
                lerp(1.0f, g, tintStrength),
                lerp(1.0f, b, tintStrength),
                1.0f
        );

        // Draw tinted overlay (same geometry as base border)
        drawBorderPieces(context, texture, x, y, width, height, border, secondHalf);

        // Reset color
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        //  Glow Overlay Layer

        float glowPhase = (time % GLOW_PERIOD_MS) / (float) GLOW_PERIOD_MS;
        float pulse = 0.5f - 0.5f * (float) Math.cos(glowPhase * (float) Math.PI * 2.0f);
        float alpha = 0.08f + pulse * 0.07f; // 0.08 -> 0.15

        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        // Glow overlay at same size 
        drawBorderPieces(context, texture, x, y, width, height, border, secondHalf);

        // Reset color again
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    // Draws the 6 border pieces 
    private static void drawBorderPieces(DrawContext context,
                                         Identifier texture,
                                         int n, int o,
                                         int l, int m,
                                         int border, int secondHalf) {

        int texW = 128;
        int texH = 128;

        // left top corner
        context.drawTexture(texture, n - 6, o - 6,
                0 + secondHalf * 64, 0 + border * 16,
                8, 8, texW, texH);

        // right top corner
        context.drawTexture(texture, n + l - 2, o - 6,
                56 + secondHalf * 64, 0 + border * 16,
                8, 8, texW, texH);

        // left bottom corner
        context.drawTexture(texture, n - 6, o + m - 2,
                0 + secondHalf * 64, 8 + border * 16,
                8, 8, texW, texH);

        // right bottom corner
        context.drawTexture(texture, n + l - 2, o + m - 2,
                56 + secondHalf * 64, 8 + border * 16,
                8, 8, texW, texH);

        // middle header
        context.drawTexture(texture,
                (n - 6 + n + l + 6) / 2 - 24, o - 9,
                8 + secondHalf * 64, 0 + border * 16,
                48, 8, texW, texH);

        // bottom footer
        context.drawTexture(texture,
                (n - 6 + n + l + 6) / 2 - 24, o + m + 1,
                8 + secondHalf * 64, 8 + border * 16,
                48, 8, texW, texH);
    }

    // Smooth tint: gold -> white -> cyan -> back toward gold
    private static int samplePerfectTint(float t) {
        t = wrap01(t);

        if (t < 0.5f) {
            float f = t * 2.0f; // 0..1
            return mixColor(GOLD_COLOR, WHITE_COLOR, f);
        } else {
            float f = (t - 0.5f) * 2.0f; // 0..1
            int whiteToCyan = mixColor(WHITE_COLOR, CYAN_COLOR, f);
            // drift back a bit toward gold so the loop feels smooth
            return mixColor(whiteToCyan, GOLD_COLOR, f * 0.5f);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    private static float wrap01(float v) {
        v = v % 1.0f;
        if (v < 0f) v += 1.0f;
        return v;
    }
}
