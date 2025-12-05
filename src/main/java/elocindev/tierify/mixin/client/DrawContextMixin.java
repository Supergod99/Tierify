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
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Shadow
    @Mutable
    @Final
    private MinecraftClient client;

    @Inject(method = "drawItemTooltip", at = @At("HEAD"))
    private void captureStack(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        TierifyClient.CURRENT_TOOLTIP_STACK = stack;
    }

    @Inject(method = "drawItemTooltip", at = @At("RETURN"))
    private void releaseStack(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        TierifyClient.CURRENT_TOOLTIP_STACK = ItemStack.EMPTY;
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", 
            at = @At("HEAD"), cancellable = true)
    private void drawTooltipMixin(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo info) {
        
        ItemStack stack = TierifyClient.CURRENT_TOOLTIP_STACK;
        boolean debug = false; // Toggle this if spam is too much, but we need it once.

        // --- FAIL-SAFE & DEBUGGING ---
        if ((stack == null || stack.isEmpty()) && this.client.currentScreen instanceof HandledScreen) {
            HandledScreenAccessor screen = (HandledScreenAccessor) this.client.currentScreen;
            Slot focused = screen.getFocusedSlot();
            if (focused != null && focused.hasStack()) {
                stack = focused.getStack();
                debug = true; // Only debug if we actually found something interesting
            }
        }

        if (debug) System.out.println("[Tierify Debug] Mixin Fired. Stack found: " + (stack != null ? stack.getName().getString() : "NULL"));

        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack != null && !stack.isEmpty()) {
            
            NbtCompound tieredTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            
            if (debug) System.out.println("[Tierify Debug] Tiered Tag Present: " + (tieredTag != null) + " | Tag: " + tieredTag);

            if (tieredTag != null) {
                
                // --- 1. PERFECT TIER CHECK ---
                boolean isPerfect = tieredTag.getBoolean("Perfect");
                if (debug && isPerfect) System.out.println("[Tierify Debug] Item is Perfect. Checking templates...");

                if (isPerfect) {
                    String perfectKey = "{BorderTier:\"tiered:perfect\"}";
                    
                    for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                        if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(perfectKey)) {
                            if (debug) System.out.println("[Tierify Debug] Perfect Template MATCHED at index " + i + ". Rendering...");
                            
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
                    if (debug) System.out.println("[Tierify Debug] Item was Perfect, but NO TEMPLATE matched key: " + perfectKey);
                }

                // --- 2. STANDARD TIER CHECK ---
                String tierId = tieredTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
                String lookupKey = "{Tier:\"" + tierId + "\"}";
                
                if (debug) System.out.println("[Tierify Debug] Looking for Standard Tier Key: " + lookupKey);

                for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                    if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(lookupKey)) {
                        if (debug) System.out.println("[Tierify Debug] Standard Template MATCHED at index " + i);
                        
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
                if (debug) System.out.println("[Tierify Debug] NO TEMPLATE matched Standard Key: " + lookupKey);
            }
        } else {
             // Often fires for air or empty slots, don't spam console here unless necessary
        }
    }
}
