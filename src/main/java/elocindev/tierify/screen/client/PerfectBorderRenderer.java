package elocindev.tierify.screen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.api.BorderTemplate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class PerfectBorderRenderer {
    
    private static boolean loggedOnce = false;
    // 4 second loop
    private static final long TINT_PERIOD_MS = 4000L;
    // ~3 second glow pulse
    private static final long GLOW_PERIOD_MS = 3000L;

    // Cosmic palette to match Perfect label:
    // Electric Violet → Radiant Teal → Starlight Silver
    private static final int COSMIC_1 = 0xA400FF;
    private static final int COSMIC_2 = 0x00F5CC;
    private static final int COSMIC_3 = 0xE6F7FF;

    /**
     * Renders animated tint + glow overlay for the Perfect border.
     *
     * Assumes:
     *  - The caller pushed the matrix stack and set Z high enough.
     *  - The base border has already been drawn.
     */
    public static void renderPerfectBorderOverlay(DrawContext context,
                                                  BorderTemplate borderTemplate,
                                                  int x, int y,
                                                  int width, int height) {

        // Only apply to the Perfect border (index 6 in your tooltip_borders.json)
        // DEBUG: detect if the overlay is ever triggered
        if (borderTemplate.getIndex() != 6) {
            return;
        }
        
        // One-time debug print
        if (!loggedOnce) {
            System.out.println("[Tierify DEBUG] Perfect border overlay triggered for item!");
            loggedOnce = true;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        long time = System.currentTimeMillis();

        // Recompute border row / half like TieredTooltip
        int border = borderTemplate.getIndex();
        int secondHalf = border > 7 ? 1 : 0;
        if (border > 7) {
            border -= 8;
        }

        Identifier texture = borderTemplate.getIdentifier();

        // -----------------------------
        // 1) Animated Cosmic Tint Layer
        // -----------------------------
        float phase = (time % TINT_PERIOD_MS) / (float) TINT_PERIOD_MS; // 0..1

        int tintColor = samplePerfectTint(phase);
        float r = ((tintColor >> 16) & 0xFF) / 255.0f;
        float g = ((tintColor >> 8) & 0xFF) / 255.0f;
        float b = (tintColor & 0xFF) / 255.0f;

        // Make this much stronger so it’s clearly visible
        float tintStrength = 0.45f; // 45% toward cosmic color

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

        // -----------------------------
        // 2) Stronger Glow Overlay
        // -----------------------------
        float glowPhase = (time % GLOW_PERIOD_MS) / (float) GLOW_PERIOD_MS;
        float pulse = 0.5f - 0.5f * (float) Math.cos(glowPhase * (float) Math.PI * 2.0f);

        // Much stronger alpha swing so it’s unmissable
        float alpha = 0.2f + pulse * 0.35f; // 0.2 → 0.55

        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        // Glow overlay at same size for now (can expand later if you like)
        drawBorderPieces(context, texture, x, y, width, height, border, secondHalf);

        // Reset & cleanup
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // Draws the 6 border pieces exactly like your TieredTooltip
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

    // Smooth tint over cosmic palette
    private static int samplePerfectTint(float t) {
        t = wrap01(t);

        if (t < 0.5f) {
            float f = t * 2.0f;
            return mixColor(COSMIC_1, COSMIC_2, f);
        } else {
            float f = (t - 0.5f) * 2.0f;
            return mixColor(COSMIC_2, COSMIC_3, f);
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
