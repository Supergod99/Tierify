package elocindev.tierify.mixin.compat;

import dev.xylonity.tooltipoverhaul.client.TooltipRenderer;
import dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.List;

@Mixin(TooltipRenderer.class)
public interface TooltipRendererAccessor {
    @Accessor("LAYERS_MAIN")
    static List<ITooltipLayer> getLayersMain() {
        throw new AssertionError();
    }
}
