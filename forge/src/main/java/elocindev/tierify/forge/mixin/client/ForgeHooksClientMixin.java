package elocindev.tierify.forge.mixin.client;

import com.mojang.datafixers.util.Either;
import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.compat.TooltipOverhaulCompatForge;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(ForgeHooksClient.class)
public class ForgeHooksClientMixin {

    @Inject(
            method = "gatherTooltipComponentsFromElements",
            remap = false,
            at = @At("HEAD"),
            cancellable = true
    )
    private static void tierify$disableWrapForTiered(
            ItemStack stack,
            List<Either<FormattedText, TooltipComponent>> elements,
            int mouseX,
            int screenWidth,
            int screenHeight,
            Font fallbackFont,
            CallbackInfoReturnable<List<ClientTooltipComponent>> cir
    ) {
        if (!shouldDisableWrapping(stack)) return;

        Font font = ForgeHooksClient.getTooltipFont(stack, fallbackFont);
        RenderTooltipEvent.GatherComponents event =
                new RenderTooltipEvent.GatherComponents(stack, screenWidth, screenHeight, elements, -1);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            cir.setReturnValue(List.of());
            return;
        }

        int tooltipTextWidth = event.getTooltipElements().stream()
                .mapToInt(either -> either.map(font::width, component -> 0))
                .max()
                .orElse(0);

        boolean needsWrapByScreen = false;

        int tooltipX = mouseX + 12;
        if (tooltipX + tooltipTextWidth + 4 > screenWidth) {
            tooltipX = mouseX - 16 - tooltipTextWidth;
            if (tooltipX < 4) {
                if (mouseX > screenWidth / 2) {
                    tooltipTextWidth = mouseX - 12 - 8;
                } else {
                    tooltipTextWidth = screenWidth - 16 - mouseX;
                }
                needsWrapByScreen = true;
            }
        }

        boolean needsWrap = needsWrapByScreen;
        if (event.getMaxWidth() > 0 && tooltipTextWidth > event.getMaxWidth()) {
            tooltipTextWidth = event.getMaxWidth();
            needsWrap = true;
        }

        boolean skipWrap = !needsWrapByScreen;
        if (needsWrap && !skipWrap) {
            int tooltipTextWidthF = tooltipTextWidth;
            List<ClientTooltipComponent> out = event.getTooltipElements().stream()
                    .flatMap(either -> either.map(
                            text -> splitLine(text, font, tooltipTextWidthF),
                            component -> Stream.of(ClientTooltipComponent.create(component))
                    ))
                    .toList();
            cir.setReturnValue(out);
            return;
        }

        List<ClientTooltipComponent> out = event.getTooltipElements().stream()
                .map(either -> either.map(
                        text -> ClientTooltipComponent.create(
                                text instanceof Component c ? c.getVisualOrderText() : Language.getInstance().getVisualOrder(text)
                        ),
                        ClientTooltipComponent::create
                ))
                .toList();

        cir.setReturnValue(out);
    }

    private static boolean shouldDisableWrapping(ItemStack stack) {
        if (!ForgeTierifyConfig.tieredTooltip()) return false;
        if (TooltipOverhaulCompatForge.isLoaded()) return false;
        if (stack == null || stack.isEmpty()) return false;

        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return false;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        return (tierId != null && !tierId.isEmpty()) || tiered.getBoolean("Perfect");
    }

    private static Stream<ClientTooltipComponent> splitLine(FormattedText text, Font font, int maxWidth) {
        if (text instanceof Component component && component.getString().isEmpty()) {
            return Stream.of(component.getVisualOrderText()).map(ClientTooltipComponent::create);
        }
        return font.split(text, maxWidth).stream().map(ClientTooltipComponent::create);
    }
}
