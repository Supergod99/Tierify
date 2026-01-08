package elocindev.tierify.forge.registry;

import elocindev.tierify.TierifyCommon;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForgeItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TierifyCommon.MODID);

    public static final RegistryObject<Item> LIMESTONE_CHUNK =
            ITEMS.register("limestone_chunk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PYRITE_CHUNK =
            ITEMS.register("pyrite_chunk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GALENA_CHUNK =
            ITEMS.register("galena_chunk", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CLEANSING_STONE =
            ITEMS.register("cleansing_stone", () -> new Item(new Item.Properties()));

    // Optional: put them into a creative tab for testing (safe, removable later)
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            e.accept(LIMESTONE_CHUNK);
            e.accept(PYRITE_CHUNK);
            e.accept(GALENA_CHUNK);
            e.accept(CLEANSING_STONE);
        }
    }

    private ForgeItemRegistry() {}
}

