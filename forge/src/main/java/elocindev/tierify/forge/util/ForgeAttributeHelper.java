package elocindev.tierify.forge.util;

import elocindev.tierify.forge.registry.ForgeAttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public final class ForgeAttributeHelper {

    private ForgeAttributeHelper() {}

    public static boolean shouldMeeleCrit(Player player) {
        AttributeInstance instance = player.getAttribute(ForgeAttributeRegistry.CRIT_CHANCE.get());
        if (instance != null) {
            float critChance = 0.0f;
            for (AttributeModifier modifier : instance.getModifiers()) {
                critChance += (float) modifier.getAmount();
            }
            return player.getRandom().nextDouble() < critChance;
        }
        return false;
    }

    public static float getExtraDigSpeed(Player player, float oldDigSpeed) {
        AttributeInstance instance = player.getAttribute(ForgeAttributeRegistry.DIG_SPEED.get());
        if (instance != null) {
            float extraDigSpeed = oldDigSpeed;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.getAmount();

                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    extraDigSpeed += amount;
                } else {
                    extraDigSpeed *= (amount + 1.0f);
                }
            }
            return extraDigSpeed;
        }

        return oldDigSpeed;
    }

    public static float getExtraRangeDamage(Player player, float oldDamage) {
        AttributeInstance instance = player.getAttribute(ForgeAttributeRegistry.RANGE_ATTACK_DAMAGE.get());
        if (instance != null) {
            float rangeDamage = oldDamage;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.getAmount();

                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    rangeDamage += amount;
                } else {
                    rangeDamage *= (amount + 1.0f);
                }
            }
            return Math.min(rangeDamage, Integer.MAX_VALUE);
        }
        return oldDamage;
    }

    public static float getExtraCritDamage(Player player, float oldDamage) {
        AttributeInstance instance = player.getAttribute(ForgeAttributeRegistry.CRIT_CHANCE.get());
        if (instance != null) {
            float customChance = 0.0f;
            for (AttributeModifier modifier : instance.getModifiers()) {
                customChance += (float) modifier.getAmount();
            }
            if (player.level().getRandom().nextFloat() > (1.0f - Math.abs(customChance))) {
                float extraCrit = oldDamage;
                if (customChance < 0.0f) {
                    extraCrit = extraCrit / 2.0f;
                }
                return oldDamage + Math.min(customChance > 0.0f ? extraCrit : -extraCrit, Integer.MAX_VALUE);
            }
        }
        return oldDamage;
    }
}
