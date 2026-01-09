package elocindev.tierify.forge.mixin;

import elocindev.tierify.TierifyConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
        if (tag == null) return;

        CompoundTag container = null;
        if (tag.contains(TierifyConstants.NBT_SUBTAG_EXTRA_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag extra = tag.getCompound(TierifyConstants.NBT_SUBTAG_EXTRA_KEY);
            if (extra.contains("durable")) {
                container = extra;
            }
        }
        if (container == null && tag.contains(TierifyConstants.NBT_SUBTAG_KEY, Tag.TAG_COMPOUND)) {
            CompoundTag tier = tag.getCompound(TierifyConstants.NBT_SUBTAG_KEY);
            if (tier.contains("durable")) {
                container = tier;
            }
        }
        if (container == null && tag.contains("durable")) {
            container = tag;
        }

        if (container == null) return;

        int base = cir.getReturnValue();
        if (container.contains("durable", Tag.TAG_INT)) {
            int durable = container.getInt("durable");
            if (durable != 0) {
                cir.setReturnValue(base + durable);
            }
            return;
        }

        float durable = container.getFloat("durable");
        float bonus = container.getFloat("durable_set_bonus");
        float total = durable + bonus;
        if (total != 0.0f) {
            cir.setReturnValue(base + (int) (total * base));
        }
    }
}
