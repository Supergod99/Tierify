package elocindev.tierify.forge.mixin.compat;

import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.DecimalFormat;

@Pseudo
@Mixin(targets = "net.levelz.screen.SkillInfoScreen", remap = false)
public class SkillInfoScreenMixin {

    @Shadow(remap = false)
    @Mutable
    @Final
    private String title;

    @Shadow(remap = false)
    private Component translatableText1A;

    @Shadow(remap = false)
    private Component translatableText1B;

    @Inject(
            method = "init",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0, shift = Shift.BEFORE),
            remap = false
    )
    private void tierify$injectSmithingInfo(CallbackInfo info) {
        if (!"smithing".equals(title)) return;
        String pct = new DecimalFormat("0.0").format(ForgeTierifyConfig.levelzReforgeModifier() * 100);
        this.translatableText1A = Component.translatable("text.tiered.smithing_info_1_1", pct);
        this.translatableText1B = Component.translatable("text.tiered.smithing_info_1_2", pct);
    }
}
