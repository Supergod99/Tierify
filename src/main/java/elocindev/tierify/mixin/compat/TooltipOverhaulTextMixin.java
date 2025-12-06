package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.style.text.DefaultText;
import dev.xylonity.tooltipoverhaul.client.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.util.Util;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.Point;

@Mixin(DefaultText.class)
public class TooltipOverhaulTextMixin {


    @Redirect(
        method = "render(Ldev/xylonity/tooltipoverhaul/client/layer/LayerDepth;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;Lnet/minecraft/util/math/Vec2f;Ljava/awt/Point;Lnet/minecraft/text/Text;Lnet/minecraft/client/font/TextRenderer;)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/xylonity/tooltipoverhaul/util/Util;getTitleAlignmentX(Ldev/xylonity/tooltipoverhaul/client/TooltipContext;FF)I"
        )
    )
    private int tierify$modifyTitleAlignment(
            TooltipContext ctx, 
            float containerWidth, 
            float textWidth, 

            LayerDepth depth,
            TooltipContext ctx2,
            Vec2f pos,
            Point size,
            Text rarity,
            TextRenderer font
    ) {

        int originalX = Util.getTitleAlignmentX(ctx, containerWidth, textWidth);

 
        if (rarity == null) {
            return originalX;
        }


        if (rarity.getString().contains("Perfect")) {
            

            int absoluteLeft = (int) pos.x;
            int centeredX = absoluteLeft + (int)((containerWidth - textWidth) / 2.0f);
            
            return centeredX;
        }

        return originalX;
    }
}
