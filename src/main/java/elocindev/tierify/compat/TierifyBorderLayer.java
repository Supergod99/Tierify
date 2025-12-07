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
import net.minecraft.util.math.Vec2f; // [FIXED] Was class_241
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.awt.Point;

public class TierifyBorderLayer implements ITooltipLayer {

    @Override
    public void render(TooltipContext ctx, Vec2f pos, Point size, TooltipStyle style, Text rarity, TextRenderer font, CustomFrameData customFrame) {
        // 1. Check if item has a Tier
        if (!ctx.stack().hasNbt() || !ctx.stack().getSubNbt(Tierify.NBT_SUBTAG_KEY).contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            return;
        }

        // 2. Resolve the Tier Key (Standard or Perfect)
        String tierId = ctx.stack().getSubNbt(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = ctx.stack().getSubNbt(Tierify.NBT_SUBTAG_KEY).getBoolean("Perfect");
        String lookupKey = isPerfect ? "{BorderTier:\"tiered:perfect\"}" : "{Tier:\"" + tierId + "\"}";

        // 3. Find the matching Border Template
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

        // 4. Geometry Setup
        int x = (int) pos.x; // [FIXED] Was field_1343
        int y = (int) pos.y; // [FIXED] Was field_1342
        int width = size.x;
        int height = size.y;

        int index = match.getIndex();
        int secondHalf = index > 7 ? 1 : 0;
        if (index > 7) index -= 8;
        
        Identifier texture = match.getIdentifier();

        // 5. Draw the Tierify Border
        ctx.push(() -> {
            // Z-Level 3000 ensures we draw ON TOP of the Tooltip Overhaul background/icons
            ctx.translate(0.0f, 0.0f, LayerDepth.BACKGROUND_OVERLAY.getZ());
            
            DrawContext drawContext = ctx.graphics();
            int texW = 128;
            int texH = 128;

            // --- Drawing Logic (Copied from your TieredTooltip) ---
            
            // Corners
            drawContext.drawTexture(texture, x - 6, y - 6, 0 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            drawContext.drawTexture(texture, x + width - 2, y - 6, 56 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            drawContext.drawTexture(texture, x - 6, y + height - 2, 0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            drawContext.drawTexture(texture, x + width - 2, y + height - 2, 56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);

            // Edges (Stretched to fit Tooltip Overhaul dynamic size)
            // Top
            drawContext.drawTexture(texture, x + 2, y - 6, width - 4, 8, 8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
            // Bottom
            drawContext.drawTexture(texture, x + 2, y + height - 2, width - 4, 8, 8 + secondHalf * 64, 8 + index * 16, 48, 8, texW, texH);
            // Left
            drawContext.drawTexture(texture, x - 6, y + 2, 8, height - 4, 0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            // Right
            drawContext.drawTexture(texture, x + width - 2, y + 2, 8, height - 4, 56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            
            // Optional: Draw Middle Header "Gem" if width allows
            if (width > 48) {
                 drawContext.drawTexture(texture, x + (width / 2) - 24, y - 9, 8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
            }
        });
    }
}
