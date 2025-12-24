package elocindev.tierify.mixin;

import elocindev.tierify.Tierify;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow public abstract NbtCompound getNbt();
    @Shadow public abstract boolean hasNbt();

    @Inject(method = "getMaxDamage", at = @At("TAIL"), cancellable = true)
    private void getMaxDamageMixin(CallbackInfoReturnable<Integer> cir) {
        if (!hasNbt()) return;

        NbtCompound root = getNbt();
        if (root == null) return;
        // namespaced TierifyExtra
        NbtCompound extra = root.getCompound(Tierify.NBT_SUBTAG_EXTRA_KEY);
        if (extra != null && extra.contains("durable")) {
            applyDurable(extra, cir);
            return;
        }

        // fallback: only honor root "durable" if item is tiered
        if (root.contains(Tierify.NBT_SUBTAG_KEY) && root.contains("durable")) {
            applyDurable(root, cir);
        }
    }

    private static void applyDurable(NbtCompound tag, CallbackInfoReturnable<Integer> cir) {
        int base = cir.getReturnValue();
        // Support both old int durable and new float/double durable
        if (tag.contains("durable", 3 /* INT */)) {
            int durableInt = tag.getInt("durable");
            if (durableInt > 0) {
                cir.setReturnValue(base + durableInt);
            }
            return;
        }
        // NBT numeric types
        if (tag.contains("durable")) {
            float durable = tag.getFloat("durable");
            if (!Float.isFinite(durable) || durable <= 0.0F) return;
            cir.setReturnValue(base + (int) (durable * base));
        }
    }
}
