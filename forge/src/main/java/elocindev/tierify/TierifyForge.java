package elocindev.tierify;

import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.config.EntityLootDropProfiles;
import elocindev.tierify.forge.config.ReforgeMaterialLootProfiles;
import elocindev.tierify.forge.config.TreasureBagProfiles;
import elocindev.tierify.forge.loot.TierifyLootModifier;
import elocindev.tierify.forge.network.ForgeNetwork;
import elocindev.tierify.forge.registry.ForgeAttributeRegistry;
import elocindev.tierify.forge.registry.ForgeItemRegistry;
import elocindev.tierify.forge.registry.ForgeMenuTypes;
import elocindev.tierify.forge.registry.ForgeSoundRegistry;
import elocindev.tierify.platform.ForgePlatformHelper;
import elocindev.tierify.platform.Platform;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(TierifyCommon.MODID)
public final class TierifyForge {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TierifyCommon.MODID);
    private static final String DEFAULT_COMMON_CONFIG = "echelon-defaults/echelon-common.toml";
    private static final String DEFAULT_CLIENT_CONFIG = "echelon-defaults/echelon-client.toml";
    private static final String DEFAULT_ENTITY_PROFILES = "echelon-defaults/echelon-entity-drop-profiles.txt";
    private static final String DEFAULT_REFORGE_MATERIAL_PROFILES = "echelon-defaults/echelon-reforge-material-profiles.txt";
    private static final String DEFAULT_TREASURE_BAG_PROFILES = "echelon-defaults/echelon-treasure-bag-profiles.txt";

    @SuppressWarnings("unused")
    private static final RegistryObject<Codec<? extends IGlobalLootModifier>> TIERIFY_LOOT =
            LOOT_MODIFIERS.register("tierify_loot", () -> TierifyLootModifier.CODEC);

    public TierifyForge() {
        @SuppressWarnings("removal")
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ensureDefaultConfig("echelon-common.toml", DEFAULT_COMMON_CONFIG);
        ensureDefaultConfig("echelon-client.toml", DEFAULT_CLIENT_CONFIG);
        ensureDefaultConfig("echelon-entity-drop-profiles.txt", DEFAULT_ENTITY_PROFILES);
        ensureDefaultConfig("echelon-reforge-material-profiles.txt", DEFAULT_REFORGE_MATERIAL_PROFILES);
        ensureDefaultConfig("echelon-treasure-bag-profiles.txt", DEFAULT_TREASURE_BAG_PROFILES);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeTierifyConfig.SPEC, "echelon-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ForgeTierifyConfig.CLIENT_SPEC, "echelon-client.toml");

        Platform.init(new ForgePlatformHelper());
        ForgeItemRegistry.ITEMS.register(modBus);
        ForgeMenuTypes.MENUS.register(modBus);
        ForgeSoundRegistry.SOUND_EVENTS.register(modBus);
        ForgeAttributeRegistry.ATTRIBUTES.register(modBus);
        LOOT_MODIFIERS.register(modBus);

        ForgeNetwork.init();

        modBus.addListener(TierifyForge::onConfigLoad);
        modBus.addListener(TierifyForge::onConfigReload);
    }

    private static void ensureDefaultConfig(String filename, String resourcePath) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path target = configDir.resolve(filename);
        if (Files.exists(target)) {
            return;
        }

        try (InputStream stream = TierifyForge.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return;
            }
            Files.createDirectories(configDir);
            Files.copy(stream, target);
        } catch (IOException ignored) {
            // If we cannot copy defaults, fallback to Forge-generated config.
        }
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ForgeTierifyConfig.SPEC) {
            EntityLootDropProfiles.reload();
            ReforgeMaterialLootProfiles.reload();
            TreasureBagProfiles.reload();
        }
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ForgeTierifyConfig.SPEC) {
            EntityLootDropProfiles.reload();
            ReforgeMaterialLootProfiles.reload();
            TreasureBagProfiles.reload();
        }
    }
}


