package elocindev.tierify.forge.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow @Final private static EntityDataAccessor<Float> DATA_HEALTH_ID;
    @Redirect(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V")
    )
    private void tierify$readAdditionalSaveData(LivingEntity self, float health) {
        // Avoid clamping health before tiered max-health modifiers are applied.
        self.getEntityData().set(DATA_HEALTH_ID, health);
    }
}
