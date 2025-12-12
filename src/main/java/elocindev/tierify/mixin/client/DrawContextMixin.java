package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Inject(method = "drawItemTooltip", at = @At("HEAD"), cancellable = true)
    private void drawItemTooltipMixin(TextRenderer textRenderer, ItemStack stack, int x, int y, CallbackInfo info) {
        // If Tooltip Overhaul is present, let it handle rendering.
        if (FabricLoader.getInstance().isModLoaded("tooltipoverhaul")) {
            return;
        }

        // Logic restored from Original Mod (plus Perfect check)
        if (Tierify.CLIENT_CONFIG.tieredTooltip && stack.hasNbt()) {
            NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            
            if (tierTag != null) {
                // 1. Get the Raw ID
                String tier = tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY);
                
                // 2. Determine Lookup Key
                String lookupKey = tier;
                if (tierTag.getBoolean("Perfect")) {
                    lookupKey = "tiered:perfect";
                }

                // 3. Match against the simple strings in BORDER_TEMPLATES
                for (int i = 0; i < TierifyClient.BORDER_TEMPLATES.size(); i++) {
                    if (TierifyClient.BORDER_TEMPLATES.get(i).containsDecider(lookupKey)) {

                        List<Text> text = net.minecraft.client.gui.screen.Screen.getTooltipFromItem(MinecraftClient.getInstance(), stack);
                        List<TooltipComponent> list = text.stream().map(Text::asOrderedText).map(TooltipComponent::of).collect(Collectors.toList());
                        stack.getTooltipData().ifPresent(data -> list.add(1, TooltipComponent.of(data)));

                        TieredTooltip.renderTieredTooltipFromComponents(
                            (DrawContext) (Object) this, 
                            textRenderer, 
                            list, 
                            x, 
                            y, 
                            HoveredTooltipPositioner.INSTANCE, 
                            TierifyClient.BORDER_TEMPLATES.get(i)
                        );
                        
                        info.cancel();
                        return;
                    }
                }
            }
        }
    }
}
