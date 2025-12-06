package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.style.text.DefaultText;
import dev.xylonity.tooltipoverhaul.client.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Vec2f;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.awt.Point;

@Mixin(DefaultText.class)
public class TooltipOverhaulTextMixin {

    // Strategy: Intercept the integer 'x' variable immediately after it is calculated.
    // This BYPASSES the method signature crashes entirely because we target the 'STORE' opcode,
    // which doesn't require complex class descriptors.
    @ModifyVariable(
        method = "render(Ldev/xylonity/tooltipoverhaul/client/layer/LayerDepth;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;Lnet/minecraft/util/math/Vec2f;Ljava/awt/Point;Lnet/minecraft/text/Text;Lnet/minecraft/client/font/TextRenderer;)V",
        at = @At("STORE"),
        ordinal = 0 // Targets the first integer stored (which is the 'x' position)
    )
    private int tierify$centerPerfectLabel(
            int originalX, 
            LayerDepth depth,
            TooltipContext ctx,
            Vec2f pos,
            Point size,
            Text rarity,
            TextRenderer font
    ) {
        if (rarity == null) {
            return originalX;
        }


        if (rarity.getString().contains("Perfect")) {
            

            int absoluteLeft = (int) pos.x;
            int containerWidth = size.x;

            int textWidth = font.getWidth(rarity);
            

            return absoluteLeft + (containerWidth - textWidth) / 2;
        }

        return originalX;
    }
}
