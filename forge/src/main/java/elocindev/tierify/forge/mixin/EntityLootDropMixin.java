package elocindev.tierify.forge.mixin;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.EntityLootDropProfiles;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(LivingEntity.class)
public abstract class EntityLootDropMixin {

    @Redirect(
            method = "dropFromLootTable",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"
            ),
            require = 0
    )
    private void tierify$wrapGenerateLoot(LootTable table, LootParams params, long seed, Consumer<ItemStack> consumer) {
        LivingEntity self = (LivingEntity) (Object) this;
        Consumer<ItemStack> wrapped = tierify$wrapConsumer(self, consumer, RandomSource.create(seed ^ 0x5EEDC0DEL));
        table.getRandomItems(params, seed, wrapped);
    }

    private Consumer<ItemStack> tierify$wrapConsumer(LivingEntity self, Consumer<ItemStack> original, RandomSource rng) {
        if (self.level().isClientSide()) return original;
        if (!ForgeTierifyConfig.entityLootDropModifier()) return original;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType());
        EntityLootDropProfiles.Entry profile = EntityLootDropProfiles.get(entityId);
        if (profile == null) return original;

        return (ItemStack stack) -> {
            if (stack == null || stack.isEmpty()) {
                original.accept(stack);
                return;
            }

            CompoundTag tierTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (tierTag != null && tierTag.contains(TierifyConstants.NBT_SUBTAG_DATA_KEY)) {
                original.accept(stack);
                return;
            }

            if (rng.nextFloat() < profile.chance()) {
                ForgeTieredAttributeSubscriber.applyTierWithCustomWeights(stack, profile.weights(), rng);
            }

            original.accept(stack);
        };
    }
}
