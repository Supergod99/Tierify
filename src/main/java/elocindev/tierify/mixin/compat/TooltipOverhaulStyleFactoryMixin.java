package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.style.StyleFactory;
import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
import elocindev.tierify.compat.TierifyBorderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = StyleFactory.class, remap = false)
public class TooltipOverhaulStyleFactoryMixin {

    @Inject(method = "create", at = @At("RETURN"))
    private void tierify$addTierifyBorderLayer(TooltipContext context, CustomFrameData data,
                                              CallbackInfoReturnable<List<ITooltipLayer>> cir) {
        List<ITooltipLayer> layers = cir.getReturnValue();
        if (layers == null) return;

        // Our layer self-guards (returns immediately if stack is not Tierify/Material/etc),
        // so it is safe to always append it here.
        layers.add(new TierifyBorderLayer());
    }
}
