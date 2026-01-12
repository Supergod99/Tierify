package elocindev.tierify.forge.registry;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.item.ReforgeAddition;
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
            ITEMS.register("limestone_chunk", () -> new ReforgeAddition(new Item.Properties(), 1));
    public static final RegistryObject<Item> PYRITE_CHUNK =
            ITEMS.register("pyrite_chunk", () -> new ReforgeAddition(new Item.Properties(), 2));
    public static final RegistryObject<Item> GALENA_CHUNK =
            ITEMS.register("galena_chunk", () -> new ReforgeAddition(new Item.Properties(), 3));
    public static final RegistryObject<Item> CLEANSING_STONE =
            ITEMS.register("cleansing_stone", () -> new ReforgeAddition(new Item.Properties(), 0));
    public static final RegistryObject<Item> CHAROITE =
            ITEMS.register("charoite", () -> new ReforgeAddition(new Item.Properties(), 4));
    public static final RegistryObject<Item> CROWN_TOPAZ =
            ITEMS.register("crown_topaz", () -> new ReforgeAddition(new Item.Properties(), 5));
    public static final RegistryObject<Item> PAINITE =
            ITEMS.register("painite", () -> new ReforgeAddition(new Item.Properties(), 6));

    // Optional: put them into a creative tab for testing (safe, removable later)
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            e.accept(LIMESTONE_CHUNK);
            e.accept(PYRITE_CHUNK);
            e.accept(GALENA_CHUNK);
            e.accept(CLEANSING_STONE);
            e.accept(CHAROITE);
            e.accept(CROWN_TOPAZ);
            e.accept(PAINITE);
        }
    }

    private ForgeItemRegistry() {}
}

