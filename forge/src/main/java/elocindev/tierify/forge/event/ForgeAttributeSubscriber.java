package elocindev.tierify.forge.event;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.registry.ForgeAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeAttributeSubscriber {

    private ForgeAttributeSubscriber() {}

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ForgeAttributeRegistry.CRIT_CHANCE.get());
        event.add(EntityType.PLAYER, ForgeAttributeRegistry.DIG_SPEED.get());
        event.add(EntityType.PLAYER, ForgeAttributeRegistry.DURABLE.get());
        event.add(EntityType.PLAYER, ForgeAttributeRegistry.RANGE_ATTACK_DAMAGE.get());
    }
}
