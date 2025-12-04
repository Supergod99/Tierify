package elocindev.tierify.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
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

    @Inject(method = "drawItemTooltip", at = @At("HEAD"), cancellable = true)
    private void drawItemTooltipMixin(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {

        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack.hasNbt() && stack.getNbt().contains("Tiered")) {
            String nbtString = stack.getNbt().getCompound("Tiered").asString();
            
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                // Check if we need to render a custom border (either specifically for this stack or via decider)
                boolean matchesDecider = !TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack) 
                                         && TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString);
                boolean matchesStack = TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack);

                if (matchesDecider) {
                    TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } 
                else if (matchesStack) {
                    // 1. Get the raw text lines (Lore, Attributes, Name, etc.)
                    List<Text> text = Screen.getTooltipFromItem(client, stack);
                    List<TooltipComponent> list = new ArrayList<>();

                    // 2. THE FIX: Iterate and Wrap
                    // Instead of blindly converting to OrderedText, we check the width.
                    // 200 is a safe max width for tooltips (Vanilla is usually ~170-200).
                    int maxWidth = 200; 

                    for (Text t : text) {
                        // wrapLines returns List<OrderedText>. 
                        // If the line is short, it returns a list of size 1.
                        // If long, it splits it while PRESERVING styles/colors perfectly.
                        List<OrderedText> wrapped = textRenderer.wrapLines(t, maxWidth);
                        
                        for (OrderedText line : wrapped) {
                            list.add(TooltipComponent.of(line));
                        }
                    }

                    // 3. Re-inject TooltipData (like Bundles) at index 1 if it exists
                    stack.getTooltipData().ifPresent(data -> {
                        if (list.size() > 1) {
                            list.add(1, TooltipComponent.of(data));
                        } else {
                            list.add(TooltipComponent.of(data));
                        }
                    });

                    // 4. Render using your custom system
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
