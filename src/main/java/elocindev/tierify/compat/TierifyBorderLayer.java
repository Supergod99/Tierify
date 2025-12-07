package elocindev.tierify.compat;

import dev.xylonity.tooltipoverhaul.client.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.client.style.TooltipStyle;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import draylar.tiered.api.BorderTemplate;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec2f;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.awt.Point;

public class TierifyBorderLayer implements ITooltipLayer {

    @Override
    public void render(TooltipContext ctx, Vec2f pos, Point size, TooltipStyle style, Text rarity, TextRenderer font, CustomFrameData customFrame) {
        // 1. Safety Check
        if (!ctx.stack().hasNbt()) return;

        NbtCompound tierTag = ctx.stack().getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag == null || !tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            return;
        }

        // 2. Resolve Tier
        String tierId = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = tierTag.getBoolean("Perfect");
        String lookupKey = isPerfect ? "{BorderTier:\"tiered:perfect\"}" : "{Tier:\"" + tierId + "\"}";

        // 3. Find Template
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

        // 4. Setup Geometry
        final int x = (int) pos.x; 
        final int y = (int) pos.y; 
        final int width = size.x;
        final int height = size.y;

        // Calculate Border Colors
        final int startColor = match.getStartGradient();
        final int endColor = match.getEndGradient();

        // Calculate Texture Index
        int rawIndex = match.getIndex();
        final int secondHalf = rawIndex > 7 ? 1 : 0;
        final int index = rawIndex > 7 ? rawIndex - 8 : rawIndex;
        
        final Identifier texture = match.getIdentifier();

        // 5. Draw
        ctx.push(() -> {
            // Z-Level 3000 draws on top of everything
            ctx.translate(0.0f, 0.0f, LayerDepth.BACKGROUND_OVERLAY.getZ());
            
            DrawContext drawContext = ctx.graphics();
            
            // --- A. Draw Gradient Lines (The "Connectors") ---
            // [FIX] Expanded offsets from -3 to -5 to align with the fancy corners.
            // This pushes the lines outward so they connect with the pixel art.
            
            int borderX = x - 5;
            int borderY = y - 5;
            int borderW = width + 10;
            int borderH = height + 10;

            // Vertical Left
            drawContext.fillGradient(borderX, borderY + 1, borderX + 1, borderY + borderH - 1, 400, startColor, endColor);
            // Vertical Right
            drawContext.fillGradient(borderX + borderW - 1, borderY + 1, borderX + borderW, borderY + borderH - 1, 400, startColor, endColor);
            // Horizontal Top
            drawContext.fillGradient(borderX, borderY, borderX + borderW, borderY + 1, 400, startColor, startColor);
            // Horizontal Bottom
            drawContext.fillGradient(borderX, borderY + borderH - 1, borderX + borderW, borderY + borderH, 400, endColor, endColor);


            // --- B. Draw Texture Corners & Header (The "Fancy" Bits) ---
            // These draw at -6, so the -5 lines will sit perfectly inside them.
            int texW = 128;
            int texH = 128;
            
            int cX = x;
            int cY = y;
            int cW = width;
            int cH = height;

            // Top Left Corner
            drawContext.drawTexture(texture, cX - 6, cY - 6, 0 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            // Top Right Corner
            drawContext.drawTexture(texture, cX + cW - 2, cY - 6, 56 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            // Bottom Left Corner
            drawContext.drawTexture(texture, cX - 6, cY + cH - 2, 0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            // Bottom Right Corner
            drawContext.drawTexture(texture, cX + cW - 2, cY + cH - 2, 56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);

            // Header Plate (Centered "Gem")
            if (cW >= 48) {
                 drawContext.drawTexture(texture, cX + (cW / 2) - 24, cY - 9, 8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
            }
            
            // Footer Plate (Centered)
             if (cW >= 48) {
                 drawContext.drawTexture(texture, cX + (cW / 2) - 24, cY + cH + 1, 8 + secondHalf * 64, 8 + index * 16, 48, 8, texW, texH);
            }
        });
    }
}
