package elocindev.tierify.forge.reforge;

import elocindev.tierify.TierifyCommon;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeReforgeReloadListener {
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent e) {
        e.addListener(new ForgeReforgeData.Loader());
    }
    private ForgeReforgeReloadListener() {}
}
