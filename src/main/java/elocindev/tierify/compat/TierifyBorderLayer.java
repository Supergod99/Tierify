package elocindev.tierify.compat;

import dev.xylonity.tooltipoverhaul.client.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.client.style.TooltipStyle;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.item.ReforgeAddition;
import elocindev.tierify.screen.client.PerfectLabelAnimator;
import elocindev.tierify.screen.client.PerfectBorderRenderer;
import elocindev.tierify.util.SetBonusUtils;
import draylar.tiered.api.BorderTemplate;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec2f;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import java.awt.Point;

public class TierifyBorderLayer implements ITooltipLayer {

    private static final float SET_BONUS_LABEL_NUDGE_Y = 4.0f;

    @Override
    public void render(TooltipContext ctx, Vec2f pos, Point size, TooltipStyle style, Text rarity, TextRenderer font, CustomFrameData customFrame) {
        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;
        ItemStack stack = ctx.stack();
        // tiered items use tier id (or tiered:perfect)
        // Reforge materials use the item registry id 
        String lookupKey;
        boolean isPerfect = false;
    
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            String tierId = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
            isPerfect = tierTag.getBoolean("Perfect");
            lookupKey = isPerfect ? "tiered:perfect" : tierId;
        } else if (stack.getItem() instanceof ReforgeAddition) {
            lookupKey = Registries.ITEM.getId(stack.getItem()).toString();
        } else {
            return;
        }
    
        BorderTemplate match = null;
        if (TierifyClient.BORDER_TEMPLATES != null) {
            for (BorderTemplate template : TierifyClient.BORDER_TEMPLATES) {
                if (template.containsDecider(lookupKey)) {
                    match = template;
                    break;
                }
            }
        }
    
        if (match == null) return;
        final boolean isPerfectFinal = isPerfect;

        
        // Final reference for lambda use
        final BorderTemplate finalMatch = match;

        // 4. Setup Geometry
        final int x = (int) pos.x; 
        final int y = (int) pos.y; 
        final int width = size.x;
        final int height = size.y;

        final int startColor = match.getStartGradient();
        final int endColor = match.getEndGradient();

        int rawIndex = match.getIndex();
        final int secondHalf = rawIndex > 7 ? 1 : 0;
        final int index = rawIndex > 7 ? rawIndex - 8 : rawIndex;
        
        final Identifier texture = match.getIdentifier();

