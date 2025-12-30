package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import elocindev.tierify.config.EntityLootDropProfiles;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class EntityEquipmentDropMixin {

    @ModifyArg(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;dropStack(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/ItemEntity;"
        ),
        index = 0,
        require = 0
    )
    private ItemStack echelon$maybeReforgeEquipmentDrop_entityOwner_noYOffset(ItemStack stack) {
        return echelon$maybeReforgeEquipmentDrop(stack);
    }
    
    @ModifyArg(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;"
        ),
        index = 0,
        require = 0
    )
    private ItemStack echelon$maybeReforgeEquipmentDrop_entityOwner_withYOffset(ItemStack stack) {
        return echelon$maybeReforgeEquipmentDrop(stack);
    }

    @ModifyArg(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;dropStack(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/ItemEntity;"
        ),
        index = 0,
        require = 0
    )
    private ItemStack echelon$maybeReforgeEquipmentDrop_noYOffset(ItemStack stack) {
        return echelon$maybeReforgeEquipmentDrop(stack);
    }

    @ModifyArg(
        method = "dropEquipment",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/LivingEntity;dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;"
        ),
        index = 0,
        require = 0
    )
    private ItemStack echelon$maybeReforgeEquipmentDrop_withYOffset(ItemStack stack) {
        return echelon$maybeReforgeEquipmentDrop(stack);
    }

    
    private ItemStack echelon$maybeReforgeEquipmentDrop(ItemStack stack) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (stack == null || stack.isEmpty()) return stack;
        if (self.getWorld().isClient()) return stack;

        // mobs only (avoid players or other LivingEntity implementors)
        if (!(self instanceof MobEntity)) return stack;

        if (!Tierify.CONFIG.entityEquipmentDropModifier) return stack;

        // Dont overwrite an already-tiered item (covers entityItemModifier path too)
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            return stack;
        }

        Identifier entityId = Registries.ENTITY_TYPE.getId(self.getType());
        EntityLootDropProfiles.Entry profile = EntityLootDropProfiles.get(entityId);
        if (profile == null) return stack;

        // Deterministic RNG per (entity, item) without consuming vanilla RNG streams
        long seed =
            self.getUuid().getLeastSignificantBits()
            ^ (self.getUuid().getMostSignificantBits() * 31L)
            ^ ((long) Registries.ITEM.getRawId(stack.getItem()) << 32)
            ^ 0xECA11D0E5EEDC0DEL;
        Random rng = Random.create(seed);

        if (rng.nextFloat() > profile.chance()) return stack;

        ModifierUtils.setItemStackAttributeEntityWeightedWithCustomWeights(null, stack, profile.weights());
        return stack;
    }
}
