package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.style.StyleFactory;
import elocindev.tierify.Tierify;
import elocindev.tierify.compat.TierifyBorderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = StyleFactory.class, remap = false)
public class TooltipOverhaulStyleFactoryMixin {

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private void tierify$addTierifyBorderLayer(TooltipContext context, Object data, CallbackInfoReturnable<List<ITooltipLayer>> cir) {
        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;

        List<ITooltipLayer> original = cir.getReturnValue();
        if (original == null) return;

        // Copy to avoid mutating TooltipOverhaul's list assumptions.
        List<ITooltipLayer> layers = new ArrayList<>(original);
        layers.add(new TierifyBorderLayer());
        cir.setReturnValue(layers);
    }
}
