package elocindev.tierify.mixin.client;

import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DrawContext.class)
public class TieredDrawContextMixin {

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At("HEAD"), cancellable = true)
    private void renderTieredTooltip(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
      
        if (FabricLoader.getInstance().isModLoaded("tooltipoverhaul")) {
            return;
        }

        if (!Tierify.CLIENT_CONFIG.tieredTooltip) {
            return;
        }

        ItemStack stack = TierifyClient.CURRENT_TOOLTIP_STACK;
        if (stack.isEmpty() || !stack.hasNbt()) {
            return;
        }

        NbtCompound nbt = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (nbt == null) {
            return;
        }
        
        String tierId = nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = nbt.getBoolean("Perfect");
        String lookupKey = isPerfect ? "{BorderTier:\"tiered:perfect\"}" : "{Tier:\"" + tierId + "\"}";
        BorderTemplate borderTemplate = null;
        for (BorderTemplate template : TierifyClient.BORDER_TEMPLATES) {
            if (template.containsDecider(lookupKey)) {
                borderTemplate = template;
                break;
            }
        }
        if (borderTemplate != null) {
            TieredTooltip.renderTieredTooltipFromComponents((DrawContext) (Object) this, textRenderer, components, x, y, positioner, borderTemplate);
            ci.cancel(); 
        }
    }
}
