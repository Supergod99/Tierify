package elocindev.tierify.forge.registry;

import elocindev.tierify.TierifyCommon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ForgeSoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TierifyCommon.MODID);

    public static final RegistryObject<SoundEvent> REFORGE_SOUND_COMMON = register("reforge_sound_common");
    public static final RegistryObject<SoundEvent> REFORGE_SOUND_UNCOMMON = register("reforge_sound_uncommon");
    public static final RegistryObject<SoundEvent> REFORGE_SOUND_RARE = register("reforge_sound_rare");
    public static final RegistryObject<SoundEvent> REFORGE_SOUND_EPIC = register("reforge_sound_epic");
    public static final RegistryObject<SoundEvent> REFORGE_SOUND_LEGENDARY = register("reforge_sound_legendary");
    public static final RegistryObject<SoundEvent> REFORGE_SOUND_MYTHIC = register("reforge_sound_mythic");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, name)));
    }

    private ForgeSoundRegistry() {}
}
