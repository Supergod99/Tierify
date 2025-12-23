package elocindev.tierify.mixin.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.DamageUtil;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * Connector-safe lethality fix:
 * - Does NOT reference Brutality classes.
 * - Runs AFTER Brutality/AttributesLib by using LOW priority, so our return value "wins".
 * - Only activates when lethality/armor_pen are actually non-zero.
 * - Clamps effective armor to >= 0 to avoid pathological behavior on 0-armor / low-armor targets.
 */
@Mixin(value = LivingEntity.class, priority = 50) // LOW priority = runs later than typical mod mixins (default 1000)
public abstract class LethalityScalingFixMixin {

    @Unique private static final Identifier BRUTALITY_LETHALITY_ID = new Identifier("brutality", "lethality");
    @Unique private static final Identifier BRUTALITY_ARMOR_PEN_ID = new Identifier("brutality", "armor_penetration");

    @Unique private static final double EPS = 1.0e-6;
    @Unique private static final float MAX_DAMAGE_CAP = 1_000_000.0F;

    /* -----------------------------
     * Primary (Yarn/Fabric): LivingEntity.applyArmorToDamage(DamageSource, float)float
     * ----------------------------- */
    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void echelon$fixLethality_applyArmorToDamage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$computeAdjustedDamage(source, amount);
        if (out != null) {
            cir.setReturnValue(out);
        }
    }

    /* -----------------------------
     * Secondary (Mojmap/Forge): LivingEntity.getDamageAfterArmorAbsorb(DamageSource, float)float
     * Connector-safe optional hook.
     * ----------------------------- */
    @Inject(
        method = "getDamageAfterArmorAbsorb(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        expect = 0
    )
    private void echelon$fixLethality_getDamageAfterArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$computeAdjustedDamage(source, amount);
        if (out != null) {
            cir.setReturnValue(out);
        }
    }

    /**
     * Returns:
     * - null if we should NOT override anything
     * - finite damage value if we should override
     */
    @Unique
    private Float echelon$computeAdjustedDamage(DamageSource source, float amount) {
        if (source == null) return null;
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return null;

        // We only care about armor reduction stage; negative/zero damage shouldn't be touched.
        if (amount <= 0.0F) return null;

        // Attacker must be a LivingEntity to have attributes.
        if (!(source.getAttacker() instanceof LivingEntity attacker)) return null;

        // Look up Brutality attributes by registry id.
        EntityAttribute lethAttr = Registries.ATTRIBUTE.get(BRUTALITY_LETHALITY_ID);
        if (lethAttr == null) return null;

        EntityAttribute penAttr = Registries.ATTRIBUTE.get(BRUTALITY_ARMOR_PEN_ID); // may be null; treat as 0.

        EntityAttributeInstance lethInst = attacker.getAttributeInstance(lethAttr);
        if (lethInst == null) return null;

        double lethality = lethInst.getValue();
        if (!Double.isFinite(lethality)) return null;

        double armorPen = 0.0;
        if (penAttr != null) {
            EntityAttributeInstance penInst = attacker.getAttributeInstance(penAttr);
            if (penInst != null) {
                armorPen = penInst.getValue();
            }
        }
        if (!Double.isFinite(armorPen)) armorPen = 0.0;

        // IMPORTANT: if there is no lethality and no pen, do NOT override anything.
        if (Math.abs(lethality) < EPS && Math.abs(armorPen) < EPS) return null;

        // If incoming damage is non-finite, cap it only if we're already overriding due to lethality/pen.
        if (!Float.isFinite(amount)) {
            if (Float.isNaN(amount)) return null;
            amount = MAX_DAMAGE_CAP;
        }

        LivingEntity target = (LivingEntity) (Object) this;

        float armor = target.getArmor();
        float toughness = (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);

        // Clamp armorPen to [0..1] and apply as sane percent pen.
        // (Armor Pen is not your focus, but this keeps it functional without “2 - pen” behavior.)
        float penClamped = (float) Math.max(0.0, Math.min(1.0, armorPen));
        float armorAfterPen = armor * (1.0F - penClamped);

        // Apply lethality as flat armor reduction, but NEVER below 0.
        float effectiveArmor = armorAfterPen - (float) lethality;
        if (!Float.isFinite(effectiveArmor)) return null;
        if (effectiveArmor < 0.0F) effectiveArmor = 0.0F;

        float out = echelon$damageAfterArmorCompat(target, source, amount, effectiveArmor, toughness);
        if (!Float.isFinite(out)) return null;

        // Defensive clamp: never return negative.
        if (out < 0.0F) out = 0.0F;

        return out;
    }

    /**
     * Uses AttributesLib's armor formula when present, otherwise vanilla.
     *
     * AttributesLib’s armor reduction uses an "a/(a+armor)" style reduction and may be customized. :contentReference[oaicite:1]{index=1}
     */
    @Unique
    private static float echelon$damageAfterArmorCompat(LivingEntity target, DamageSource source, float damage, float armor, float toughness) {
        // Try AttributesLib first (reflection, Connector-safe).
        try {
            Class<?> cls = Class.forName("dev.shadowsoffire.attributeslib.api.ALCombatRules");

            // Prefer the 5-arg overload: getDamageAfterArmor(LivingEntity, DamageSource, float, float, float)
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals("getDamageAfterArmor")) continue;
                if (m.getParameterCount() != 5) continue;

                Object ret = m.invoke(null, target, source, damage, armor, toughness);
                if (ret instanceof Float f) return f;
                if (ret instanceof Number n) return n.floatValue();
            }
        } catch (Throwable ignored) {
            // Fall back to vanilla
        }

        return DamageUtil.getDamageLeft(damage, armor, toughness);
    }
}
