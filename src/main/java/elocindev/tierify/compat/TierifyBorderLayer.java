package elocindev.tierify.compat;

import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
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
        // Closest equivalent to your previous "BACKGROUND_OVERLAY" intent.
        // This is where TooltipOverhaul draws overlays like the default frame overlay.
        return LayerDepth.OVERLAY;
    }

    @Override
    public void renderInternal(TooltipContext context) {
        // Match TooltipOverhaul 1.4's layer behavior: translate to the layer Z and restore pose afterwards.
        // This prevents subtle ordering issues between primitives (fillGradient) and textured border parts.
        context.push(() -> {
            context.translate(0.0F, 0.0F, this.getLayerDepth().getZ());
            context.setLayerDepth(this.getLayerDepth());
            this.render(context, context.getTooltipPosition());
        });
    }

    @Override
    public void render(TooltipContext ctx, Vec2f pos) {
        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;

        ItemStack stack = ctx.getStack();
        CustomFrameData frame = ctx.getFrameData(); // may be null
        TextRenderer font = ctx.getFont();
        DrawContext drawContext = ctx.getGraphics();

        // Determine lookup key (tiered id / tiered:perfect / reforge material id)
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

        // Find matching border template
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

        final BorderTemplate finalMatch = match;
        final boolean isPerfectFinal = isPerfect;
        // Tooltip geometry (TooltipOverhaul computes size/pos in TooltipContext)
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

        // Draw inside a pushed context so z ordering stays correct
        ctx.push(() -> {
            // Draw Sequence: gradient lines -> corner textures -> perfect overlay -> labels

            // A) Gradient outline (same geometry you used before)
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

            // TooltipOverhaul 1.4 batches GUI primitives and GUI textured quads into different render layers.
            // If we don't flush here, the batch order can cause the outline to appear over our corner/plate
            // textures, visually "cutting" into them.
            ctx.flush();

            // B) Texture corners / plates
            ctx.push(() -> {
                ctx.translate(0.0f, 0.0f, 1.0f);

                int texW = 128;
                int texH = 128;

                // Top Left
                drawContext.drawTexture(texture, x - 6, y - 6,
                        0 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
                // Top Right
                drawContext.drawTexture(texture, x + width - 2, y - 6,
                        56 + secondHalf * 64, 0 + index * 16, 8, 8, texW, texH);
                // Bottom Left
                drawContext.drawTexture(texture, x - 6, y + height - 2,
                        0 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);
                // Bottom Right
                drawContext.drawTexture(texture, x + width - 2, y + height - 2,
                        56 + secondHalf * 64, 8 + index * 16, 8, 8, texW, texH);

                // Header Plate / Footer Plate
                if (width >= 48) {
                    drawContext.drawTexture(texture, x + (width / 2) - 24, y - 9,
                            8 + secondHalf * 64, 0 + index * 16, 48, 8, texW, texH);
                    drawContext.drawTexture(texture, x + (width / 2) - 24, y + height + 1,
                            8 + secondHalf * 64, 8 + index * 16, 48, 8, texW, texH);
                }
            });

            // C) Animated Perfect Overlay
            ctx.push(() -> {
                ctx.translate(0.0f, 0.0f, 2.0f);
                PerfectBorderRenderer.renderPerfectBorderOverlay(drawContext, finalMatch, x, y, width, height);
            });

            // D) Set bonus label
            renderSetBonusActiveLabel(ctx, font, x, y, width, frame);

            // E) Perfect label
            if (isPerfectFinal) {
                renderPerfectLabel(ctx, font, x, y, width);
            }
        });
    }

    private void renderSetBonusActiveLabel(TooltipContext ctx, TextRenderer font, int bgX, int bgY, int bgWidth, CustomFrameData frame) {
        var client = MinecraftClient.getInstance();
        if (client.player == null) return;

        MutableText label = SetBonusUtils.getSetBonusActiveLabel(client.player, ctx.getStack());
        if (label == null) return;

        float scale = 0.65f;

        int textWidth = font.getWidth(label);
        float scaledWidth = textWidth * scale;

        ItemStack stack = ctx.getStack();
        Text title = stack.getName();
        int titleWidth = font.getWidth(title);

        // Use TooltipOverhaul’s computed padding so this stays aligned with 1.4’s layout fixes
        final float padding = ctx.getPaddingX();

        // TooltipOverhaul already knows if an icon is present (and accounts for disableIcon config/frame)
        final float iconGutter = ctx.hasIcon() ? 20.0f : 0.0f;

        float contentLeft = bgX + padding + iconGutter;
        float contentRight = bgX + bgWidth - padding;
        float contentWidth = Math.max(0.0f, contentRight - contentLeft);

        String alignment = "left";
        if (frame != null) {
            try {
                alignment = frame.titleAlignment().orElse("left");
            } catch (Throwable ignored) { }
        }

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

        // Clamp: never enter icon gutter; never exceed tooltip bounds
        float minX = contentLeft;
        float maxX = bgX + bgWidth - scaledWidth - padding;
        xPos = Math.max(minX, Math.min(maxX, xPos));

        float baseHeight = 9f;
        float scaledHeight = baseHeight * scale;

        float topPadding = ctx.getPaddingY();
        float gapTop = bgY - 3f;
        float gapBottom = bgY + topPadding;

        float yPos = gapTop + ((gapBottom - gapTop) - scaledHeight) / 2f;
        float yOffset = (baseHeight - scaledHeight) / 2f;
        yPos += yOffset;
        yPos += SET_BONUS_LABEL_NUDGE_Y;

        final float xPosFinal = xPos;
        final float yPosFinal = yPos;

        ctx.push(() -> {
            ctx.translate(xPosFinal, yPosFinal, 10.0f);
            ctx.scale(scale, scale, 1.0f);
            ctx.getGraphics().drawText(font, label, 0, 0, 0xFFFFFF, true);
        });
    }

    private void renderPerfectLabel(TooltipContext ctx, TextRenderer font, int bgX, int bgY, int bgWidth) {
        MutableText label = PerfectLabelAnimator.getPerfectLabel();
        float scale = 0.65f;
        int textWidth = font.getWidth(label);

        float centeredX = bgX + (bgWidth / 2.0f) - ((textWidth * scale) / 2.0f);

        // Keep your established placement
        float fixedY = bgY + 22.0f;

        ctx.push(() -> {
            ctx.translate(centeredX, fixedY, 10.0f);
            ctx.scale(scale, scale, 1.0f);
            ctx.getGraphics().drawText(font, label, 0, 0, 0xFFFFFF, true);
        });
    }
}
