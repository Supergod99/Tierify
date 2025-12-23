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

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(value = LivingEntity.class, priority = 1) // LOW so we run late and "win" return value
public abstract class LethalityScalingFixMixin {

    @Unique private static final Identifier BRUTALITY_LETHALITY_ID = new Identifier("brutality", "lethality");
    @Unique private static final Identifier BRUTALITY_ARMOR_PEN_ID = new Identifier("brutality", "armor_penetration");

    @Unique private static final double EPS = 1.0e-6;

    // Your requested strength: 5% of D0 per excess lethality point (uncapped).
    @Unique private static final double EXCESS_BONUS_PER_POINT = 0.05;

    // Pure numeric safety (not a lethality cap)
    @Unique private static final double MAX_DAMAGE_OUT = 1.0e12;

    // AttributesLib reflection cache
    @Unique private static volatile boolean AL_LOOKED_UP = false;
    @Unique private static volatile Method AL_GET_DAMAGE_AFTER_ARMOR = null;

    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void echelon$applyArmorToDamage_lethalityFix(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$compute(source, amount);
        if (out != null) cir.setReturnValue(out);
    }

    @Dynamic("Optional Connector-safe hook for Mojmap-named method")
    @Inject(
        method = "getDamageAfterArmorAbsorb(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        expect = 0
    )
    private void echelon$getDamageAfterArmorAbsorb_lethalityFix(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$compute(source, amount);
        if (out != null) cir.setReturnValue(out);
    }

    @Dynamic("Optional fallback for intermediary-named method in some pipelines")
    @Inject(
        method = "method_26323(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        expect = 0
    )
    private void echelon$method_26323_lethalityFix(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$compute(source, amount);
        if (out != null) cir.setReturnValue(out);
    }

    @Unique
    private Float echelon$compute(DamageSource source, float amount) {
        if (source == null) return null;
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return null;
        if (!Float.isFinite(amount) || amount <= 0.0F) return null;

        if (!(source.getAttacker() instanceof LivingEntity attacker)) return null;

        EntityAttribute lethAttr = Registries.ATTRIBUTE.get(BRUTALITY_LETHALITY_ID);
        if (lethAttr == null) return null;

        EntityAttributeInstance lethInst = attacker.getAttributeInstance(lethAttr);
        if (lethInst == null) return null;

        double lethality = lethInst.getValue();
        if (!Double.isFinite(lethality) || Math.abs(lethality) < EPS) return null; // no lethality => no override

        // Read armor pen, but do not change its semantics; follow Brutality’s model: armor * (2 - armorPen)
        double armorPen = 1.0; // Brutality baseline behavior effectively assumes 1.0 means "no change"
        EntityAttribute penAttr = Registries.ATTRIBUTE.get(BRUTALITY_ARMOR_PEN_ID);
        if (penAttr != null) {
            EntityAttributeInstance penInst = attacker.getAttributeInstance(penAttr);
            if (penInst != null) {
                double v = penInst.getValue();
                if (Double.isFinite(v)) armorPen = v;
            }
        }

        LivingEntity target = (LivingEntity) (Object) this;

        float armor = target.getArmor();
        if (!Float.isFinite(armor)) return null;

        float toughness = (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        if (!Float.isFinite(toughness)) toughness = 0.0F;

        // Brutality semantics: multiplier = (2 - armorPen). Numeric safety only: block negative multipliers.
        double multD = 2.0 - armorPen;
        if (!Double.isFinite(multD)) multD = 1.0;
        if (multD < 0.0) multD = 0.0;              // prevent negative effective armor
        if (multD > 10.0) multD = 10.0;            // extreme safety; should never hit in normal gameplay

        float effectiveArmorBeforeLethality = (float) (armor * multD);
        if (!Float.isFinite(effectiveArmorBeforeLethality)) return null;

        // Baseline: armor+pen but no lethality (so lethality never reduces damage)
        float baselineArmor = Math.max(0.0F, effectiveArmorBeforeLethality);
        float baseline = echelon$afterArmorCompat(target, source, amount, baselineArmor, toughness);
        if (!Float.isFinite(baseline) || baseline < 0.0F) baseline = 0.0F;

        // Apply lethality; do NOT allow negative armor into formulas
        float reducedArmor = effectiveArmorBeforeLethality - (float) lethality;
        float excess = 0.0F;
        if (reducedArmor < 0.0F) {
            excess = -reducedArmor; // = lethality - effectiveArmorBeforeLethality
            reducedArmor = 0.0F;
        }

        float afterArmor = echelon$afterArmorCompat(target, source, amount, reducedArmor, toughness);
        if (!Float.isFinite(afterArmor) || afterArmor < 0.0F) afterArmor = 0.0F;

        // D0: damage at 0 armor under the active armor rules (AttributesLib returns amount when armor<=0).
        float d0 = echelon$afterArmorCompat(target, source, amount, 0.0F, toughness);
        if (!Float.isFinite(d0) || d0 < 0.0F) d0 = amount;

        // Uncapped linear % bonus based on "excess lethality"
        double bonus = (double) d0 * (excess * EXCESS_BONUS_PER_POINT);
        double out = (double) afterArmor + bonus;

        // Guarantee lethality never reduces damage
        if (out < baseline) out = baseline;

        // Numeric safety only
        if (!Double.isFinite(out)) out = baseline;
        if (out < 0.0) out = 0.0;
        if (out > MAX_DAMAGE_OUT) out = MAX_DAMAGE_OUT;

        return (float) out;
    }

    @Unique
    private static float echelon$afterArmorCompat(LivingEntity target, DamageSource source, float damage, float armor, float toughness) {
        Method m = echelon$getALMethod();
        if (m != null) {
            try {
                Object r = m.invoke(null, target, source, damage, armor, toughness);
                if (r instanceof Number n) return n.floatValue();
            } catch (Throwable ignored) {
                // fall back
            }
        }
        return DamageUtil.getDamageLeft(damage, armor, toughness);
    }

    @Unique
    private static Method echelon$getALMethod() {
        if (AL_LOOKED_UP) return AL_GET_DAMAGE_AFTER_ARMOR;
        AL_LOOKED_UP = true;

        try {
            Class<?> cls = Class.forName("dev.shadowsoffire.attributeslib.api.ALCombatRules");
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals("getDamageAfterArmor")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 5 && p[2] == float.class && p[3] == float.class && p[4] == float.class) {
                    AL_GET_DAMAGE_AFTER_ARMOR = m;
                    break;
                }
            }
        } catch (Throwable ignored) {
            AL_GET_DAMAGE_AFTER_ARMOR = null;
        }

        return AL_GET_DAMAGE_AFTER_ARMOR;
    }
}
