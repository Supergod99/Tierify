package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Shadow @Nullable protected Slot focusedSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // --- RESTORED: Render logic for inventories (where DrawContext.drawItemTooltip is ignored) ---
    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void tierify$drawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        // 1. Compatibility Check (Skip if Tooltip Overhaul is present)
        if (FabricLoader.getInstance().isModLoaded("tooltipoverhaul")) {
            return;
        }

        // 2. Validate Slot & Item
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            ItemStack stack = this.focusedSlot.getStack();
            
            // 3. Check for Tierify NBT
            if (Tierify.CLIENT_CONFIG.tieredTooltip && stack.hasNbt()) {
                NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
                
                if (tierTag != null) {
                    // 4. Resolve Lookup Key (Original Mod Logic)
                    String tier = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
                    String lookupKey = tierTag.getBoolean("Perfect") ? "tiered:perfect" : tier;

                    // 5. Match & Render
                    for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                        if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(lookupKey)) {
                            
                            // Get tooltip text (Vanilla logic)
                            List<Text> text = Screen.getTooltipFromItem(this.client, stack);
                            // Vanilla-like wrapping: cap width to the screen
                            int maxWidth = this.width - 16;
                            List<TooltipComponent> components = new ArrayList<>();
                            int dataInsertIndex = 1; // will be corrected after title is processed
                            
                            for (int lineIndex = 0; lineIndex < text.size(); lineIndex++) {
                                Text line = text.get(lineIndex);
                                // Wrap each line to the max width
                                List<OrderedText> wrapped = this.textRenderer.wrapLines(line, maxWidth);
                        
                                for (OrderedText ot : wrapped) {
                                    components.add(TooltipComponent.of(ot));
                                }
                                // TooltipData should be inserted after the full (possibly wrapped) title block
                                if (lineIndex == 0) {
                                    dataInsertIndex = components.size();
                                }
                            }
                            
                            stack.getTooltipData().ifPresent(data -> {
                                int idx = Math.min(Math.max(dataInsertIndex, 1), components.size());
                                components.add(idx, TooltipComponent.of(data));
                            });

                            // Render Custom Border
                            TieredTooltip.renderTieredTooltipFromComponents(
                                context, 
                                this.textRenderer, 
                                components, 
                                x, 
                                y, 
                                HoveredTooltipPositioner.INSTANCE, 
                                TierifyClient.BORDER_TEMPLATES.get(i)
                            );
                            
                            // Cancel vanilla render
                            ci.cancel(); 
                            return;
                        }
                    }
                }
            }
        }
    }
    
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
}
