package elocindev.tierify.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.TierifyClient;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Shadow
    @Mutable
    @Final
    private MinecraftClient client;

    @Inject(method = "drawItemTooltip", 
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"), 
            cancellable = true)
    private void drawItemTooltipMixin(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {

        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack.hasNbt() && stack.getNbt().contains("Tiered")) {
            String nbtString = stack.getNbt().getCompound("Tiered").asString();
            
            // --- FIX: Generate list/data manually here to prevent 'InjectionError' ---
            // This is safe in DrawContext because it's a low-level render call
            List<Text> text = net.minecraft.client.gui.screen.Screen.getTooltipFromItem(this.client, stack);
            Optional<TooltipData> data = stack.getTooltipData();

            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                boolean matchesDecider = !TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack) 
                                         && TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString);
                boolean matchesStack = TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack);

                if (matchesDecider) {
                    TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } 
                else if (matchesStack) {
                    List<TooltipComponent> list = new ArrayList<>();

                    // --- SMART WRAP (Width 350) ---
                    int wrapWidth = 350; 

                    for (int k = 0; k < text.size(); k++) {
                        Text t = text.get(k);
                        int width = textRenderer.getWidth(t);

                        // Don't wrap title (k=0) or short lines.
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
                        if (list.size() > 1) {
                            list.add(1, TooltipComponent.of(d));
                        } else {
                            list.add(TooltipComponent.of(d));
                        }
                    });

                    TieredTooltip.renderTieredTooltipFromComponents(
                        (DrawContext) (Object) this, 
                        textRenderer, 
                        list, 
                        x, 
                        y, 
                        HoveredTooltipPositioner.INSTANCE, 
                        TierifyClient.BORDER_TEMPLATES.get(i)
                    );

                    info.cancel();
                    break;
                }
            }
        }
    }
}
