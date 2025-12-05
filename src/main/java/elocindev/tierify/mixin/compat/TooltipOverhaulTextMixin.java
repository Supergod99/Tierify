package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.style.text.DefaultText;
import elocindev.tierify.screen.client.component.PerfectTierComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.Point;

@Mixin(DefaultText.class)
public class TooltipOverhaulTextMixin {

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            // We use the Intermediary signature here. 
            // class_5684 = TooltipComponent
            // method_32665 = drawText
            // class_327 = TextRenderer
            // class_4597$class_4598 = VertexConsumerProvider$Immediate
            target = "Lnet/minecraft/class_5684;method_32665(Lnet/minecraft/class_327;IILorg/joml/Matrix4f;Lnet/minecraft/class_4597$class_4598;)V",
            remap = true
        )
    )
    private void tierify$centerPerfectLabel(TooltipComponent instance, 
                                            TextRenderer textRenderer, 
                                            int x, int y, 
                                            org.joml.Matrix4f matrix, 
                                            VertexConsumerProvider.Immediate vertexConsumers, 
                                            dev.xylonity.tooltipoverhaul.client.layer.LayerDepth depth, 
                                            dev.xylonity.tooltipoverhaul.client.TooltipContext ctx, 
                                            Vec2f pos, 
                                            Point size) {
        
        int drawX = x;
        
        if (instance instanceof PerfectTierComponent) {

            
            int tooltipWidth = size.x;
            int componentWidth = instance.getWidth(textRenderer);
            

            int absoluteLeft = (int) pos.x;
            

            drawX = absoluteLeft + (tooltipWidth - componentWidth) / 2;
        }

        instance.drawText(textRenderer, drawX, y, matrix, vertexConsumers);
    }
}
