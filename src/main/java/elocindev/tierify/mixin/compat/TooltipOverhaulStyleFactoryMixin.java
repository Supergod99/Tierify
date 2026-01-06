package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import dev.xylonity.tooltipoverhaul.client.layer.impl.InnerOverlayLayer;
import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.style.StyleFactory;
import dev.xylonity.tooltipoverhaul.client.style.effect.StarsEffect;

import draylar.tiered.api.BorderTemplate;

import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.compat.TierifyBorderLayer;
import elocindev.tierify.compat.tto.PerfectGoldStarsEffect;
import elocindev.tierify.item.ReforgeAddition;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = StyleFactory.class, remap = false)
public class TooltipOverhaulStyleFactoryMixin {

    @Inject(
            method = "create(Ldev/xylonity/tooltipoverhaul/client/render/TooltipContext;Ldev/xylonity/tooltipoverhaul/client/frame/CustomFrameData;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void tierify$fixDoubleBorderOutline(TooltipContext context, CustomFrameData frameData, CallbackInfoReturnable<List<ITooltipLayer>> cir) {
        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;

        List<ITooltipLayer> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        // Only modify TooltipOverhaul rendering when THIS stack is one of ours (i.e., it matches a Tierify border template).
        if (!tierify$matchesTierifyBorder(context.getStack())) return;

        List<ITooltipLayer> layers = new ArrayList<>(original);

        // TooltipOverhaul 1.4 always adds an InnerOverlayLayer (GradientInnerOverlay / StaticInnerOverlay).
        // That inner 1px outline stacks with Tierify's outline and makes 3 sides look thicker (left side is often obscured by icon layers).
        // Removing it restores the pre-update look where Tierify fully owns the border.
        layers.removeIf(layer -> layer instanceof InnerOverlayLayer);

        // Perfect: use Tierify's gold, smaller, more frequent stars ONLY when Tierify is using its DEFAULT effects.
        // If the user enables override or template-index test mode, keep TooltipOverhaul's built-in stars untouched.
        if (tierify$isPerfect(context.getStack())
                && (Tierify.CLIENT_CONFIG.ttoSpecialEffectOverride == null || Tierify.CLIENT_CONFIG.ttoSpecialEffectOverride.isBlank())
                && !Tierify.CLIENT_CONFIG.ttoSpecialEffectByTemplateIndex) {
            // Remove TooltipOverhaul's default StarsEffect layer and replace with our gold variant.
            layers.removeIf(layer -> layer instanceof StarsEffect);
            layers.add(new PerfectGoldStarsEffect());
        }

        // Add our custom Tierify border renderer.
        layers.add(new TierifyBorderLayer());

        cir.setReturnValue(layers);
    }

    private static boolean tierify$isPerfect(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        return tierTag != null && tierTag.getBoolean("Perfect");
    }

    private static boolean tierify$matchesTierifyBorder(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // Determine lookup key (tiered id / tiered:perfect / reforge material id)
        String lookupKey;
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);

        if (tierTag != null && tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            String tierId = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
            boolean isPerfect = tierTag.getBoolean("Perfect");
            lookupKey = isPerfect ? "tiered:perfect" : tierId;
        } else if (stack.getItem() instanceof ReforgeAddition) {
            lookupKey = Registries.ITEM.getId(stack.getItem()).toString();
        } else {
            return false;
        }

        if (TierifyClient.BORDER_TEMPLATES == null) return false;

        for (BorderTemplate template : TierifyClient.BORDER_TEMPLATES) {
            if (template != null && template.containsDecider(lookupKey)) {
                return true;
            }
        }
        return false;
    }
}
