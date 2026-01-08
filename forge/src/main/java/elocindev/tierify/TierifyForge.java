package elocindev.tierify;

import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.loot.TierifyLootModifier;
import elocindev.tierify.forge.network.ForgeNetwork;
import elocindev.tierify.forge.registry.ForgeAttributeRegistry;
import elocindev.tierify.forge.registry.ForgeItemRegistry;
import elocindev.tierify.forge.registry.ForgeMenuTypes;
import elocindev.tierify.forge.registry.ForgeSoundRegistry;
import elocindev.tierify.platform.ForgePlatformHelper;
import elocindev.tierify.platform.Platform;
import com.mojang.serialization.Codec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TierifyCommon.MODID)
public final class TierifyForge {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TierifyCommon.MODID);

    @SuppressWarnings("unused")
    private static final RegistryObject<Codec<? extends IGlobalLootModifier>> TIERIFY_LOOT =
            LOOT_MODIFIERS.register("tierify_loot", () -> TierifyLootModifier.CODEC);

    public TierifyForge() {
        @SuppressWarnings("removal")
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeTierifyConfig.SPEC);

        Platform.init(new ForgePlatformHelper());
        ForgeItemRegistry.ITEMS.register(modBus);
        ForgeMenuTypes.MENUS.register(modBus);
        ForgeSoundRegistry.SOUND_EVENTS.register(modBus);
        ForgeAttributeRegistry.ATTRIBUTES.register(modBus);
        LOOT_MODIFIERS.register(modBus);

        ForgeNetwork.init();
    }
}


