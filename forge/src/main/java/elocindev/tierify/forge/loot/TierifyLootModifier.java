package elocindev.tierify.forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!ForgeTierifyConfig.lootContainerModifier()) return generatedLoot;
        if (context != null && context.getLevel().isClientSide()) return generatedLoot;

        for (ItemStack stack : generatedLoot) {
            ForgeTieredAttributeSubscriber.applyRandomTierIfAbsent(stack);
        }

        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