        // 5. Draw Sequence
        ctx.push(() -> {
            // LAYER 0: Move to Background Overlay Z-Depth (3000)
            ctx.translate(0.0f, 0.0f, LayerDepth.BACKGROUND_OVERLAY.getZ());
            
            DrawContext drawContext = ctx.graphics();
            
            // --- A. Draw Gradient Lines ---
            int i = x - 3;
            int j = y - 3;
            int k = width + 6;
            int l = height + 6;

            // Vertical Left
            drawContext.fillGradient(i, j + 1, i + 1, j + l - 1, 0, startColor, endColor);
            // Vertical Right
            drawContext.fillGradient(i + k - 1, j + 1, i + k, j + l - 1, 0, startColor, endColor);
            // Horizontal Top
            drawContext.fillGradient(i, j, i + k, j + 1, 0, startColor, startColor);
            // Horizontal Bottom
            drawContext.fillGradient(i, j + l - 1, i + k, j + l, 0, endColor, endColor);

            // --- B. Draw Texture Corners ---
            // LAYER 1: Move Z up slightly (+1.0)
            ctx.translate(0.0f, 0.0f, 1.0f);
            
            int texW = 128;
            int texH = 128;

            // Top Left
            drawContext.drawTexture(texture, x - 6, y - 6, 0 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            // Top Right
            drawContext.drawTexture(texture, x + width - 2, y - 6, 56 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            // Bottom Left
            drawContext.drawTexture(texture, x - 6, y + height - 2, 0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            // Bottom Right
            drawContext.drawTexture(texture, x + width - 2, y + height - 2, 56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);

            // Header Plate
            if (width >= 48) {
                 drawContext.drawTexture(texture, x + (width / 2) - 24, y - 9, 8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
            }
             // Footer Plate
             if (width >= 48) {
                 drawContext.drawTexture(texture, x + (width / 2) - 24, y + height + 1, 8 + secondHalf * 64, 8 + index * 16, 48, 8, texW, texH);
            }

            // --- C. Animated Perfect Overlay (Glow) ---
            // LAYER 2: Move Z up again (+1.0)
            ctx.push(() -> {
                ctx.translate(0.0f, 0.0f, 1.0f); 
                PerfectBorderRenderer.renderPerfectBorderOverlay(drawContext, finalMatch, x, y, width, height);
            });

            renderSetBonusActiveLabel(ctx, font, x, y, width, customFrame);
            // --- D. Draw "Perfect" Text (Centered) ---
            if (isPerfectFinal) {
                renderPerfectLabel(ctx, font, x, y, width);
            }
        });
    }

    private void renderSetBonusActiveLabel(TooltipContext ctx, TextRenderer font, int bgX, int bgY, int bgWidth, CustomFrameData customFrame) {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;
    
        MutableText label = SetBonusUtils.getSetBonusActiveLabel(client.player, ctx.stack());
        if (label == null) return;
    
        float scale = 0.65f;
    
        int textWidth = font.getWidth(label);
        float scaledWidth = textWidth * scale;
    
        ItemStack stack = ctx.stack();
        Text title = stack.getName();
        int titleWidth = font.getWidth(title);
    
        // TooltipOverhaul frame defaults:
        final float padding = 4.0f;
        final boolean iconDisabled = frameDisableIcon(customFrame);
        final float iconGutter = iconDisabled ? 0.0f : 20.0f; // 4px pad + 16px icon (matches prior intent)
    
        // Title region excludes the icon gutter when icon is enabled
        float contentLeft = bgX + padding + iconGutter;
        float contentRight = bgX + bgWidth - padding;
        float contentWidth = Math.max(0.0f, contentRight - contentLeft);
    
        String alignment = frameTitleAlignment(customFrame);
        float titleStartX;
        if ("middle".equalsIgnoreCase(alignment) || "center".equalsIgnoreCase(alignment)) {
            titleStartX = contentLeft + (contentWidth - titleWidth) / 2.0f;
        } else if ("right".equalsIgnoreCase(alignment)) {
            titleStartX = contentRight - titleWidth;
        } else {
            // left / default
            titleStartX = contentLeft;
        }
    
        float titleCenterX = titleStartX + (titleWidth / 2.0f);
        float xPos = titleCenterX - (scaledWidth / 2.0f);
    
        // Clamp: never enter icon gutter; never exceed tooltip bounds
        float minX = contentLeft; // critical: prevents overlap with icon column
        float maxX = bgX + bgWidth - scaledWidth - padding;
        xPos = Math.max(minX, Math.min(maxX, xPos));
    
        float baseHeight = 9f;
        float scaledHeight = baseHeight * scale;
    
        float topPadding = 4f;
        float gapTop = bgY - 3f;
        float gapBottom = bgY + topPadding;
    
        float yPos = gapTop + ((gapBottom - gapTop) - scaledHeight) / 2f;
        float yOffset = (baseHeight - scaledHeight) / 2f;
        yPos += yOffset;
        yPos += SET_BONUS_LABEL_NUDGE_Y;
    
        final float xPosFinal = xPos;
        final float yPosFinal = yPos;
        final float scaleFinal = scale;
    
        ctx.push(() -> {
            ctx.translate(xPosFinal, yPosFinal, LayerDepth.BACKGROUND_OVERLAY.getZ() + 10);
            ctx.scale(scaleFinal, scaleFinal, 1.0f);
            ctx.graphics().drawText(font, label, 0, 0, 0xFFFFFF, true);
        });
    }
    
        private static boolean frameDisableIcon(CustomFrameData frame) {
        if (frame == null) return false;
        try {
            var m = frame.getClass().getMethod("disableIcon"); // record accessor in many builds
            Object optObj = m.invoke(frame);
    
            if (optObj instanceof java.util.Optional<?> o) {
                if (o.isPresent()) {
                    Object v = o.get();
                    if (v instanceof Boolean b) return b;
                }
                return false;
            }
        } catch (Throwable ignored) {}
        return false;
    }
    
    private static String frameTitleAlignment(CustomFrameData frame) {
        if (frame == null) return "left";
        try {
            var m = frame.getClass().getMethod("titleAlignment");
            Object optObj = m.invoke(frame);
    
            if (optObj instanceof java.util.Optional<?> o) {
                if (o.isPresent()) {
                    Object v = o.get();
                    if (v instanceof String s && !s.isEmpty()) return s;
                }
                return "left";
            }
        } catch (Throwable ignored) {}
        return "left";
    }

    
    private void renderPerfectLabel(TooltipContext ctx, TextRenderer font, int bgX, int bgY, int bgWidth) {
        MutableText label = PerfectLabelAnimator.getPerfectLabel();
        float scale = 0.65f;
        int textWidth = font.getWidth(label);
        
        // Center text relative to the tooltip background width
        float centeredX = bgX + (bgWidth / 2.0f) - ((textWidth * scale) / 2.0f);
        
        // FIX: Moved up to 20.0f (was 24.0f) to center between Title and Description Line
        float fixedY = bgY + 22.0f; 

        // LAYER 3: Float the text well above the border
        ctx.push(() -> {
            ctx.translate(centeredX, fixedY, LayerDepth.BACKGROUND_OVERLAY.getZ() + 10);
            ctx.scale(scale, scale, 1.0f);
            ctx.graphics().drawText(font, label, 0, 0, 0xFFFFFF, true);
        });
    }
}
