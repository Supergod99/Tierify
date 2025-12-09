package elocindev.tierify.mixin.compat;

import elocindev.tierify.Tierify;
import elocindev.tierify.screen.client.component.PerfectTierComponent;
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
public class TooltipOverhaulRenderMixin {

    @Inject(
        method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", 
        at = @At("HEAD")
    )
    private void tierify$injectPerfectComponent(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
        ItemStack stack = elocindev.tierify.TierifyClient.CURRENT_TOOLTIP_STACK;
        
        if (stack != null && !stack.isEmpty()) {
            NbtCompound nbt = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (nbt != null && nbt.getBoolean("Perfect")) {
                boolean exists = components.stream().anyMatch(c -> c instanceof PerfectTierComponent);
                if (!exists && components.size() >= 1) {
                    try {
                        components.add(1, new PerfectTierComponent());
                    } catch (UnsupportedOperationException e) {
                    }
                }
            }
        }
    }
}
