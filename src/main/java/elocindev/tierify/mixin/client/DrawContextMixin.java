package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Inject(method = "drawItemTooltip", at = @At("HEAD"))
    private void tierify$captureStackForTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        TierifyClient.CURRENT_TOOLTIP_STACK = stack;
    }
    
    @Inject(method = "drawItemTooltip", at = @At("RETURN"))
    private void tierify$clearStackForTooltip(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo ci) {
        TierifyClient.CURRENT_TOOLTIP_STACK = ItemStack.EMPTY;
    }

    @Inject(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void tierify$drawTooltipFromComponents(
        TextRenderer textRenderer,
        List<TooltipComponent> components,
        int x, int y,
        TooltipPositioner positioner,
        CallbackInfo ci
    ) {
        if (FabricLoader.getInstance().isModLoaded("tooltipoverhaul")) return;
    
        ItemStack stack = TierifyClient.CURRENT_TOOLTIP_STACK;
        if (stack == null || stack.isEmpty()) return;
    
        if (!Tierify.CLIENT_CONFIG.tieredTooltip || !stack.hasNbt()) return;
    
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag == null) return;
    
        String tier = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
        String lookupKey = tierTag.getBoolean("Perfect") ? "tiered:perfect" : tier;
    
        for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
            if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(lookupKey)) {
                TieredTooltip.renderTieredTooltipFromComponents(
                    (DrawContext)(Object)this,
                    textRenderer,
                    components,
                    x,
                    y,
                    positioner,
                    TierifyClient.BORDER_TEMPLATES.get(i)
                );
                ci.cancel();
                return;
            }
        }
    }
}
