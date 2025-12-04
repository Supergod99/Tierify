package elocindev.tierify.mixin.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    public HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;Ljava/util/Optional;II)V"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    protected void drawMouseoverTooltipMixin(DrawContext context, int x, int y, CallbackInfo info, ItemStack stack) {
        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack.hasNbt() && stack.getNbt().contains("Tiered")) {
            

            NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (tierTag != null && tierTag.getBoolean("Perfect")) {

                for (int p = 0; p < TierifyClient.BORDER_TEMPLATES.size(); p++) {
                    BorderTemplate template = TierifyClient.BORDER_TEMPLATES.get(p);

                    // Matches the perfect decider defined in TooltipBorderLoader
                    if (template.containsDecider("{BorderTier:\"tiered:perfect\"}")) {

                        // Register this stack to this template
                        if (!template.containsStack(stack)) {
                            template.addStack(stack);
                        }
                        
                        List<Text> text = Screen.getTooltipFromItem(client, stack);
                        List<TooltipComponent> list = new ArrayList<>();
                        int wrapWidth = 300; // Allow wider tooltips
                        
                        for (int k = 0; k < text.size(); k++) {
                            Text t = text.get(k);
                            int width = this.textRenderer.getWidth(t);
                        
                            // If it's the title (0) or short enough, do NOT touch it. 

                            if (k == 0 || width <= wrapWidth) {
                                list.add(TooltipComponent.of(t.asOrderedText()));
                            } else {

                                List<OrderedText> wrapped = this.textRenderer.wrapLines(t, wrapWidth);
                                for (OrderedText line : wrapped) {
                                    list.add(TooltipComponent.of(line));
                                }
                            }
                        }
                        // END FIX

                        stack.getTooltipData().ifPresent(data -> {
                            if (list.size() > 1) {
                                list.add(1, TooltipComponent.of(data));
                            } else {
                                list.add(TooltipComponent.of(data));
                            }
                        });

                        TieredTooltip.renderTieredTooltipFromComponents(
                                context,
                                this.textRenderer,
                                list,
                                x,
                                y,
                                HoveredTooltipPositioner.INSTANCE,
                                template
                        );

                        info.cancel();
                        return;
                    }
                }
            }


            String nbtString = stack.getNbt().getCompound("Tiered").asString();
            for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                if (!TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack) && TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(nbtString)) {
                    TierifyClient.BORDER_TEMPLATES.get(i).addStack(stack);
                } else if (TierifyClient.BORDER_TEMPLATES.get(i).containsStack(stack)) {
                    List<Text> text = Screen.getTooltipFromItem(client, stack);


                    List<TooltipComponent> list = new ArrayList<>();
                    int maxWidth = 200;

                    for (Text t : text) {
                        List<OrderedText> wrapped = this.textRenderer.wrapLines(t, maxWidth);
                        for (OrderedText line : wrapped) {
                            list.add(TooltipComponent.of(line));
                        }
                    }


                    stack.getTooltipData().ifPresent(data -> {
                        if (list.size() > 1) {
                            list.add(1, TooltipComponent.of(data));
                        } else {
                            list.add(TooltipComponent.of(data));
                        }
                    });

                    TieredTooltip.renderTieredTooltipFromComponents(context, this.textRenderer, list, x, y, HoveredTooltipPositioner.INSTANCE, TierifyClient.BORDER_TEMPLATES.get(i));

                    info.cancel();
                    break;
                }
            }
        }
    }
}
