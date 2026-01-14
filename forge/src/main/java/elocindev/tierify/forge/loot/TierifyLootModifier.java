package elocindev.tierify.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.config.ReforgeMaterialLootProfiles;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class TierifyLootModifier extends LootModifier {

    public static final Codec<TierifyLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, TierifyLootModifier::new)
    );

    private static final TagKey<Item> TAG_REFORGE_TIER_1 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_1")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_2 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_2")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_3 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_3")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_4 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_4")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_5 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_5")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_6 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_6")
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

    private static boolean rollReforgeMaterialChance(RandomSource random) {
        float chance = ForgeTierifyConfig.reforgeMaterialLootChance();
        if (chance >= 1.0f) return true;
        if (chance <= 0.0f) return false;
        return random.nextFloat() < chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context != null && context.getLevel().isClientSide()) return generatedLoot;

        RandomSource rng = context != null ? context.getRandom() : RandomSource.create();
        ResourceLocation dimensionId = context != null ? context.getLevel().dimension().location() : null;

        if (ForgeTierifyConfig.lootContainerModifier()) {
            for (ItemStack stack : generatedLoot) {
                if (!rollLootContainerChance(rng)) continue;
                ForgeTieredAttributeSubscriber.applyTierFromEntityWeights(stack, dimensionId, rng);
            }
        }

        if (ForgeTierifyConfig.reforgeMaterialLootModifier()) {
            maybeAddReforgeMaterial(generatedLoot, context, rng, dimensionId);
        }

        return generatedLoot;
    }

    private static void maybeAddReforgeMaterial(ObjectArrayList<ItemStack> generatedLoot,
                                                LootContext context,
                                                RandomSource rng,
                                                ResourceLocation dimensionId) {
        if (generatedLoot == null || context == null || dimensionId == null) return;
        if (!rollReforgeMaterialChance(rng)) return;

        ReforgeMaterialLootProfiles.Entry profile = ReforgeMaterialLootProfiles.get(dimensionId);
        if (profile == null) return;

        int tier = pickTierFromWeights(profile.weights(), rng);
        if (tier <= 0) return;

        ItemStack material = pickReforgeMaterial(tier, context, rng);
        if (!material.isEmpty()) {
            generatedLoot.add(material);
        }
    }

    private static int pickTierFromWeights(int[] weights, RandomSource rng) {
        if (weights == null || weights.length != 6) return 0;

        int w1 = Math.max(0, weights[0]);
        int w2 = Math.max(0, weights[1]);
        int w3 = Math.max(0, weights[2]);
        int w4 = Math.max(0, weights[3]);
        int w5 = Math.max(0, weights[4]);
        int w6 = Math.max(0, weights[5]);

        int total = w1 + w2 + w3 + w4 + w5 + w6;
        if (total <= 0) return 0;

        int roll = rng.nextInt(total);
        if (roll < w1) return 1;
        roll -= w1;
        if (roll < w2) return 2;
        roll -= w2;
        if (roll < w3) return 3;
        roll -= w3;
        if (roll < w4) return 4;
        roll -= w4;
        if (roll < w5) return 5;
        return 6;
    }

    private static ItemStack pickReforgeMaterial(int tier, LootContext context, RandomSource rng) {
        TagKey<Item> tag = switch (tier) {
            case 1 -> TAG_REFORGE_TIER_1;
            case 2 -> TAG_REFORGE_TIER_2;
            case 3 -> TAG_REFORGE_TIER_3;
            case 4 -> TAG_REFORGE_TIER_4;
            case 5 -> TAG_REFORGE_TIER_5;
            case 6 -> TAG_REFORGE_TIER_6;
            default -> null;
        };
        if (tag == null) return ItemStack.EMPTY;

        var registry = context.getLevel().registryAccess().registryOrThrow(Registries.ITEM);
        var tagSet = registry.getTag(tag);
        if (tagSet.isEmpty()) return ItemStack.EMPTY;

        int size = tagSet.get().size();
        if (size <= 0) return ItemStack.EMPTY;

        int index = rng.nextInt(size);
        int i = 0;
        for (Holder<Item> holder : tagSet.get()) {
            if (i++ == index) {
                return new ItemStack(holder.value());
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
