package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import elocindev.tierify.config.EntityLootDropProfiles;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class EntityEquipmentDropMixin {

    /**
     * Debug probe: proves whether we're actually entering the equipment-drop path at runtime.
     * Keep require=0 so we don't crash if the signature differs in a given environment.
     */
    @Inject(method = "dropEquipment", at = @At("HEAD"), require = 0)
    private void echelon$debugDropEquipmentHead(DamageSource source, int lootingMultiplier, boolean allowDrops, CallbackInfo ci) {
        MobEntity self = (MobEntity) (Object) this;
        Identifier entityId = Registries.ENTITY_TYPE.getId(self.getType());
        Tierify.LOGGER.info("[EquipDrop] ENTER dropEquipment(entity={}, looting={}, allowDrops={}, enabled={})",
                entityId, lootingMultiplier, allowDrops, Tierify.CONFIG.entityEquipmentDropModifier);
    }

    // Option B: four hook targets inside dropEquipment
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
        MobEntity self = (MobEntity) (Object) this;

        if (stack == null || stack.isEmpty()) return stack;
        if (self.getWorld().isClient()) return stack;

        Identifier entityId = Registries.ENTITY_TYPE.getId(self.getType());
        Identifier itemId = Registries.ITEM.getId(stack.getItem());

        // Log early so we can diagnose "hook fires but config disables it" scenarios.
        Tierify.LOGGER.info("[EquipDrop] HIT entity={} item={} enabled={}", entityId, itemId, Tierify.CONFIG.entityEquipmentDropModifier);

        if (!Tierify.CONFIG.entityEquipmentDropModifier) return stack;

        // Don’t overwrite already-tiered items
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
            Tierify.LOGGER.info("[EquipDrop] SKIP already-tiered entity={} item={}", entityId, itemId);
            return stack;
        }

        EntityLootDropProfiles.Entry profile = EntityLootDropProfiles.get(entityId);
        if (profile == null) {
            Tierify.LOGGER.info("[EquipDrop] SKIP no-profile entity={} item={}", entityId, itemId);
            return stack;
        }

        Random rng = Random.create();
        float roll = rng.nextFloat();
        if (roll > profile.chance()) {
            Tierify.LOGGER.info("[EquipDrop] SKIP chance entity={} item={} roll={} chance={}",
                    entityId, itemId, roll, profile.chance());
            return stack;
        }

        Tierify.LOGGER.info("[EquipDrop] APPLY entity={} item={} chance={} weights={},{},{},{},{},{}",
                entityId,
                itemId,
                profile.chance(),
                profile.weights()[0], profile.weights()[1], profile.weights()[2],
                profile.weights()[3], profile.weights()[4], profile.weights()[5]
        );

        ModifierUtils.setItemStackAttributeEntityWeightedWithCustomWeights(null, stack, profile.weights());

        NbtCompound afterTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        boolean hasTierAfter = afterTag != null && afterTag.contains(Tierify.NBT_SUBTAG_DATA_KEY);

        Tierify.LOGGER.info("[EquipDrop] DONE entity={} item={} hasTierAfter={}", entityId, itemId, hasTierAfter);

        return stack;
    }
}
