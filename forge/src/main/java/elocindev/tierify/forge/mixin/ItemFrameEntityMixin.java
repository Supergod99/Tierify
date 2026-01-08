package elocindev.tierify.forge.mixin;

import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {

    @Inject(method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V", at = @At("HEAD"))
    private void tierify$setItem(ItemStack stack, boolean update, CallbackInfo ci) {
        ItemFrame self = (ItemFrame) (Object) this;
        if (self.level().isClientSide()) return;
        if (update) return;
        if (!ForgeTierifyConfig.lootContainerModifier()) return;

        ForgeTieredAttributeSubscriber.applyRandomTierIfAbsent(stack);
    }
}
