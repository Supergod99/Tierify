package elocindev.tierify.forge.registry;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.screen.ReforgeMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ForgeMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TierifyCommon.MODID);

    public static final RegistryObject<MenuType<ReforgeMenu>> REFORGE =
            MENUS.register("reforge", () -> IForgeMenuType.create(ReforgeMenu::new));

    private ForgeMenuTypes() {}
}
