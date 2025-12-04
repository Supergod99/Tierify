package elocindev.tierify.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow @Nullable protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // @Redirect is safer than @Inject with locals. It gives us the exact list passed by the game.
    @Redirect(method = "drawMouseoverTooltip", 
              at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"))
    private void redirectDrawTooltip(DrawContext context, TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y) {
        
        // We still need the stack to check for NBT tags
        ItemStack stack = ItemStack.EMPTY;
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            stack = this.focusedSlot.getStack();
        }

        if (Tierify.CLIENT_CONFIG.tieredTooltip && !stack.isEmpty() && stack.hasNbt() && stack.getNbt().contains("Tiered")) {
            
            // --- 1. PERFECT BORDER OVERRIDE ---
            NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (tierTag != null && tierTag.getBoolean("Perfect")) {

                for (int p = 0; p < TierifyClient.BORDER_TEMPLATES.size(); p++) {
                    BorderTemplate template = TierifyClient.BORDER_TEMPLATES.get(p);

                    if (template.containsDecider("{BorderTier:\"tiered:perfect\"}")) {
                        if (!template.containsStack(stack)) {
                            template.addStack(stack);
                        }
                        
                        renderTieredTooltip(context, textRenderer, text, data, x, y, template);
                        return;
                    }
                }
            }

            // --- 2. STANDARD TIER BORDER ---
            String nbtString = stack.getNbt().getCompound("Tiered").asString();
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                BorderTemplate template = TierifyClient.BORDER_TEMPLATES.get(i);
                
                if (!template.containsStack(stack) && template.containsDecider(nbtString)) {
                    template.addStack(stack);
                } else if (template.containsStack(stack)) {
                    renderTieredTooltip(context, textRenderer, text, data, x, y, template);
                    return;
                }
            }
        }

        // Fallback: If not tiered, run the original vanilla call
        context.drawTooltip(textRenderer, text, data, x, y);
    }

    // Helper method to keep the mixin clean and avoid code duplication
    private void renderTieredTooltip(DrawContext context, TextRenderer textRenderer, List<Text> text, Optional<TooltipData> data, int x, int y, BorderTemplate template) {
        List<TooltipComponent> list = new ArrayList<>();
        int wrapWidth = 350;

        for (int k = 0; k < text.size(); k++) {
            Text t = text.get(k);
            int width = textRenderer.getWidth(t);

            // Don't wrap title (k=0) or short lines
            if (k == 0 || width <= wrapWidth) {
                list.add(TooltipComponent.of(t.asOrderedText()));
            } else {
                List<OrderedText> wrapped = textRenderer.wrapLines(t, wrapWidth);
                for (OrderedText line : wrapped) {
                    list.add(TooltipComponent.of(line));
                }
            }
        }

        data.ifPresent(d -> {
            // Add bundle/modded data to the list
            // NOTE: If using Fabric TooltipComponentCallback, we might need manual conversion here,
            // but TooltipComponent.of(data) handles the vanilla cases (Bundle/Map).
            if (list.size() > 1) {
                list.add(1, TooltipComponent.of(d));
            } else {
                list.add(TooltipComponent.of(d));
            }
        });

        TieredTooltip.renderTieredTooltipFromComponents(
                context,
                textRenderer,
                list,
                x,
                y,
                HoveredTooltipPositioner.INSTANCE,
                template
        );
    }
}
