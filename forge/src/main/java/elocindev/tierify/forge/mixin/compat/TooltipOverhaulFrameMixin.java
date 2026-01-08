package elocindev.tierify.forge.mixin.compat;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.client.TierifyTooltipBorderRendererForge;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;

@Pseudo
@Mixin(targets = "dev.xylonity.tooltipoverhaul.client.frame.CustomFrameManager", remap = false)
public class TooltipOverhaulFrameMixin {

    @Inject(method = "of", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tierify$injectTieredFrame(ItemStack stack, CallbackInfoReturnable<Optional<?>> cir) {
        if (!ForgeTierifyConfig.tieredTooltip()) return;

        CompoundTag nbt = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (nbt == null) return;

        boolean isPerfect = nbt.getBoolean("Perfect");
        String tierId = nbt.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if ((tierId == null || tierId.isEmpty()) && !isPerfect) return;

        TierifyTooltipBorderRendererForge.Template match = TierifyTooltipBorderRendererForge.findTemplate(tierId, isPerfect);
        if (match == null) return;

        Object frameData = buildFrameData(match, isPerfect);
        if (frameData == null) return;

        cir.setReturnValue(Optional.of(frameData));
    }

    @Unique
    private static Object buildFrameData(TierifyTooltipBorderRendererForge.Template match, boolean isPerfect) {
        try {
            Class<?> dataClass = Class.forName("dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData");

            Object innerBorderNone = enumConstant(dataClass, "InnerBorderType", "NONE");
            Object gradientCustom = enumConstant(dataClass, "GradientType", "CUSTOM");
            Object dividerNormal = enumConstant(dataClass, "DividerLineType", "NORMAL");

            String startHex = intToHex(match.startGradient());
            String endHex = intToHex(match.endGradient());
            String midHex = interpolateHex(match.startGradient(), match.endGradient());

            List<String> gradient = List.of(startHex, midHex, endHex);

            Object[] args = new Object[] {
                    List.of(),
                    List.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(match.backgroundGradient()),
                    innerBorderNone == null ? Optional.empty() : Optional.of(innerBorderNone),
                    gradientCustom == null ? Optional.empty() : Optional.of(gradientCustom),
                    Optional.of(gradient),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("middle"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    dividerNormal == null ? Optional.empty() : Optional.of(dividerNormal),
                    Optional.of(startHex),
                    Optional.empty(),
                    isPerfect ? Optional.of("stars") : Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(false),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            };

            Constructor<?> ctor = findCtor(dataClass, args.length);
            if (ctor == null) return null;
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static Constructor<?> findCtor(Class<?> dataClass, int count) {
        for (Constructor<?> ctor : dataClass.getDeclaredConstructors()) {
            if (ctor.getParameterCount() == count) return ctor;
        }
        return null;
    }

    @Unique
    private static Object enumConstant(Class<?> dataClass, String nested, String name) {
        for (Class<?> inner : dataClass.getDeclaredClasses()) {
            if (!inner.isEnum()) continue;
            if (!inner.getSimpleName().equals(nested)) continue;
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumClass = (Class<? extends Enum>) inner;
            return Enum.valueOf(enumClass, name);
        }
        return null;
    }

    @Unique
    private static String intToHex(int color) {
        return String.format("#%08X", color);
    }

    @Unique
    private static String interpolateHex(int c1, int c2) {
        int a = (((c1 >> 24) & 0xFF) + ((c2 >> 24) & 0xFF)) / 2;
        int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF)) / 2;
        int g = (((c1 >>  8) & 0xFF) + ((c2 >>  8) & 0xFF)) / 2;
        int b = (((c1      ) & 0xFF) + ((c2      ) & 0xFF)) / 2;
        return String.format("#%02X%02X%02X%02X", a, r, g, b);
    }
}
