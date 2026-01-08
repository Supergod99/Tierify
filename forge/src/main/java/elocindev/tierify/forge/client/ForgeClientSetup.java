package elocindev.tierify.forge.client;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.compat.TooltipOverhaulCompatForge;
import elocindev.tierify.forge.registry.ForgeMenuTypes;
import elocindev.tierify.forge.screen.client.ReforgeScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeClientSetup {

    private ForgeClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            MenuScreens.register(ForgeMenuTypes.REFORGE.get(), ReforgeScreen::new);
            TooltipOverhaulCompatForge.init();
        });
    }
}
