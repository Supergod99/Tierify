package draylar.tiered.api;

import com.google.common.collect.Multimap;
import elocindev.tierify.TierifyCommon;
import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.util.SetBonusUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SetBonusLogic {

    private static final UUID SET_BONUS_ID = UUID.fromString("98765432-1234-1234-1234-987654321012");
    private static final String BONUS_NAME = "Tierify Set Bonus";
    private static final String DURABLE_SB_KEY = "durable_set_bonus";
    private static final ResourceLocation DURABLE_ID = new ResourceLocation(TierifyCommon.MODID, "generic.durable");

    public static void updatePlayerSetBonus(ServerPlayer player) {
        if (!ForgeTierifyConfig.enableArmorSetBonuses()) {
            removeSetBonus(player);
            clearDurableSetBonusEverywhere(player);
            return;
        }

        removeSetBonus(player);
        clearDurableSetBonusEverywhere(player);

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!SetBonusUtils.hasSetBonus(player, chest)) return;

        CompoundTag tierTag = chest.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tierTag == null) return;

        ResourceLocation tierId = ResourceLocation.tryParse(tierTag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY));
        if (tierId == null) return;

        float pct = SetBonusUtils.hasPerfectSetBonus(player, chest)
                ? ForgeTierifyConfig.armorSetPerfectBonusPercent()
                : ForgeTierifyConfig.armorSetBonusMultiplier();

        pct = Math.max(0.0f, pct);

        applySetBonus(player, tierId, pct);
        applyDurableSetBonus(player, pct);
    }

    private static void applySetBonusFromEquippedItem(ServerPlayer player, ItemStack stack, EquipmentSlot slot, float setBonusPercent) {
        Multimap<Attribute, AttributeModifier> mods = stack.getAttributeModifiers(slot);

        UUID expectedTierUuid = TierifyConstants.MODIFIERS[armorStandSlotId(slot)];

        for (Map.Entry<Attribute, AttributeModifier> e : mods.entries()) {
            Attribute attr = e.getKey();
            AttributeModifier base = e.getValue();

            // Only boost positive stats
            if (base.getAmount() <= 0.0D) continue;

            // Only boost Tierify/Tiered tier modifiers (not vanilla armor/toughness, etc.)
            if (!expectedTierUuid.equals(base.getId())) continue;

            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) continue;

            double bonusAmount = base.getAmount() * (double) setBonusPercent * 4.0D;

            AttributeModifier bonus = new AttributeModifier(
                    SET_BONUS_ID,
                    BONUS_NAME,
                    bonusAmount,
                    base.getOperation()
            );

            inst.addTransientModifier(bonus);
        }
    }

    private static void applySetBonus(ServerPlayer player, ResourceLocation tierId, float setBonusPercent) {
        if (tierId == null) return;

        List<ForgeTieredAttributeSubscriber.TierAttributeSnapshot> attributes =
                ForgeTieredAttributeSubscriber.getTierAttributeSnapshots(tierId);
        if (attributes.isEmpty()) return;

        var registry = player.server.registryAccess().registryOrThrow(Registries.ATTRIBUTE);

        for (ForgeTieredAttributeSubscriber.TierAttributeSnapshot entry : attributes) {
            if (DURABLE_ID.equals(entry.attributeId())) continue;

            double baseValue = entry.amount();
            if (baseValue <= 0.0D) continue;

            Attribute attr = registry.get(entry.attributeId());
            if (attr == null) continue;

            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) continue;

            double bonusAmount = baseValue * (double) setBonusPercent * 4.0D;

            AttributeModifier bonus = new AttributeModifier(
                    SET_BONUS_ID,
                    BONUS_NAME,
                    bonusAmount,
                    entry.operation()
            );

            inst.addTransientModifier(bonus);
        }
    }

    private static void applyDurableSetBonus(ServerPlayer player, float pct) {
        if (pct <= 0.0f) return;

        EquipmentSlot[] slots = new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack == null || stack.isEmpty() || !stack.hasTag()) continue;
            if (!SetBonusUtils.hasSetBonus(player, stack)) continue;

            CompoundTag root = stack.getTag();
            if (root == null) continue;

            CompoundTag extra = root.contains(TierifyConstants.NBT_SUBTAG_EXTRA_KEY, 10)
                    ? root.getCompound(TierifyConstants.NBT_SUBTAG_EXTRA_KEY)
                    : null;
            CompoundTag tier = root.contains(TierifyConstants.NBT_SUBTAG_KEY, 10)
                    ? root.getCompound(TierifyConstants.NBT_SUBTAG_KEY)
                    : null;

            CompoundTag container = null;
            if (extra != null && extra.contains("durable")) container = extra;
            else if (tier != null && tier.contains("durable")) container = tier;
            else if (root.contains("durable")) container = root;

            if (container == null || !container.contains("durable")) continue;

            float baseMult = container.getFloat("durable");
            if (!Float.isFinite(baseMult) || baseMult <= 0.0F) continue;

            float sb = baseMult * pct;
            float prev = container.contains(DURABLE_SB_KEY) ? container.getFloat(DURABLE_SB_KEY) : 0.0F;
            if (Math.abs(prev - sb) > 1.0e-6f) {
                container.putFloat(DURABLE_SB_KEY, sb);

                if (container == extra) root.put(TierifyConstants.NBT_SUBTAG_EXTRA_KEY, extra);
                else if (container == tier) root.put(TierifyConstants.NBT_SUBTAG_KEY, tier);
            }
        }
    }

    // Matches Fabric’s slot.getArmorStandSlotId() behavior closely enough for HEAD/CHEST/LEGS/FEET.
    private static int armorStandSlotId(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> 0;
            case OFFHAND -> 1;
            case FEET -> 2;
            case LEGS -> 3;
            case CHEST -> 4;
            case HEAD -> 5;
        };
    }

    public static void removeSetBonus(ServerPlayer player) {
        var registry = player.server.registryAccess().registryOrThrow(Registries.ATTRIBUTE);

        for (Attribute attribute : registry) {
            AttributeInstance inst = player.getAttribute(attribute);
            if (inst != null && inst.getModifier(SET_BONUS_ID) != null) {
                inst.removeModifier(SET_BONUS_ID);
            }
        }
    }

    private static void clearDurableSetBonusEverywhere(ServerPlayer player) {
        Inventory inv = player.getInventory();

        for (int i = 0; i < inv.items.size(); i++) {
            clearDurableSetBonusOnStack(inv.items.get(i));
        }

        for (int i = 0; i < inv.armor.size(); i++) {
            clearDurableSetBonusOnStack(inv.armor.get(i));
        }

        for (int i = 0; i < inv.offhand.size(); i++) {
            clearDurableSetBonusOnStack(inv.offhand.get(i));
        }
    }

    private static void clearDurableSetBonusOnStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return;

        CompoundTag root = stack.getTag();
        if (root == null) return;

        boolean changed = false;

        if (root.contains(TierifyConstants.NBT_SUBTAG_EXTRA_KEY, 10)) {
            CompoundTag extra = root.getCompound(TierifyConstants.NBT_SUBTAG_EXTRA_KEY);
            if (extra.contains(DURABLE_SB_KEY)) {
                extra.remove(DURABLE_SB_KEY);
                root.put(TierifyConstants.NBT_SUBTAG_EXTRA_KEY, extra);
                changed = true;
            }
        }

        if (root.contains(TierifyConstants.NBT_SUBTAG_KEY, 10)) {
            CompoundTag tier = root.getCompound(TierifyConstants.NBT_SUBTAG_KEY);
            if (tier.contains(DURABLE_SB_KEY)) {
                tier.remove(DURABLE_SB_KEY);
                root.put(TierifyConstants.NBT_SUBTAG_KEY, tier);
                changed = true;
            }
        }

        if (root.contains(DURABLE_SB_KEY)) {
            root.remove(DURABLE_SB_KEY);
            changed = true;
        }

        if (!changed) return;

        int max = stack.getMaxDamage();
        if (max > 0 && stack.getDamageValue() >= max) {
            stack.setDamageValue(max - 1);
        }
    }
}
