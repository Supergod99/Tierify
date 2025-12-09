package elocindev.tierify.util;

import java.util.List;

import org.joml.Vector2ic;

import elocindev.tierify.screen.client.PerfectLabelAnimator;
import elocindev.tierify.screen.client.PerfectBorderRenderer;
import net.minecraft.text.MutableText;
import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.Tierify;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;

@Environment(EnvType.CLIENT)
public class TieredTooltip {

    public static String getPlateForModifier(String modifier) {
        switch(modifier.toLowerCase()) {
            case "common":
                return "\u00A77\u00A7r"+"\u00A7F\uFFA1\u00A7r";
            case "uncommon":
                return "\u00A72\u00A7r"+"\u00A7F\uFFA2\u00A7r";
            case "rare":
                return "\u00A73\u00A7r"+"\u00A7F\uFFA3\u00A7r";
            case "epic":
                return "\u00A75\u00A7r"+"\u00A7F\uFFA4\u00A7r";
            case "legendary":
                return "\u00A76\u00A7r"+"\u00A7F\uFFA5\u00A7r";
            case "mythic":
                return "\u00A74\u00A7r"+"\u00A7F\uFFA6\u00A7r";
            default:
                return modifier;
        }
    }

    public static void renderTieredTooltipFromComponents(DrawContext context, TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner,
            BorderTemplate borderTemplate) {
        TooltipComponent tooltipComponent2;
        int r;
        int k;
        if (components.isEmpty()) {
            return;
        }
        int i = 0;
        int j = components.size() == 1 ? -2 : 0;
        
        for (TooltipComponent tooltipComponent : components) {
            if (tooltipComponent == null) {
                continue;
            }
            k = tooltipComponent.getWidth(textRenderer);
            if (k > i) {
                i = k;
            }
            j += tooltipComponent.getHeight();
        }
        

        i += 8; // Horizontal Padding
        
        int topPadding = 4;
        int bottomPadding = 4;
        
        j += topPadding + bottomPadding; 
        
        // Extra height for Perfect label
        if (borderTemplate.getIndex() == 6) {    
            j += 12;
        }
        
        if (i < 64) {
            i = 64;
        }
        if (j < 16) {
            j = 16;
        }

        int l = i;
        int m = j;

        Vector2ic vector2ic = positioner.getPosition(context.getScaledWindowWidth(), context.getScaledWindowHeight(), x, y, l, m);
        int n = vector2ic.x();
        int o = vector2ic.y();
        context.getMatrices().push();

        int backgroundColor = borderTemplate.getBackgroundGradient();
        int colorStart = borderTemplate.getStartGradient();
        int colorEnd = borderTemplate.getEndGradient();

        int border = borderTemplate.getIndex();
        int secondHalf = border > 7 ? 1 : 0;
        if (border > 7) {
            border -= 8;
        }

        boolean isPerfectTooltip = (border == 6);

        renderTooltipBackground(context, n, o, l, m, 400, backgroundColor, colorStart, colorEnd);
        context.getMatrices().translate(0.0f, 0.0f, 400.0f);
        
        // Apply Top Padding to text start position
        int q = o + topPadding;

        for (r = 0; r < components.size(); ++r) {
            int nameCentering = 0;
            tooltipComponent2 = components.get(r);
        
            if (r == 0 && Tierify.CLIENT_CONFIG.centerName) {
                nameCentering = i / 2 - tooltipComponent2.getWidth(textRenderer) / 2;
            }
        
            if (r == 0 && isPerfectTooltip) {
                tooltipComponent2.drawText(
                        textRenderer,
                        n + nameCentering,
                        q,
                        context.getMatrices().peek().getPositionMatrix(),
                        context.getVertexConsumers()
                );
        
                q += tooltipComponent2.getHeight() + 2;
        
                MutableText perfectText = PerfectLabelAnimator.getPerfectLabel();
                float scale = 0.65f;
        
                int textWidth = textRenderer.getWidth(perfectText);
                float scaledWidth = textWidth * scale;
        
                float xPos = n + (l - scaledWidth) / 2f;
        
                float baseHeight = 9f;           
                float scaledHeight = baseHeight * scale;
                float yOffset = -(baseHeight - scaledHeight) / 2f; 
        
                context.getMatrices().push();
                context.getMatrices().translate(xPos, q + yOffset, 400f);
                context.getMatrices().scale(scale, scale, 1f);
        
                textRenderer.draw(
                        perfectText,
                        0,
                        0,
                        0xFFFFFF,
                        false,
                        context.getMatrices().peek().getPositionMatrix(),
                        context.getVertexConsumers(),
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        0xF000F0
                );
        
                context.getMatrices().pop();
        
                q += baseHeight + 3;
        
                continue;
            }
        
            // Shift X by +4 for left padding (unless centered)
            int xPos = (r == 0 && Tierify.CLIENT_CONFIG.centerName) ? (n + nameCentering) : (n + 4);

            tooltipComponent2.drawText(
                    textRenderer,
                    xPos,
                    q,
                    context.getMatrices().peek().getPositionMatrix(),
                    context.getVertexConsumers()
            );
        
            q += tooltipComponent2.getHeight() + (r == 0 ? 2 : 0);
        }

        // Reset Q for item rendering loop
        q = o + topPadding;

        for (r = 0; r < components.size(); ++r) {
            tooltipComponent2 = components.get(r);
            tooltipComponent2.drawItems(textRenderer, n + 4, q, context);
            q += tooltipComponent2.getHeight() + (r == 0 ? 2 : 0);
        }
        context.getMatrices().pop();

        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, 400.0f);

