package elocindev.tierify.forge.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxDamage", at = @At("TAIL"), cancellable = true)
    private void tierify$getMaxDamage(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("durable")) return;

        int base = cir.getReturnValue();
        int durable = tag.getInt("durable");
        if (durable > 0) {
            cir.setReturnValue(base + durable);
        } else {
            float f = tag.getFloat("durable");
            cir.setReturnValue(base + (int) (f * base));
        }
    }
}
