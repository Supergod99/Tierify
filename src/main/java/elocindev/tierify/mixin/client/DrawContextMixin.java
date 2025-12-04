package elocindev.tierify.mixin.client;

import java.util.List;

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
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Shadow
    @Mutable
    @Final
    private MinecraftClient client;

    // 1. Capture the stack when drawItemTooltip is called (Standard rendering)
    @Inject(method = "drawItemTooltip", at = @At("HEAD"))
    private void captureStack(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        TierifyClient.CURRENT_TOOLTIP_STACK = stack;
    }

    // 2. Clear it afterwards
    @Inject(method = "drawItemTooltip", at = @At("RETURN"))
    private void releaseStack(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        TierifyClient.CURRENT_TOOLTIP_STACK = ItemStack.EMPTY;
    }

    // 3. Intercept the LOW-LEVEL drawTooltip. This receives the FINAL list of components
    //    (including Icons and Separators added by other mods).
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", 
            at = @At("HEAD"), cancellable = true)
    private void drawTooltipMixin(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo info) {
        
        ItemStack stack = TierifyClient.CURRENT_TOOLTIP_STACK;

        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack != null && !stack.isEmpty() && stack.hasNbt() && stack.getNbt().contains("Tiered")) {
            String nbtString = stack.getNbt().getCompound("Tiered").asString();
            
            // Check for Perfect Tier Override
            if (stack.getNbt().getCompound("Tiered").getBoolean("Perfect")) {
                 for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                    if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider("{BorderTier:\"tiered:perfect\"}")) {
                        TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                        
                        TieredTooltip.renderTieredTooltipFromComponents(
                            (DrawContext) (Object) this, 
                            textRenderer, 
                            components, 
                            x, 
                            y, 
                            positioner, 
                            TierifyClient.BORDER_TEMPLATES.get(i)
                        );
                        info.cancel();
                        return;
                    }
                 }
            }

            // Standard Tiers
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                boolean matchesDecider = !TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack) 
                                         && TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString);
                boolean matchesStack = TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack);

                if (matchesDecider) {
                    TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } 
                else if (matchesStack) {
                    TieredTooltip.renderTieredTooltipFromComponents(
                        (DrawContext) (Object) this, 
                        textRenderer, 
                        components, // Pass the original components list (preserving icons/separators)
                        x, 
                        y, 
                        positioner, 
                        TierifyClient.BORDER_TEMPLATES.get(i)
                    );
                    info.cancel();
                    break;
                }
            }
        }
    }
}
