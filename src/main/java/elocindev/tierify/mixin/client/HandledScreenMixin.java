package elocindev.tierify.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.screen.client.PerfectLabelAnimator;
import elocindev.tierify.screen.client.PerfectBorderRenderer;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow @Nullable protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // 1. Capture the Stack (Keep this, it's good for other things)
    @Inject(method = "render", at = @At("HEAD"))
    private void captureHandledStack(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            TierifyClient.CURRENT_TOOLTIP_STACK = this.focusedSlot.getStack();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void releaseHandledStack(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        TierifyClient.CURRENT_TOOLTIP_STACK = ItemStack.EMPTY;
    }

    // 2. FORCE RENDER OVERLAY (The Fix)
    // This runs after the screen (and tooltips) are drawn.
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V", shift = At.Shift.AFTER))
    private void renderPerfectOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        
        if (this.focusedSlot == null || !this.focusedSlot.hasStack()) return;

        ItemStack stack = this.focusedSlot.getStack();
        NbtCompound tieredTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);

        // Check if it's a Perfect Item
        if (tieredTag != null && tieredTag.getBoolean("Perfect")) {
            
            TextRenderer textRenderer = this.client.textRenderer;
            
            // Calculate approximate tooltip position (Standard Minecraft Logic)
            int i = mouseX + 12;
            int j = mouseY - 12;
            int width = this.width;
            int height = this.height;
            
            // Adjust for screen edges (basic clamping)
            if (i + 100 > width) {
                i -= 28 + 100; // Shift left if too far right
            }
            if (j + 20 > height) {
                j = height - 20;
            }

            // Draw the Perfect Label
            // We push Z-Level to 1000 to ensure it draws ON TOP of Legendary Tooltips
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, 1000); 

            MutableText perfectText = PerfectLabelAnimator.getPerfectLabel();
            float scale = 0.75f; // Slightly larger for visibility

            // Draw the "Perfect" Text
            context.getMatrices().push();
            context.getMatrices().translate(i + 4, j + 2, 0); // Position inside tooltip box
            context.getMatrices().scale(scale, scale, 1f);
            
            context.drawText(textRenderer, perfectText, 0, 0, 0xFFFFFF, true);
            
            context.getMatrices().pop();
            context.getMatrices().pop();
        }
    }
}
