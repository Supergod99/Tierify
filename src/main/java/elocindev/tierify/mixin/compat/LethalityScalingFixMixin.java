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

    @Unique private static final Identifier ATTRIBLIB_ARMOR_PIERCE_ID = new Identifier("attributeslib", "armor_pierce");
    @Unique private static final Identifier ATTRIBLIB_ARMOR_SHRED_ID  = new Identifier("attributeslib", "armor_shred");

    @Unique private static final double EPS = 1.0e-6;
    @Unique private static final float  ARMOR_BYPASS_EPS = 0.001F;

    // % of D0 per excess lethality point
    @Unique private static final double EXCESS_BONUS_PER_POINT = 0.10;

    // Pure numeric safety
    @Unique private static final double MAX_DAMAGE_OUT = 1.0e12;

    // AttributesLib reflection cache (primitive-only, Yarn-safe)
    @Unique private static volatile boolean AL_LOOKED_UP = false;
    @Unique private static volatile Method AL_GET_ARMOR_DR = null;

    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void echelon$applyArmorToDamage_combinedArmorBypass(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
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
    private void echelon$getDamageAfterArmorAbsorb_combinedArmorBypass(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
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
    private void echelon$method_26323_combinedArmorBypass(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float out = echelon$compute(source, amount);
        if (out != null) cir.setReturnValue(out);
    }

    @Unique
    private Float echelon$compute(DamageSource source, float amount) {
        if (source == null) return null;
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return null;
        if (!Float.isFinite(amount) || amount <= 0.0F) return null;

        if (!(source.getAttacker() instanceof LivingEntity attacker)) return null;

        // Collect Brutality values (do NOT early-return if missing; we might still need AL pierce/shred)
        double lethality = 0.0;
        {
            EntityAttribute lethAttr = Registries.ATTRIBUTE.get(BRUTALITY_LETHALITY_ID);
            if (lethAttr != null) {
                EntityAttributeInstance lethInst = attacker.getAttributeInstance(lethAttr);
                if (lethInst != null) {
                    double v = lethInst.getValue();
                    if (Double.isFinite(v)) lethality = v;
                }
            }
        }

        double armorPen = 1.0; // Brutality default "no change"
        {
            EntityAttribute penAttr = Registries.ATTRIBUTE.get(BRUTALITY_ARMOR_PEN_ID);
            if (penAttr != null) {
                EntityAttributeInstance penInst = attacker.getAttributeInstance(penAttr);
                if (penInst != null) {
                    double v = penInst.getValue();
                    if (Double.isFinite(v)) armorPen = v;
                }
            }
        }

        // Collect AttributesLib values
        double pierce = 0.0;
        {
            EntityAttribute pierceAttr = Registries.ATTRIBUTE.get(ATTRIBLIB_ARMOR_PIERCE_ID);
            if (pierceAttr != null) {
                EntityAttributeInstance inst = attacker.getAttributeInstance(pierceAttr);
                if (inst != null) {
                    double v = inst.getValue();
                    if (Double.isFinite(v)) pierce = v;
                }
            }
        }

        double shred = 0.0;
        {
            EntityAttribute shredAttr = Registries.ATTRIBUTE.get(ATTRIBLIB_ARMOR_SHRED_ID);
            if (shredAttr != null) {
                EntityAttributeInstance inst = attacker.getAttributeInstance(shredAttr);
                if (inst != null) {
                    double v = inst.getValue();
                    if (Double.isFinite(v)) shred = v;
                }
            }
        }

        // If nothing relevant is present, do not override vanilla/other mods.
        boolean hasEffect =
            (Math.abs(lethality) > EPS) ||
            (Math.abs(armorPen - 1.0) > EPS) ||
            (pierce > ARMOR_BYPASS_EPS) ||
            (shred > ARMOR_BYPASS_EPS);

        if (!hasEffect) return null;

        LivingEntity target = (LivingEntity) (Object) this;

        float armor = target.getArmor();
        if (!Float.isFinite(armor)) return null;

        float toughness = (float) target.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
        if (!Float.isFinite(toughness)) toughness = 0.0F;

        // Brutality armor pen: armor *= (2 - armorPen) (with safety clamps)
        double multD = 2.0 - armorPen;
        if (!Double.isFinite(multD)) multD = 1.0;
        if (multD < 0.0) multD = 0.0;     // prevent negative effective armor
        if (multD > 10.0) multD = 10.0;   // extreme safety

        float effectiveArmor = (float) (armor * multD);
        if (!Float.isFinite(effectiveArmor)) return null;

        // AttributesLib bypass resistance based on toughness
        float bypassResist = Math.min(toughness * 0.02F, 0.6F);
        float bypassMult = 1.0F - bypassResist;

        // Armor Shred (%)
        if (shred > ARMOR_BYPASS_EPS) {
            float s = (float) shred;
            if (!Float.isFinite(s) || s < 0.0F) s = 0.0F;
            s *= bypassMult;
            if (s > 1.0F) s = 1.0F; // avoid flipping armor negative by % shred
            effectiveArmor *= (1.0F - s);
        }

        // Armor Pierce (flat)
        if (pierce > ARMOR_BYPASS_EPS) {
            float p = (float) pierce;
            if (!Float.isFinite(p) || p < 0.0F) p = 0.0F;
            p *= bypassMult;
            effectiveArmor -= p;
        }

        // Lethality subtraction + "excess lethality" bonus 
        double leth = lethality;
        if (!Double.isFinite(leth) || leth < 0.0) leth = 0.0;

        float armorBeforeLethality = Math.max(0.0F, effectiveArmor);

        // Baseline: armor+pen+AL bypass but no lethality (so lethality never reduces damage)
        float baseline = echelon$afterArmorCompat(amount, armorBeforeLethality, toughness);
        if (!Float.isFinite(baseline) || baseline < 0.0F) baseline = 0.0F;

        float reducedArmor = armorBeforeLethality - (float) leth;
        float excess = 0.0F;
        if (reducedArmor < 0.0F) {
            excess = -reducedArmor;
            reducedArmor = 0.0F;
        }

        float afterArmor = echelon$afterArmorCompat(amount, reducedArmor, toughness);
        if (!Float.isFinite(afterArmor) || afterArmor < 0.0F) afterArmor = 0.0F;

        float d0 = echelon$afterArmorCompat(amount, 0.0F, toughness);
        if (!Float.isFinite(d0) || d0 < 0.0F) d0 = amount;

        double bonus = (double) d0 * (excess * EXCESS_BONUS_PER_POINT);
        double out = (double) afterArmor + bonus;

        // Guarantee lethality never reduces damage
        if (out < baseline) out = baseline;

        // Numeric safety
        if (!Double.isFinite(out)) out = baseline;
        if (out < 0.0) out = 0.0;
        if (out > MAX_DAMAGE_OUT) out = MAX_DAMAGE_OUT;

        return (float) out;
    }

    @Unique
    private static float echelon$afterArmorCompat(float damage, float armor, float toughness) {
        if (armor <= 0.0F) return damage;

        Method m = echelon$getALArmorDRMethod();
        if (m != null) {
            try {
                Object r = m.invoke(null, damage, armor, toughness);
                if (r instanceof Number n) {
                    float factor = n.floatValue();
                    if (Float.isFinite(factor)) return damage * factor;
                }
            } catch (Throwable ignored) {
                // fall back
            }
        }

        // Vanilla fallback
        return DamageUtil.getDamageLeft(damage, armor, toughness);
    }

    @Unique
    private static Method echelon$getALArmorDRMethod() {
        if (AL_LOOKED_UP) return AL_GET_ARMOR_DR;
        AL_LOOKED_UP = true;

        try {
            Class<?> cls = Class.forName("dev.shadowsoffire.attributeslib.api.ALCombatRules");
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals("getArmorDamageReduction")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 3 && p[0] == float.class && p[1] == float.class && p[2] == float.class) {
                    AL_GET_ARMOR_DR = m;
                    break;
                }
            }
        } catch (Throwable ignored) {
            AL_GET_ARMOR_DR = null;
        }
        return AL_GET_ARMOR_DR;
    }
}
