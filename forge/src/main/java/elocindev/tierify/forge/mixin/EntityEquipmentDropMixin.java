package elocindev.tierify.forge.mixin;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.EntityLootDropProfiles;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Mob.class)
public abstract class EntityEquipmentDropMixin {

    @ModifyArg(
            method = "dropCustomDeathLoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
            ),
            index = 0,
            require = 0
    )
    private ItemStack tierify$maybeReforgeEquipmentDrop(ItemStack stack) {
        return tierify$maybeReforgeEquipmentDrop(stack, (Mob) (Object) this);
    }

    @ModifyArg(
            method = "dropCustomDeathLoot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"
            ),
            index = 0,
            require = 0
    )
    private ItemStack tierify$maybeReforgeEquipmentDrop_entityOwner(ItemStack stack) {
        return tierify$maybeReforgeEquipmentDrop(stack, (Mob) (Object) this);
    }

    private static ItemStack tierify$maybeReforgeEquipmentDrop(ItemStack stack, Mob self) {

        if (stack == null || stack.isEmpty()) return stack;
        if (self.level().isClientSide()) return stack;
        if (!ForgeTierifyConfig.entityEquipmentDropModifier()) return stack;

        CompoundTag tierTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(TierifyConstants.NBT_SUBTAG_DATA_KEY)) return stack;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType());
        EntityLootDropProfiles.Entry profile = EntityLootDropProfiles.get(entityId);
        if (profile == null) return stack;

        RandomSource rng = RandomSource.create();
        if (rng.nextFloat() > profile.chance()) return stack;

        ForgeTieredAttributeSubscriber.applyTierWithCustomWeights(stack, profile.weights(), rng);
        return stack;
    }
}
