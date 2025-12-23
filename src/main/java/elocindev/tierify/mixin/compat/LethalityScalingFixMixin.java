package elocindev.tierify.mixin.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Identifier;
import net.minecraft.entity.DamageUtil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connector-safe lethality fix:
 * - Does NOT reference Brutality classes.
 * - Only activates when Brutality's lethality attribute is present AND non-zero.
 * - Cancels before Brutality's own getDamageAfterArmorAbsorb injection runs.
 */
@Mixin(value = LivingEntity.class, priority = 3000)
public abstract class LethalityScalingFixMixin {

    @Unique private static final Identifier BRUTALITY_LETHALITY_ID = new Identifier("brutality", "lethality");

    /**
     * How far below zero we allow "effective armor" to go.
     * Negative armor increases damage in vanilla formulas; extremely negative values can produce pathological results
     * when other mods feed in weird numbers. Keep this conservative.
     */
    @Unique private static final float MIN_EFFECTIVE_ARMOR = -30.0F;

    @Unique private static final double EPS = 1.0e-6;

    /* -----------------------------
     * Primary: Yarn/Fabric name
     * ----------------------------- */
    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void echelon$fixLethality_applyArmorToDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$computeLethalityAdjustedDamage(source, amount);
        if (out != null) {
            cir.setReturnValue(out); // also cancels
        }
    }

    /* -----------------------------
     * Secondary: Mojmap/Forge name
     *
     * require = 0: if this method name doesn't exist in the current mapping/runtime, do NOT crash.
     * This is the safest way to “try” the alternate name under Connector setups.
     * ----------------------------- */
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true, require = 0)
    private void echelon$fixLethality_getDamageAfterArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$computeLethalityAdjustedDamage(source, amount);
        if (out != null) {
            cir.setReturnValue(out); // also cancels
        }
    }

    /**
     * Returns:
     * - null if we should NOT override vanilla (or Brutality) behavior
     * - a finite float damage value if we should override
     */
    @Unique
    private Float echelon$computeLethalityAdjustedDamage(DamageSource source, float amount) {
        // Basic sanity: do not touch bypass-armor damage, non-finite, or non-positive damage
        if (source == null) return null;
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return null;
        if (!Float.isFinite(amount) || amount <= 0.0F) return null;

        // Lethality should be attacker-driven; only apply for player attackers
        if (!(source.getAttacker() instanceof PlayerEntity player)) return null;

        // Only run if Brutality's attribute actually exists in the registry
        EntityAttribute lethalityAttr = Registries.ATTRIBUTE.get(BRUTALITY_LETHALITY_ID);
        if (lethalityAttr == null) return null;

        EntityAttributeInstance lethalityInst = player.getAttributeInstance(lethalityAttr);
        if (lethalityInst == null) return null;

        double lethality = lethalityInst.getValue();
        if (!Double.isFinite(lethality) || Math.abs(lethality) < EPS) {
            // IMPORTANT: if there's no lethality, we do NOT override anything.
            return null;
        }

        LivingEntity target = (LivingEntity) (Object) this;

        float armor = target.getArmor();
        float toughness = (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        // Apply lethality as a direct armor reduction.
        float effectiveArmor = armor - (float) lethality;

        // Clamp only on the negative side to avoid extreme values.
        if (!Float.isFinite(effectiveArmor)) return null;
        if (effectiveArmor < MIN_EFFECTIVE_ARMOR) effectiveArmor = MIN_EFFECTIVE_ARMOR;

        float out = DamageUtil.getDamageLeft(amount, effectiveArmor, toughness);
        if (!Float.isFinite(out)) return null;

        return out;
    }
}
