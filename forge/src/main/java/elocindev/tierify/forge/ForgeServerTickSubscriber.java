package elocindev.tierify.forge;

import draylar.tiered.api.SetBonusLogic;
import elocindev.tierify.server.SetBonusTickHandler;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "tiered", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeServerTickSubscriber {

    private ForgeServerTickSubscriber() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        var server = event.getServer();

        SetBonusTickHandler.endServerTick(
                server,
                server.getPlayerList().getPlayers(),
                ForgeTierifyConfig::enableArmorSetBonuses,
                SetBonusLogic::updatePlayerSetBonus,
                SetBonusLogic::updatePlayerSetBonus
        );
    }
}
