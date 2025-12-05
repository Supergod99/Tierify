package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.mixin.GuiGraphicsMixin;
import elocindev.tierify.Tierify;
import elocindev.tierify.screen.client.component.PerfectTierComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(GuiGraphicsMixin.class)
public class TooltipOverhaulRenderMixin {

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void tierify$injectPerfectComponent(TextRenderer font, List<TooltipComponent> components, int mouseX, int mouseY, net.minecraft.client.gui.tooltip.TooltipPositioner tooltipPositioner, CallbackInfo ci) {
        // Retrieve the stack tracked by our HandledScreenMixin
        ItemStack stack = elocindev.tierify.TierifyClient.CURRENT_TOOLTIP_STACK;
        
        if (stack != null && !stack.isEmpty()) {
            NbtCompound nbt = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (nbt != null && nbt.getBoolean("Perfect")) {
                // Insert our custom component at index 1 (under the title)
                // Ensure we don't duplicate it if it's already there (rare case with re-entry)
                boolean exists = components.stream().anyMatch(c -> c instanceof PerfectTierComponent);
                if (!exists && components.size() >= 1) {
                    components.add(1, new PerfectTierComponent());
                }
            }
        }
    }
}
