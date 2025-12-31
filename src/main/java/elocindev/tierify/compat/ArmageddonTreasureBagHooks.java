package elocindev.tierify.compat;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import elocindev.tierify.config.TreasureBagProfiles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public final class ArmageddonTreasureBagHooks {
    private ArmageddonTreasureBagHooks() {}

    /**
     * Called per spawned ItemStack produced by a treasure bag.
     * Uses "bag item id in player's hands" to resolve profile and apply chance+weights.
     */
    public static ItemStack maybeReforgeFromHeldBag(ItemStack spawned, Entity entity) {
        if (spawned == null || spawned.isEmpty()) return spawned;
        if (!Tierify.CONFIG.treasureBagDropModifier) return spawned;

        // Don’t overwrite already-tiered items
        NbtCompound tierTag = spawned.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) return spawned;

        if (!(entity instanceof PlayerEntity player)) return spawned;
        if (player.getWorld().isClient()) return spawned; // safety

        Identifier bagId = resolveBagIdFromHands(player);
        if (bagId == null) return spawned;

        TreasureBagProfiles.Entry profile = TreasureBagProfiles.get(bagId);
        if (profile == null) return spawned;

        // Per-spawned-item chance roll (local RNG)
        if (Random.create().nextFloat() > profile.chance()) return spawned;

        // IMPORTANT: this API expects PlayerEntity (see ModifierUtils signature)
        ModifierUtils.setItemStackAttributeEntityWeightedWithCustomWeights(player, spawned, profile.weights());
        return spawned;
    }

    private static Identifier resolveBagIdFromHands(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        if (!main.isEmpty()) {
            Identifier id = Registries.ITEM.getId(main.getItem());
            if (TreasureBagProfiles.get(id) != null) return id;
        }

        ItemStack off = player.getOffHandStack();
        if (!off.isEmpty()) {
            Identifier id = Registries.ITEM.getId(off.getItem());
            if (TreasureBagProfiles.get(id) != null) return id;
        }

        return null;
    }
}
