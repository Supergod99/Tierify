package elocindev.tierify.forge.mixin;

import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public class ArmorStandEntityMixin {

    @Unique
    private boolean tierify$isGenerated = true;
    @Unique
    private boolean tierify$isClient = true;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tierify$addAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("IsGenerated", this.tierify$isGenerated);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tierify$readAdditionalSaveData(CompoundTag tag, CallbackInfo ci) {
        this.tierify$isGenerated = tag.getBoolean("IsGenerated");
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void tierify$interactAt(Player player, Vec3 hitPos, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.tierify$isGenerated = false;
        this.tierify$isClient = player.level().isClientSide();
    }

    @Inject(method = "setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private void tierify$setItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (!this.tierify$isClient && this.tierify$isGenerated && ForgeTierifyConfig.lootContainerModifier()) {
            ForgeTieredAttributeSubscriber.applyRandomTierIfAbsent(stack);
        }
    }
}
