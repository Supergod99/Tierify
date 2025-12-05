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

    // --- TARGET 1: The standard method (Vanilla uses this) ---
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", 
            at = @At("HEAD"), cancellable = true)
    private void drawTooltipWithPositioner(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo info) {
        handleTooltipRender(textRenderer, components, x, y, positioner, info);
    }

    // --- TARGET 2: The simple method (Mods often use this) ---
    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;II)V", 
            at = @At("HEAD"), cancellable = true)
    private void drawTooltipSimple(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, CallbackInfo info) {
        // We pass 'null' for the positioner, the renderer handles this gracefully
        handleTooltipRender(textRenderer, components, x, y, null, info);
    }

    // --- SHARED LOGIC ---
    private void handleTooltipRender(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo info) {
        ItemStack stack = TierifyClient.CURRENT_TOOLTIP_STACK;

        // FAIL-SAFE: Active Retrieval if stack is missing
        if ((stack == null || stack.isEmpty()) && this.client.currentScreen instanceof HandledScreen) {
            HandledScreenAccessor screen = (HandledScreenAccessor) this.client.currentScreen;
            Slot focused = screen.getFocusedSlot();
            if (focused != null && focused.hasStack()) {
                stack = focused.getStack();
            }
        }

        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack != null && !stack.isEmpty()) {
            NbtCompound tieredTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            
            if (tieredTag != null) {
                // Check Perfect
                if (tieredTag.getBoolean("Perfect")) {
                    String perfectKey = "{BorderTier:\"tiered:perfect\"}";
                    attemptRender(perfectKey, textRenderer, components, x, y, positioner, info);
                    if (info.isCancelled()) return;
                }

                // Check Standard
                String tierId = tieredTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
                String lookupKey = "{Tier:\"" + tierId + "\"}";
                attemptRender(lookupKey, textRenderer, components, x, y, positioner, info);
            }
        }
    }

    private void attemptRender(String key, TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo info) {
        for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
            if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(key)) {
                // The renderer will handle layout, spacing, and the border
                TieredTooltip.renderTieredTooltipFromComponents(
                    (DrawContext) (Object) this, 
                    textRenderer, 
                    components, 
                    x, 
                    y, 
                    positioner, 
                    TierifyClient.BORDER_TEMPLATES.get(i)
                );
                info.cancel(); // Stop vanilla render
                return;
            }
        }
    }
}
