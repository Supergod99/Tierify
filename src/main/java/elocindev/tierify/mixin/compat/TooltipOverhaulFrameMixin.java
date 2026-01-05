package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameManager;
import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.item.ReforgeAddition;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

@Mixin(value = CustomFrameManager.class, remap = false)
public class TooltipOverhaulFrameMixin {

    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void tierify$injectTieredFrame(ItemStack stack, CallbackInfoReturnable<Optional<CustomFrameData>> cir) {
        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;

        String lookupKey;
        boolean isPerfect = false;

        NbtCompound nbt = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (nbt != null && nbt.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            isPerfect = nbt.getBoolean("Perfect");
            lookupKey = isPerfect ? "tiered:perfect" : nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY);
        } else if (stack.getItem() instanceof ReforgeAddition) {
            lookupKey = Registries.ITEM.getId(stack.getItem()).toString();
        } else {
            return;
        }

        BorderTemplate match = null;
        if (TierifyClient.BORDER_TEMPLATES != null) {
            for (BorderTemplate template : TierifyClient.BORDER_TEMPLATES) {
                if (template.containsDecider(lookupKey)) {
                    match = template;
                    break;
                }
            }
        }
        if (match == null) return;

        String startHex = tierify$intToHex(match.getStartGradient());
        String endHex   = tierify$intToHex(match.getEndGradient());
        String midHex   = tierify$interpolateHex(match.getStartGradient(), match.getEndGradient());

        // TooltipOverhaul 1.4.0: InnerBorderType/DividerLineType enums are gone.
        // borderType/dividerLineType are Optional<String> now. :contentReference[oaicite:5]{index=5}
        CustomFrameData frameData = new CustomFrameData(
                List.of(),                         // items
                List.of(),                         // tags
                Optional.empty(),                  // namespace
                Optional.empty(),                  // texture
                Optional.of(match.getBackgroundGradient()), // backgroundColor
                Optional.of("none"),               // borderType (disables inner overlay)
                Optional.of(CustomFrameData.GradientType.CUSTOM), // gradientType
                Optional.of(List.of(startHex, midHex, endHex)),   // gradientColors

                Optional.empty(),                  // itemRating
                Optional.empty(),                  // colorItemRating
                Optional.empty(),                  // ratingAlignment
                Optional.of("middle"),             // titleAlignment (matches your prior behavior)

                Optional.empty(),                  // tooltipPositionX
                Optional.empty(),                  // tooltipPositionY
                Optional.empty(),                  // mainPanelPaddingX
                Optional.empty(),                  // mainPanelPaddingY
                Optional.empty(),                  // dividerLineTopPadding
                Optional.empty(),                  // dividerLineBottomPadding

                Optional.empty(),                  // iconSize (currently disabled upstream per changelog)
                Optional.empty(),                  // iconRotatingSpeed
                Optional.empty(),                  // iconAppearAnimation

                Optional.empty(),                  // secondPanelX
                Optional.empty(),                  // secondPanelY
                Optional.empty(),                  // secondPanelSizeX
                Optional.empty(),                  // secondPanelSizeY
                Optional.empty(),                  // secondPanelRendererSpeed

                Optional.of("static"),             // dividerLineType (closest to old “NORMAL”)
                Optional.of(startHex),             // dividerLineColor
                Optional.empty(),                  // particles
                isPerfect ? Optional.of("stars") : Optional.empty(), // specialEffect (if you want it)

                List.of(),                         // vignettes
                Optional.empty(),                  // iconBackgroundType
                Optional.empty(),                  // usePlayerSkinInPreview
                Optional.empty(),                  // previewPanelModel
                Optional.empty(),                  // showSecondPanel
                Optional.empty(),                  // showRating
                Optional.empty(),                  // showShadow
                Optional.of(false),                // disableIcon
                Optional.empty(),                  // disableScrolling
                Optional.empty(),                  // disableTooltip
                Optional.empty()                   // disableDividerLine
        );

        cir.setReturnValue(Optional.of(frameData));
    }

    @Unique
    private static String tierify$intToHex(int argb) {
        // TooltipOverhaul 1.4 uses hex strings like "#FFFFFF". :contentReference[oaicite:6]{index=6}
        return String.format("#%06X", (argb & 0xFFFFFF));
    }

    @Unique
    private static String tierify$interpolateHex(int c1, int c2) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (r1 + r2) / 2, g = (g1 + g2) / 2, b = (b1 + b2) / 2;
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
