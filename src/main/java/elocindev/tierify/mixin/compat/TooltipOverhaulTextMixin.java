package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.style.text.DefaultText;
import dev.xylonity.tooltipoverhaul.client.TooltipContext;
import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.util.Util;
import elocindev.tierify.screen.client.component.PerfectTierComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.util.math.Vec2f;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.Point;

@Mixin(DefaultText.class)
public class TooltipOverhaulTextMixin {

    // Strategy: Intercept the X-coordinate calculation.
    // The target method signature is derived from your compile error log:
    // (int, int, Point, TooltipComponent, TextRenderer, TooltipContext)
    @Redirect(
        method = "render(Ldev/xylonity/tooltipoverhaul/client/layer/LayerDepth;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;Lnet/minecraft/util/math/Vec2f;Ljava/awt/Point;Lnet/minecraft/text/Text;Lnet/minecraft/client/font/TextRenderer;)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/xylonity/tooltipoverhaul/util/Util;getTitleAlignmentX(IILjava/awt/Point;Lnet/minecraft/client/gui/tooltip/TooltipComponent;Lnet/minecraft/client/font/TextRenderer;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;)I"
        )
    )
    private int tierify$modifyTitleAlignment(
            // Arguments passed to Util.getTitleAlignmentX
            int x, 
            int y, 
            Point containerSize, 
            TooltipComponent component, 
            TextRenderer fontRenderer, 
            TooltipContext context,
            
            // Captured locals from the render method (we need 'pos' for absolute coordinates)
            LayerDepth depth,
            TooltipContext capturedCtx,
            Vec2f pos,
            Point capturedSize,
            Text rarity,
            TextRenderer capturedFont
    ) {
        // 1. Calculate default behavior
        int originalX = Util.getTitleAlignmentX(x, y, containerSize, component, fontRenderer, context);

        // 2. Check if this is the "Perfect" component
        if (component instanceof PerfectTierComponent) {
            
            // 3. Force Center Alignment
            // Logic: Absolute Left Edge + (Container Width - Component Width) / 2
            int absoluteLeft = (int) pos.x;
            int containerWidth = containerSize.x;
            int componentWidth = component.getWidth(fontRenderer);
            
            // Recalculate center
            return absoluteLeft + (containerWidth - componentWidth) / 2;
        }

        return originalX;
    }
}
