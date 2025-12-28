package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameData;
import dev.xylonity.tooltipoverhaul.client.frame.CustomFrameManager;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.item.ReforgeAddition;
import draylar.tiered.api.BorderTemplate;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;
import java.util.Optional;

@Mixin(CustomFrameManager.class)
public class TooltipOverhaulFrameMixin {

    @Inject(method = "of", at = @At("HEAD"), cancellable = true)
    private static void tierify$injectTieredFrame(ItemStack stack, CallbackInfoReturnable<Optional<CustomFrameData>> cir) {

        if (!Tierify.CLIENT_CONFIG.tieredTooltip) return;

        NbtCompound nbt = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (nbt == null) return;

        String lookupKey;
        boolean isPerfect = nbt.getBoolean("Perfect");
        
        if (isPerfect) {
            lookupKey = "tiered:perfect";
        } else {
            lookupKey = nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY);
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
        String endHex = tierify$intToHex(match.getEndGradient());
        String midHex = tierify$interpolateHex(match.getStartGradient(), match.getEndGradient());

        CustomFrameData frameData = new CustomFrameData(
            List.of(), // items
            List.of(), // tags
            Optional.empty(), // namespace
            Optional.empty(), // texture
            Optional.of(match.getBackgroundGradient()), 
            Optional.of(CustomFrameData.InnerBorderType.NONE), 
            Optional.of(CustomFrameData.GradientType.CUSTOM), 
            // Keep gradient colors for Divider Line
            Optional.of(List.of(startHex, midHex, endHex)), 
            Optional.empty(), // itemRating
            Optional.empty(), // colorItemRating
            Optional.empty(), // ratingAlignment
            Optional.of("middle"), // titleAlignment
            Optional.empty(), // titlePositionX
            Optional.empty(), // titlePositionY
            Optional.empty(), // ratingPositionX
            Optional.empty(), // ratingPositionY
            Optional.empty(), // tooltipDescriptionPositionX
            Optional.empty(), // tooltipDescriptionPositionY
            Optional.empty(), // mainPanelPaddingX
            Optional.empty(), // mainPanelPaddingY
            Optional.empty(), // iconSize
            Optional.empty(), // iconRotatingSpeed
            Optional.empty(), // iconAppearAnimation
            Optional.empty(), // secondPanelX
            Optional.empty(), // secondPanelY
            Optional.empty(), // secondPanelRendererSize
            Optional.empty(), // secondPanelRendererSpeed
            Optional.of(CustomFrameData.DividerLineType.NORMAL), // dividerLineType
            Optional.of(startHex), // dividerLineColor
            Optional.empty(), // particles
            isPerfect ? Optional.of("stars") : Optional.empty(), // specialEffect
            Optional.empty(), // iconBackgroundType
            Optional.empty(), // showSecondPanel
            Optional.empty(), // showRating
            Optional.of(false), // disableIcon
            Optional.empty(), // disableScrolling
            Optional.empty(), // disableTooltip
            Optional.empty()  // disableDividerLine
        );
        cir.setReturnValue(Optional.of(frameData));
    }

    @Unique
    private static String tierify$intToHex(int color) {
        return String.format("#%08X", color);
    }

    @Unique
    private static String tierify$interpolateHex(int c1, int c2) {
        int a = (((c1 >> 24) & 0xFF) + ((c2 >> 24) & 0xFF)) / 2;
        int r = (((c1 >> 16) & 0xFF) + ((c2 >> 16) & 0xFF)) / 2;
        int g = (((c1 >>  8) & 0xFF) + ((c2 >>  8) & 0xFF)) / 2;
        int b = (((c1      ) & 0xFF) + ((c2      ) & 0xFF)) / 2;
        return String.format("#%02X%02X%02X%02X", a, r, g, b);
    }
}
