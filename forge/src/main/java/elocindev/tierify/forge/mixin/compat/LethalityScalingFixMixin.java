package elocindev.tierify.forge.mixin.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

@Mixin(value = LivingEntity.class, priority = 1)
public abstract class LethalityScalingFixMixin {

    @Unique private static final ResourceLocation BRUTALITY_LETHALITY_ID = new ResourceLocation("brutality", "lethality");
    @Unique private static final ResourceLocation BRUTALITY_ARMOR_PEN_ID = new ResourceLocation("brutality", "armor_penetration");

    @Unique private static final ResourceLocation ATTRIBLIB_ARMOR_PIERCE_ID = new ResourceLocation("attributeslib", "armor_pierce");
    @Unique private static final ResourceLocation ATTRIBLIB_ARMOR_SHRED_ID = new ResourceLocation("attributeslib", "armor_shred");

    @Unique private static final double EPS = 1.0e-6;
    @Unique private static final float ARMOR_BYPASS_EPS = 0.001F;

    @Unique private static final double EXCESS_BONUS_PER_POINT = 0.10;
    @Unique private static final double MAX_DAMAGE_OUT = 1.0e12;

    @Unique private static volatile boolean AL_LOOKED_UP = false;
    @Unique private static volatile Method AL_GET_ARMOR_DR = null;

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void echelon$getDamageAfterArmorAbsorb_combinedArmorBypass(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Float> cir
    ) {
        Float out = echelon$compute(source, amount);
        if (out != null) {
            cir.setReturnValue(out);
        }
    }

    @Unique
    private Float echelon$compute(DamageSource source, float amount) {
        if (source == null) return null;
        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) return null;
        if (!Float.isFinite(amount) || amount <= 0.0F) return null;

        Entity attackerEntity = source.getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) return null;

        double lethality = 0.0;
        {
            Attribute lethAttr = ForgeRegistries.ATTRIBUTES.getValue(BRUTALITY_LETHALITY_ID);
            if (lethAttr != null) {
                AttributeInstance lethInst = attacker.getAttribute(lethAttr);
                if (lethInst != null) {
                    double v = lethInst.getValue();
                    if (Double.isFinite(v)) lethality = v;
                }
            }
        }

        double armorPen = 1.0;
        {
            Attribute penAttr = ForgeRegistries.ATTRIBUTES.getValue(BRUTALITY_ARMOR_PEN_ID);
            if (penAttr != null) {
                AttributeInstance penInst = attacker.getAttribute(penAttr);
                if (penInst != null) {
                    double v = penInst.getValue();
                    if (Double.isFinite(v)) armorPen = v;
                }
            }
        }

        double pierce = 0.0;
        {
            Attribute pierceAttr = ForgeRegistries.ATTRIBUTES.getValue(ATTRIBLIB_ARMOR_PIERCE_ID);
            if (pierceAttr != null) {
                AttributeInstance inst = attacker.getAttribute(pierceAttr);
                if (inst != null) {
                    double v = inst.getValue();
                    if (Double.isFinite(v)) pierce = v;
                }
            }
        }

        double shred = 0.0;
        {
            Attribute shredAttr = ForgeRegistries.ATTRIBUTES.getValue(ATTRIBLIB_ARMOR_SHRED_ID);
            if (shredAttr != null) {
                AttributeInstance inst = attacker.getAttribute(shredAttr);
                if (inst != null) {
                    double v = inst.getValue();
                    if (Double.isFinite(v)) shred = v;
                }
            }
        }

        boolean hasEffect =
                (Math.abs(lethality) > EPS) ||
                (Math.abs(armorPen - 1.0) > EPS) ||
                (pierce > ARMOR_BYPASS_EPS) ||
                (shred > ARMOR_BYPASS_EPS);

        if (!hasEffect) return null;

        LivingEntity target = (LivingEntity) (Object) this;

        float armor = target.getArmorValue();
        if (!Float.isFinite(armor)) return null;

        float toughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (!Float.isFinite(toughness)) toughness = 0.0F;

        double multD = 2.0 - armorPen;
        if (!Double.isFinite(multD)) multD = 1.0;
        if (multD < 0.0) multD = 0.0;
        if (multD > 10.0) multD = 10.0;

        float effectiveArmor = (float) (armor * multD);
        if (!Float.isFinite(effectiveArmor)) return null;

        float bypassResist = Math.min(toughness * 0.02F, 0.6F);
        float bypassMult = 1.0F - bypassResist;

        if (shred > ARMOR_BYPASS_EPS) {
            float s = (float) shred;
            if (!Float.isFinite(s) || s < 0.0F) s = 0.0F;
            s *= bypassMult;
            if (s > 1.0F) s = 1.0F;
            effectiveArmor *= (1.0F - s);
        }

        if (pierce > ARMOR_BYPASS_EPS) {
            float p = (float) pierce;
            if (!Float.isFinite(p) || p < 0.0F) p = 0.0F;
            p *= bypassMult;
            effectiveArmor -= p;
        }

        double leth = lethality;
        if (!Double.isFinite(leth) || leth < 0.0) leth = 0.0;

        float armorBeforeLethality = Math.max(0.0F, effectiveArmor);

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

        if (out < baseline) out = baseline;

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
            }
        }

        return CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
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