        // Standard Border Drawing
        int texW = 128;
        int texH = 128;
        
        context.drawTexture(borderTemplate.getIdentifier(), n - 6, o - 6, 0 + secondHalf * 64, 0 + border * 16, 8, 8, texW, texH);
        context.drawTexture(borderTemplate.getIdentifier(), n + l - 2, o - 6, 56 + secondHalf * 64, 0 + border * 16, 8, 8, texW, texH);
        context.drawTexture(borderTemplate.getIdentifier(), n - 6, o + m - 2, 0 + secondHalf * 64, 8 + border * 16, 8, 8, texW, texH);
        context.drawTexture(borderTemplate.getIdentifier(), n + l - 2, o + m - 2, 56 + secondHalf * 64, 8 + border * 16, 8, 8, texW, texH);
        context.drawTexture(borderTemplate.getIdentifier(), (n - 6 + n + l + 6) / 2 - 24, o - 9, 8 + secondHalf * 64, 0 + border * 16, 48, 8, texW, texH);
        context.drawTexture(borderTemplate.getIdentifier(), (n - 6 + n + l + 6) / 2 - 24, o + m + 1, 8 + secondHalf * 64, 8 + border * 16, 48, 8, texW, texH);
        
        context.getMatrices().push();
        context.getMatrices().translate(0.0f, 0.0f, 1000.0f);
        PerfectBorderRenderer.renderPerfectBorderOverlay(context, borderTemplate, n, o, l, m);
        context.getMatrices().pop();
    }


    private static void renderTooltipBackground(DrawContext context, int x, int y, int width, int height, int z, int backgroundColor, int colorStart, int colorEnd) {
        int i = x - 3;
        int j = y - 3;
        int k = width + 6;
        int l = height + 6;
        renderHorizontalLine(context, i, j - 1, k, z, backgroundColor);
        renderHorizontalLine(context, i, j + l, k, z, backgroundColor);
        renderRectangle(context, i, j, k, l, z, backgroundColor);
        renderVerticalLine(context, i - 1, j, l, z, backgroundColor);
        renderVerticalLine(context, i + k, j, l, z, backgroundColor);
        renderBorder(context, i, j + 1, k, l, z, colorStart, colorEnd);
    }

    private static void renderBorder(DrawContext context, int x, int y, int width, int height, int z, int startColor, int endColor) {
        renderVerticalLine(context, x, y, height - 2, z, startColor, endColor);
        renderVerticalLine(context, x + width - 1, y, height - 2, z, startColor, endColor);
        renderHorizontalLine(context, x, y - 1, width, z, startColor);
        renderHorizontalLine(context, x, y - 1 + height - 1, width, z, endColor);
    }

    private static void renderVerticalLine(DrawContext context, int x, int y, int height, int z, int color) {
        context.fill(x, y, x + 1, y + height, z, color);
    }

    private static void renderVerticalLine(DrawContext context, int x, int y, int height, int z, int startColor, int endColor) {
        context.fillGradient(x, y, x + 1, y + height, z, startColor, endColor);
    }

    private static void renderHorizontalLine(DrawContext context, int x, int y, int width, int z, int color) {
        context.fill(x, y, x + width, y + 1, z, color);
    }

    private static void renderRectangle(DrawContext context, int x, int y, int width, int height, int z, int color) {
        context.fill(x, y, x + width, y + height, z, color);
    }

}
