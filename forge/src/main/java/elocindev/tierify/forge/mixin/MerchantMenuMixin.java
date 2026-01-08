package elocindev.tierify.forge.mixin;

import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin extends AbstractContainerMenu {

    protected MerchantMenuMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void tierify$applyMerchantModifier(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!ForgeTierifyConfig.merchantModifier()) return;
        if (index != 2) return;

        Slot slot = this.getSlot(index);
        if (slot == null) return;

        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;

        ForgeTieredAttributeSubscriber.applyRandomTierIfAbsent(stack);
    }
}
