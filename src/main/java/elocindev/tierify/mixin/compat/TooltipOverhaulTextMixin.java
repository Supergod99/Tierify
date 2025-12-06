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

    /**
     * TARGETING FIX FOR SINYTRA CONNECTOR:
     * We manually specify the FORGE/MOJANG class names in the target string.
     * * Fabric Name: net.minecraft.client.gui.tooltip.TooltipComponent
     * Forge Name:  net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
     * * Fabric Name: net.minecraft.client.font.TextRenderer
     * Forge Name:  net.minecraft.client.gui.Font
     */
    /*
    @Redirect(
        method = "render(Ldev/xylonity/tooltipoverhaul/client/layer/LayerDepth;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;Lnet/minecraft/util/math/Vec2f;Ljava/awt/Point;Lnet/minecraft/text/Text;Lnet/minecraft/client/font/TextRenderer;)V",
        at = @At(
            value = "INVOKE",
            target = "Ldev/xylonity/tooltipoverhaul/util/Util;getTitleAlignmentX(IILjava/awt/Point;Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;Lnet/minecraft/client/gui/Font;Ldev/xylonity/tooltipoverhaul/client/TooltipContext;)I"
        )
    )
    private int tierify$modifyTitleAlignment(
            int x, 
            int y, 
            Point containerSize, 
            TooltipComponent component, 
            TextRenderer fontRenderer, 
            TooltipContext context,
            // Captured locals
            LayerDepth depth,
            TooltipContext capturedCtx,
            Vec2f pos,
            Point capturedSize,
            Text rarity,
            TextRenderer capturedFont
    ) {
        // 1. Run the original logic using the standard Fabric method call (Connector handles the runtime bridging here)
        int originalX = Util.getTitleAlignmentX(x, y, containerSize, component, fontRenderer, context);

        // 2. Custom "Perfect" Tier Logic
        if (component instanceof PerfectTierComponent) {
            
            int absoluteLeft = (int) pos.x;
            int containerWidth = containerSize.x;
            int componentWidth = component.getWidth(fontRenderer);
            
            // Center the element
            return absoluteLeft + (containerWidth - componentWidth) / 2;
        }

        return originalX;
    }
     */
}
