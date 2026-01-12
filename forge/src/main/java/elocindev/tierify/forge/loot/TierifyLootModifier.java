package elocindev.tierify.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class TierifyLootModifier extends LootModifier {

    public static final Codec<TierifyLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, TierifyLootModifier::new)
    );

    protected TierifyLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    private static boolean rollLootContainerChance(RandomSource random) {
        float chance = ForgeTierifyConfig.lootContainerModifierChance();
        if (chance >= 1.0f) return true;
        if (chance <= 0.0f) return false;
        return random.nextFloat() < chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!ForgeTierifyConfig.lootContainerModifier()) return generatedLoot;
        if (context != null && context.getLevel().isClientSide()) return generatedLoot;

        RandomSource rng = context != null ? context.getRandom() : RandomSource.create();
        ResourceLocation dimensionId = context != null ? context.getLevel().dimension().location() : null;

        for (ItemStack stack : generatedLoot) {
            if (!rollLootContainerChance(rng)) continue;
            ForgeTieredAttributeSubscriber.applyTierFromEntityWeights(stack, dimensionId, rng);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
