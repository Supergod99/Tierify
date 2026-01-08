package draylar.tiered.api;

import com.google.common.collect.Multimap;
import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.util.SetBonusUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

public class SetBonusLogic {

    private static final UUID SET_BONUS_ID = UUID.fromString("98765432-1234-1234-1234-987654321012");
    private static final String BONUS_NAME = "Tierify Set Bonus";

    public static void updatePlayerSetBonus(ServerPlayer player) {
        if (!ForgeTierifyConfig.enableArmorSetBonuses()) {
            removeSetBonus(player);
            return;
        }

        removeSetBonus(player);

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!SetBonusUtils.hasSetBonus(player, chest)) return;

        float pct = SetBonusUtils.hasPerfectSetBonus(player, chest)
                ? ForgeTierifyConfig.armorSetPerfectBonusPercent()
                : ForgeTierifyConfig.armorSetBonusMultiplier();

        pct = Math.max(0.0f, pct);

        applySetBonusFromEquippedItem(player, chest, EquipmentSlot.CHEST, pct);
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
}
