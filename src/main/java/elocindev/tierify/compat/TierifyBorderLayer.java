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
        // 1. Safety Check: Ensure stack has NBT before accessing it
        if (!ctx.stack().hasNbt()) return;

        NbtCompound tierTag = ctx.stack().getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag == null || !tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            return;
        }

        // 2. Resolve the Tier Key
        String tierId = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = tierTag.getBoolean("Perfect");
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

        // 4. Geometry Setup (Must be FINAL for use inside the lambda)
        final int x = (int) pos.x; 
        final int y = (int) pos.y; 
        final int width = size.x;
        final int height = size.y;

        int rawIndex = match.getIndex();
        final int secondHalf = rawIndex > 7 ? 1 : 0;
        final int index = rawIndex > 7 ? rawIndex - 8 : rawIndex;
        
        final Identifier texture = match.getIdentifier();

        // 5. Draw the Tierify Border
        ctx.push(() -> {
            // Draw at Z-Level 3000 to overlay on top of Tooltip Overhaul's background
            ctx.translate(0.0f, 0.0f, LayerDepth.BACKGROUND_OVERLAY.getZ());
            
            DrawContext drawContext = ctx.graphics();
            
            // Texture dimensions matching your 128x128 sheet
            int texW = 128;
            int texH = 128;

            // --- Drawing Logic ---
            // The texture is split into:
            // 0-8: Top Row (Corners/Edges)
            // 8-16: Bottom Row (Corners/Edges)
            // Offsets are calculated based on the 128x128 grid logic from your original mod.

            // Top Left Corner
            drawContext.drawTexture(texture, x - 6, y - 6, 0 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            // Top Right Corner
            drawContext.drawTexture(texture, x + width - 2, y - 6, 56 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
            // Bottom Left Corner
            drawContext.drawTexture(texture, x - 6, y + height - 2, 0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            // Bottom Right Corner
            drawContext.drawTexture(texture, x + width - 2, y + height - 2, 56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);

            // Top Edge (Stretches the middle 48px segment)
            drawContext.drawTexture(texture, x + 2, y - 6, width - 4, 8, 8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
            
            // Bottom Edge
            drawContext.drawTexture(texture, x + 2, y + height - 2, width - 4, 8, 8 + secondHalf * 64, 8 + index * 16, 48, 8, texW, texH);
            
            // Left Edge (Uses the left 8px column, bottom row)
            drawContext.drawTexture(texture, x - 6, y + 2, 8, height - 4, 0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            
            // Right Edge (Uses the right 8px column, bottom row)
            drawContext.drawTexture(texture, x + width - 2, y + 2, 8, height - 4, 56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
            
            // Optional "Gem" Header (Only draws if tooltip is wide enough)
            if (width > 48) {
                 drawContext.drawTexture(texture, x + (width / 2) - 24, y - 9, 8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
            }
        });
    }
}
