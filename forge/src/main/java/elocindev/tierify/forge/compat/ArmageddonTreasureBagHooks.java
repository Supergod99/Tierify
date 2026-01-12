package elocindev.tierify.forge.compat;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.config.TreasureBagProfiles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ArmageddonTreasureBagHooks {

    private ArmageddonTreasureBagHooks() {}

    public static ItemStack maybeReforgeFromHeldBag(ItemStack spawned, Entity entity) {
        if (spawned == null || spawned.isEmpty()) return spawned;
        if (!ForgeTierifyConfig.treasureBagDropModifier()) return spawned;

        var tierTag = spawned.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(TierifyConstants.NBT_SUBTAG_DATA_KEY)) return spawned;

        if (!(entity instanceof Player player)) return spawned;
        if (player.level().isClientSide()) return spawned;

        ResourceLocation bagId = resolveBagIdFromHands(player);
        if (bagId == null) return spawned;

        TreasureBagProfiles.Entry profile = TreasureBagProfiles.get(bagId);
        if (profile == null) return spawned;

        if (RandomSource.create().nextFloat() > profile.chance()) return spawned;

        int[] weights = profile.weights();
        if (weights == null || weights.length == 0) return spawned;

        ForgeTieredAttributeSubscriber.applyTierWithCustomWeights(spawned, weights, RandomSource.create());

        return spawned;
    }

    private static ResourceLocation resolveBagIdFromHands(Player player) {
        ItemStack main = player.getMainHandItem();
        if (!main.isEmpty()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(main.getItem());
            if (TreasureBagProfiles.get(id) != null) return id;
        }

        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(off.getItem());
            if (TreasureBagProfiles.get(id) != null) return id;
        }

        return null;
    }
}
