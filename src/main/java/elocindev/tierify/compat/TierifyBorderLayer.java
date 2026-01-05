package elocindev.tierify.compat;

import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.util.PositionUtils;
import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.item.ReforgeAddition;
import elocindev.tierify.screen.client.PerfectBorderRenderer;
import elocindev.tierify.screen.client.PerfectLabelAnimator;
import elocindev.tierify.util.SetBonusUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;

public class TierifyBorderLayer implements ITooltipLayer {

    private static final float SET_BONUS_LABEL_NUDGE_Y = 4.0f;

    @Override
    public LayerDepth getLayerDepth() {
        // TooltipOverhaul 1.4 uses explicit depths; our border should behave like an overlay layer.
        return LayerDepth.OVERLAY;
    }

    @Override
    public void render(TooltipContext ctx, Vec2f pos) {
        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;

        ItemStack stack = ctx.getStack();
        if (stack == null || stack.isEmpty()) return;

        // Tiered items use tier id (or tiered:perfect)
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
        final BorderTemplate finalMatch = match;

        // Geometry from TooltipOverhaul 1.4 context
        Vec2f size = ctx.getTooltipSize();
        final int x = (int) pos.x;
        final int y = (int) pos.y;
        final int width = (int) size.x;
        final int height = (int) size.y;

        final int startColor = match.getStartGradient();
        final int endColor = match.getEndGradient();

        int rawIndex = match.getIndex();
        final int secondHalf = rawIndex > 7 ? 1 : 0;
        final int index = rawIndex > 7 ? rawIndex - 8 : rawIndex;

        final Identifier texture = match.getIdentifier();

        ctx.push(() -> {
            // Draw at OVERLAY depth (TooltipOverhaul 1.4)
            ctx.translate(0.0f, 0.0f, (float) LayerDepth.OVERLAY.getZ());

            DrawContext drawContext = ctx.getGraphics();

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
            ctx.push(() -> {
                ctx.translate(0.0f, 0.0f, 1.0f);
                PerfectBorderRenderer.renderPerfectBorderOverlay(drawContext, finalMatch, x, y, width, height);
            });

            // Set bonus label (above title area)
            renderSetBonusActiveLabel(ctx, x, y, width);

            // --- D. Draw "Perfect" Text ---
            if (isPerfectFinal) {
                renderPerfectLabel(ctx, x, y, width);
            }
        });
    }

    private void renderSetBonusActiveLabel(TooltipContext ctx, int bgX, int bgY, int bgWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        ItemStack stack = ctx.getStack();
        if (stack == null || stack.isEmpty()) return;

        MutableText label = SetBonusUtils.getSetBonusActiveLabel(client.player, stack);
        if (label == null) return;

        TextRenderer font = ctx.getTextRenderer();

        float scale = 0.65f;
        int textWidth = font.getWidth(label);
        float scaledWidth = textWidth * scale;

        Text title = stack.getName();
        int titleWidth = font.getWidth(title);

        // Use TooltipOverhaul’s computed padding + icon presence
        float paddingX = (float) ctx.getPaddingX();
        float paddingY = (float) ctx.getPaddingY();

        // Icon gutter is effectively 16px icon + 4px pad in TooltipOverhaul’s default layout.
        float iconGutter = ctx.hasIcon() ? 20.0f : 0.0f;

        float contentLeft = bgX + paddingX + iconGutter;
        float contentRight = bgX + bgWidth - paddingX;
        float contentWidth = Math.max(0.0f, contentRight - contentLeft);

        // Match TooltipOverhaul alignment decisions
        String alignment = PositionUtils.getTitleTextAlignment(ctx);
        float titleStartX;
        if ("middle".equalsIgnoreCase(alignment) || "center".equalsIgnoreCase(alignment)) {
            titleStartX = contentLeft + (contentWidth - titleWidth) / 2.0f;
        } else if ("right".equalsIgnoreCase(alignment)) {
            titleStartX = contentRight - titleWidth;
        } else {
            titleStartX = contentLeft;
        }

        float titleCenterX = titleStartX + (titleWidth / 2.0f);
        float xPos = titleCenterX - (scaledWidth / 2.0f);

        // Clamp within the title content region
        float minX = contentLeft;
        float maxX = bgX + bgWidth - scaledWidth - paddingX;
        xPos = Math.max(minX, Math.min(maxX, xPos));

        float baseHeight = 9.0f;
        float scaledHeight = baseHeight * scale;

        // Place within the top padding band
        float gapTop = bgY - 3.0f;
        float gapBottom = bgY + paddingY;

        float yPos = gapTop + ((gapBottom - gapTop) - scaledHeight) / 2.0f;
        float yOffset = (baseHeight - scaledHeight) / 2.0f;
        yPos += yOffset;
        yPos += SET_BONUS_LABEL_NUDGE_Y;

        final float xPosFinal = xPos;
        final float yPosFinal = yPos;

        ctx.push(() -> {
            ctx.translate(xPosFinal, yPosFinal, (float) LayerDepth.OVERLAY.getZ() + 10.0f);
            ctx.scale(scale, scale, 1.0f);
            ctx.getGraphics().drawText(font, label, 0, 0, 0xFFFFFF, true);
        });
    }

    private void renderPerfectLabel(TooltipContext ctx, int bgX, int bgY, int bgWidth) {
        TextRenderer font = ctx.getTextRenderer();

        MutableText label = PerfectLabelAnimator.getPerfectLabel();
        float scale = 0.65f;
        int textWidth = font.getWidth(label);

        float centeredX = bgX + (bgWidth / 2.0f) - ((textWidth * scale) / 2.0f);

        // Preserve your established placement (between title and divider region)
        float fixedY = bgY + 22.0f;

        ctx.push(() -> {
            ctx.translate(centeredX, fixedY, (float) LayerDepth.OVERLAY.getZ() + 10.0f);
            ctx.scale(scale, scale, 1.0f);
            ctx.getGraphics().drawText(font, label, 0, 0, 0xFFFFFF, true);
        });
    }
}
