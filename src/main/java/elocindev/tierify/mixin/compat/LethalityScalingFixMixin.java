package elocindev.tierify.mixin.compat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.entity.DamageUtil; 
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 500)
public class LethalityScalingFixMixin {

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void fixBrutalityMath(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (source.getAttacker() instanceof net.minecraft.entity.player.Player player) {
            
            EntityAttribute lethalityAttr = Registries.ATTRIBUTE.get(new Identifier("brutality", "lethality"));
            EntityAttribute penAttr = Registries.ATTRIBUTE.get(new Identifier("brutality", "armor_penetration"));

            if (lethalityAttr == null || penAttr == null) return;

            EntityAttributeInstance lethalityInstance = player.getAttributeInstance(lethalityAttr);
            EntityAttributeInstance penInstance = player.getAttributeInstance(penAttr);

            if (lethalityInstance != null && penInstance != null) {
                double lethality = lethalityInstance.getValue();
                double armorPen = penInstance.getValue();

                if (lethality == 0 && armorPen == 0) return;

                LivingEntity target = (LivingEntity) (Object) this;
                float currentArmor = target.getArmor();

                double effectivePen = Math.min(armorPen, 1.0); 
                float modifiedArmor = currentArmor * (float) (1.0 - effectivePen);
                
                modifiedArmor -= (float) lethality;
                
                float toughness = (float) target.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
                float finalDamage = DamageUtil.getDamageLeft(amount, modifiedArmor, toughness);

                cir.setReturnValue(finalDamage);
            }
        }
    }
}
