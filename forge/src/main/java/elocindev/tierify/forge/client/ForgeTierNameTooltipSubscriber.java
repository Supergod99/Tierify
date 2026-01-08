package elocindev.tierify.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NOTE:
 * Tooltip name prefixing is handled by the ItemStackClientMixin (getHoverName injection),
 * and Set Bonus / Perfect labels are rendered by the GuiGraphicsTooltipBorderMixin.
 *
 * This subscriber is intentionally left as a no-op to avoid double-inserting
 * label lines into the vanilla tooltip text list.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeTierNameTooltipSubscriber {

    private ForgeTierNameTooltipSubscriber() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        // no-op (kept for compatibility / easy re-enable if needed)
    }
}
