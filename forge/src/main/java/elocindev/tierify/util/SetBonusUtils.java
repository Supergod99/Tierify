package elocindev.tierify.util;

import elocindev.tierify.TierifyConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public final class SetBonusUtils {

    private SetBonusUtils() {}

    public static boolean hasSetBonus(ServerPlayer player, ItemStack anyArmorPiece) {
        if (player == null || anyArmorPiece == null || anyArmorPiece.isEmpty()) return false;
        if (!(anyArmorPiece.getItem() instanceof ArmorItem)) return false;

        CompoundTag tag = anyArmorPiece.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tag == null) return false;

        String targetTier = tag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (targetTier.isEmpty()) return false;

        // Require full set: HEAD, CHEST, LEGS, FEET all match the same Tiered/Tier value.
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) return false;

            CompoundTag armorTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (armorTag == null) return false;

            String armorTier = armorTag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
            if (!targetTier.equals(armorTier)) return false;
        }

        return true;
    }

    public static boolean hasPerfectSetBonus(ServerPlayer player, ItemStack anyArmorPiece) {
        if (!hasSetBonus(player, anyArmorPiece)) return false;

        CompoundTag tag = anyArmorPiece.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tag == null) return false;

        String targetTier = tag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (targetTier.isEmpty()) return false;

        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) return false;

            CompoundTag armorTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (armorTag == null) return false;

            if (!targetTier.equals(armorTag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY))) return false;
            if (!armorTag.getBoolean("Perfect")) return false;
        }

        return true;
    }
}

