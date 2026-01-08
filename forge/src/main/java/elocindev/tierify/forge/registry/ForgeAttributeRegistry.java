package elocindev.tierify.forge.registry;

import elocindev.tierify.TierifyCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ForgeAttributeRegistry {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TierifyCommon.MODID);

    public static final RegistryObject<Attribute> DIG_SPEED =
            register("generic.dig_speed", 0.0D, 0.0D, 2048.0D);
    public static final RegistryObject<Attribute> CRIT_CHANCE =
            register("generic.crit_chance", 0.0D, 0.0D, 1.0D);
    public static final RegistryObject<Attribute> DURABLE =
            register("generic.durable", 0.0D, 0.0D, 1.0D);
    public static final RegistryObject<Attribute> RANGE_ATTACK_DAMAGE =
            register("generic.range_attack_damage", 0.0D, 0.0D, 2048.0D);
    public static final RegistryObject<Attribute> FORTUNE =
            register("generic.fortune", 0.0D, 0.0D, 100.0D);

    private static RegistryObject<Attribute> register(String path, double base, double min, double max) {
        ResourceLocation id = new ResourceLocation(TierifyCommon.MODID, path);
        return ATTRIBUTES.register(id.getPath(),
                () -> new RangedAttribute(path, base, min, max).setSyncable(true));
    }

    private ForgeAttributeRegistry() {}
}
