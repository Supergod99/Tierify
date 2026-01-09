package elocindev.tierify.forge.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.obscuria.obscureapi.api.utils.Icons", remap = false)
public abstract class ObscureApiIconsMixin {

    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void tierify$fixAttackSpeedSlowGold(CallbackInfoReturnable<String> cir) {
        if (!(((Object) this) instanceof Enum<?> e)) return;
        if (!"ATTACK_SPEED_SLOW".equals(e.name())) return;

        String s = cir.getReturnValue();
        if (s == null || s.isEmpty()) return;

        String fixed = s.replace("\u00A76", "\u00A7c");
        cir.setReturnValue(fixed);
    }
}
