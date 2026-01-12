package elocindev.tierify.forge.mixin.compat;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "dev.xylonity.tooltipoverhaul.client.wrap.TooltipWrapper")
public class TooltipOverhaulWrapperMixin {

    @Inject(method = "wrap", at = @At("HEAD"), cancellable = true)
    private static void tierify$skipWrap(Font font,
                                         List<ClientTooltipComponent> orig,
                                         int screenWidth,
                                         ItemStack stack,
                                         CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        List<ClientTooltipComponent> wrapped = wrapPreservingTitle(font, stack, orig, screenWidth, false);
        if (wrapped != null) {
            cir.setReturnValue(wrapped);
        }
    }

    @Inject(method = "wrapHalf", at = @At("HEAD"), cancellable = true)
    private static void tierify$skipWrapHalf(Font font,
                                             List<ClientTooltipComponent> orig,
                                             int screenWidth,
                                             ItemStack stack,
                                             CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        List<ClientTooltipComponent> wrapped = wrapPreservingTitle(font, stack, orig, screenWidth, true);
        if (wrapped != null) {
            cir.setReturnValue(wrapped);
        }
    }

    private static List<ClientTooltipComponent> wrapPreservingTitle(Font font,
                                                                    ItemStack stack,
                                                                    List<ClientTooltipComponent> orig,
                                                                    int screenWidth,
                                                                    boolean halfScreen) {
        if (!ForgeTierifyConfig.tieredTooltip()) return null;
        if (stack == null || stack.isEmpty()) return null;
        if (orig == null || orig.isEmpty()) return null;

        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return null;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if ((tierId == null || tierId.isEmpty()) && !tiered.getBoolean("Perfect")) return null;

        int paddingX = resolveTooltipOverhaulPaddingX();
        int basePadding = paddingX * 2 + 4;
        int iconPadding = stack.isEmpty() ? 0 : 26;
        int maxAllowed = Math.max(60, (halfScreen ? screenWidth / 2 - 8 : (int) (screenWidth * 0.75F)) - basePadding - iconPadding);

        for (ClientTooltipComponent component : orig) {
            if (component.getWidth(font) > maxAllowed) {
                return wrapTailPreserveTitle(font, orig, screenWidth, stack, halfScreen);
            }
        }

        return orig;
    }

    private static int resolveTooltipOverhaulPaddingX() {
        try {
            Class<?> renderer = Class.forName("dev.xylonity.tooltipoverhaul.client.TooltipRenderer");
            java.lang.reflect.Field field = renderer.getField("PADDING_X");
            Object value = field.get(null);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
        }
        return 4;
    }

    private static List<ClientTooltipComponent> wrapTailPreserveTitle(Font font,
                                                                      List<ClientTooltipComponent> orig,
                                                                      int screenWidth,
                                                                      ItemStack stack,
                                                                      boolean halfScreen) {
        if (orig.size() <= 1) {
            return orig;
        }

        List<ClientTooltipComponent> tail = tierify$wrapInternal(font, orig.subList(1, orig.size()), screenWidth, stack, halfScreen);
        if (tail == null || tail.isEmpty()) {
            return orig;
        }

        List<ClientTooltipComponent> out = new ArrayList<>(1 + tail.size());
        out.add(orig.get(0));
        out.addAll(tail);
        return out;
    }

    @Invoker("wrapInternal")
    private static List<ClientTooltipComponent> tierify$wrapInternal(Font font,
                                                                     List<ClientTooltipComponent> orig,
                                                                     int screenWidth,
                                                                     ItemStack stack,
                                                                     boolean halfScreen) {
        throw new AssertionError("Invoker should be patched by Mixin.");
    }
}
