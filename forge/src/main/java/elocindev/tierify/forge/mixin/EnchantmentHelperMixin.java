package elocindev.tierify.forge.mixin;

import elocindev.tierify.forge.registry.ForgeAttributeRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "getItemEnchantmentLevel", at = @At("RETURN"), cancellable = true)
    private static void tierify$addFortuneAttribute(Enchantment enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (enchantment != Enchantments.BLOCK_FORTUNE) return;

        double fortuneBonus = 0.0D;
        if (stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(ForgeAttributeRegistry.FORTUNE.get())) {
            Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(ForgeAttributeRegistry.FORTUNE.get());
            for (AttributeModifier mod : modifiers) {
                if (mod.getOperation() == AttributeModifier.Operation.ADDITION) {
                    fortuneBonus += mod.getAmount();
                }
            }
        }

        if (fortuneBonus > 0.0D) {
            cir.setReturnValue(cir.getReturnValue() + (int) fortuneBonus);
        }
    }
}
