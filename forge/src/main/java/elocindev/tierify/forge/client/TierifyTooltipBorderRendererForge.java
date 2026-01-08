package elocindev.tierify.forge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public final class TierifyTooltipBorderRendererForge {

    private TierifyTooltipBorderRendererForge() {}

    public record Template(
            int index,
            ResourceLocation texture,
            int startGradient,
            int endGradient,
            int backgroundGradient,
            List<String> decider
    ) {}

    private static volatile List<Template> TEMPLATES = Collections.emptyList();

    public static void setTemplates(List<Template> templates) {
        TEMPLATES = (templates == null) ? Collections.emptyList() : List.copyOf(templates);
    }

    public static void render(
            GuiGraphics gg,
            int x, int y,
            int width, int height,
            String tierId,
            int tierIndex,
            boolean isPerfect
    ) {
        Template t = findTemplate(tierId, isPerfect);
        if (t == null) return;

        renderTooltipBackground(gg, x, y, width, height, t.backgroundGradient(), t.startGradient(), t.endGradient());

        // Overlay the Tiered-style border pieces (corners + centered top/bottom bar)
        if (t.texture() != null) {
            drawTieredBorderPieces(gg, t.texture(), x, y, width, height, t.index());
        }
    }

    public static void renderOverlay(
            GuiGraphics gg,
            int x, int y,
            int width, int height,
            Template t
    ) {
        if (t == null) return;
        int i = x - 3;
        int j = y - 3;
        int k = width + 6;
        int l = height + 6;

        renderBorder(gg, i, j + 1, k, l, t.startGradient(), t.endGradient());

        if (t.texture() != null) {
            drawTieredBorderPieces(gg, t.texture(), x, y, width, height, t.index());
        }
    }

    /**
     * Draws Tiered-style "border pieces" from a 128x128 sheet row = index*16:
     * - 4 corners (8x8) at u: 0/56, v: 0/8
     * - centered top & bottom bars (48x8) at u: 8, v: 0/8
     *
     * This avoids tiling corner art down the sides (your screenshots), while the gradient outline provides
     * continuous left/right/top/bottom lines.
     */
    private static void drawTieredBorderPieces(
            GuiGraphics gg,
            ResourceLocation texture,
            int x, int y,
            int width, int height,
            int index
    ) {
        final int TEX_W = 128;
        final int TEX_H = 128;

        // Each border style lives on its own 16px-tall row.
        int border = Math.max(0, index);
        int secondHalf = border > 7 ? 1 : 0;
        if (border > 7) {
            border -= 8;
        }

        final int uOff = secondHalf * 64;
        final int vOff = border * 16;

        // Corners (8x8)
        blit(gg, texture, x - 6,         y - 6,          0  + uOff, 0 + vOff, 8, 8, TEX_W, TEX_H); // TL
        blit(gg, texture, x + width - 2, y - 6,          56 + uOff, 0 + vOff, 8, 8, TEX_W, TEX_H); // TR
        blit(gg, texture, x - 6,         y + height - 2, 0  + uOff, 8 + vOff, 8, 8, TEX_W, TEX_H); // BL
        blit(gg, texture, x + width - 2, y + height - 2, 56 + uOff, 8 + vOff, 8, 8, TEX_W, TEX_H); // BR

        // Top + bottom center bars (48x8), centered.
        // (Vanilla tooltip minimum width is clamped to 64 in the mixin, so 48px fits cleanly.)
        int barX = x + (width / 2) - 24;
        blit(gg, texture, barX, y - 9,          8 + uOff, 0 + vOff, 48, 8, TEX_W, TEX_H); // top
        blit(gg, texture, barX, y + height + 1, 8 + uOff, 8 + vOff, 48, 8, TEX_W, TEX_H); // bottom
    }

    private static void blit(
            GuiGraphics gg,
            ResourceLocation texture,
            int x, int y,
            int u, int v,
            int w, int h,
            int texW, int texH
    ) {
        // Mojmaps 1.20.1: GuiGraphics#blit(ResourceLocation, int, int, int, int, int, int, int, int)
        gg.blit(texture, x, y, u, v, w, h, texW, texH);
    }

    private static void renderTooltipBackground(GuiGraphics gg, int x, int y, int width, int height, int backgroundColor, int startColor, int endColor) {
        int i = x - 3;
        int j = y - 3;
        int k = width + 6;
        int l = height + 6;

        renderHorizontalLine(gg, i, j - 1, k, backgroundColor);
        renderHorizontalLine(gg, i, j + l, k, backgroundColor);
        renderRectangle(gg, i, j, k, l, backgroundColor);
        renderVerticalLine(gg, i - 1, j, l, backgroundColor);
        renderVerticalLine(gg, i + k, j, l, backgroundColor);
        renderBorder(gg, i, j + 1, k, l, startColor, endColor);
    }

    private static void renderBorder(GuiGraphics gg, int x, int y, int width, int height, int startColor, int endColor) {
        renderVerticalLine(gg, x, y, height - 2, startColor, endColor);
        renderVerticalLine(gg, x + width - 1, y, height - 2, startColor, endColor);
        renderHorizontalLine(gg, x, y - 1, width, startColor);
        renderHorizontalLine(gg, x, y - 1 + height - 1, width, endColor);
    }

    private static void renderVerticalLine(GuiGraphics gg, int x, int y, int height, int color) {
        gg.fill(x, y, x + 1, y + height, color);
    }

    private static void renderVerticalLine(GuiGraphics gg, int x, int y, int height, int startColor, int endColor) {
        gg.fillGradient(x, y, x + 1, y + height, startColor, endColor);
    }

    private static void renderHorizontalLine(GuiGraphics gg, int x, int y, int width, int color) {
        gg.fill(x, y, x + width, y + 1, color);
    }

    private static void renderRectangle(GuiGraphics gg, int x, int y, int width, int height, int color) {
        gg.fill(x, y, x + width, y + height, color);
    }

    @Nullable
    public static Template findTemplate(String tierId, boolean isPerfect) {
        List<Template> list = TEMPLATES;
        if (list.isEmpty()) return null;

        // Priority: perfect border > tierId match > first entry
        if (isPerfect) {
            for (Template t : list) {
                if (t.decider() != null && t.decider().contains("tiered:perfect")) return t;
            }
        }

        for (Template t : list) {
            if (t.decider() != null && t.decider().contains(tierId)) return t;
        }

        return list.get(0);
    }
}
