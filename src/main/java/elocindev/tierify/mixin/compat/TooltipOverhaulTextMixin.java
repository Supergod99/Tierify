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

    @Redirect(
        method = "render(Ldev/xylonity/tooltipoverhaul/client/layer/LayerDepth;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;Lnet/minecraft/util/math/Vec2f;Ljava/awt/Point;Lnet/minecraft/text/Text;Lnet/minecraft/client/font/TextRenderer;)V",
        at = @At(
            value = "INVOKE",
            // FIX: Removed the complex signature "(IILjava/awt/Point;...)" 
            // We only specify the owner and the name. Mixin will match it by name, ignoring the mismatched class mappings.
            target = "Ldev/xylonity/tooltipoverhaul/util/Util;getTitleAlignmentX"
        )
    )
    private int tierify$modifyTitleAlignment(
            // Arguments passed to Util.getTitleAlignmentX
            // These MUST match the types the compiler asked for (from your previous error log)
            int x, 
            int y, 
            Point containerSize, 
            TooltipComponent component, 
            TextRenderer fontRenderer, 
            TooltipContext context,
            
            // Captured locals from the render method
            LayerDepth depth,
            TooltipContext capturedCtx,
            Vec2f pos,
            Point capturedSize,
            Text rarity,
            TextRenderer capturedFont
    ) {
        // 1. Calculate default behavior (so standard items are not broken)
        int originalX = Util.getTitleAlignmentX(x, y, containerSize, component, fontRenderer, context);

        // 2. Check if this is your custom "Perfect" component
        // Since we are intercepting the calculation BEFORE drawing, your custom component
        // will still handle the rendering (keeping your stars, gradients, and scaling intact).
        if (component instanceof PerfectTierComponent) {
            
            // 3. Force Center Alignment
            // Logic: Absolute Left Edge + (Container Width - Component Width) / 2
            int absoluteLeft = (int) pos.x;
            int containerWidth = containerSize.x;
            int componentWidth = component.getWidth(fontRenderer);
            
            // Calculate the absolute centered position
            return absoluteLeft + (containerWidth - componentWidth) / 2;
        }

        return originalX;
    }
}
