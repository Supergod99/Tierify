package elocindev.tierify.forge.mixin.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemBarMixin {

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void tierify$getBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("durable")) {
            int width = Math.round(13.0f - (float) stack.getDamageValue() * 13.0f / (float) stack.getMaxDamage());
            cir.setReturnValue(width);
        }
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void tierify$getBarColor(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("durable")) {
            float f = Math.max(0.0f, ((float) stack.getMaxDamage() - (float) stack.getDamageValue()) / (float) stack.getMaxDamage());
            cir.setReturnValue(Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f));
        }
    }
}
