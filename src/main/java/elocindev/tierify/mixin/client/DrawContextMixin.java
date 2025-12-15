package elocindev.tierify.mixin.client;

import java.util.List;
import java.util.ArrayList;
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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.OrderedText;

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

                        List<Text> text = Screen.getTooltipFromItem(MinecraftClient.getInstance(), stack);
                        // Use the window width here 
                        int maxWidth = ((DrawContext)(Object)this).getScaledWindowWidth() - 16;
                        List<TooltipComponent> list = new ArrayList<>();
                        int dataInsertIndex = 1;
           
                        for (int lineIndex = 0; lineIndex < text.size(); lineIndex++) {
                            Text line = text.get(lineIndex);
                            List<OrderedText> wrapped = textRenderer.wrapLines(line, maxWidth);
                        
                            for (OrderedText ot : wrapped) {
                                list.add(TooltipComponent.of(ot));
                            }
                        
                            if (lineIndex == 0) {
                                dataInsertIndex = list.size();
                            }
                        }

                        stack.getTooltipData().ifPresent(data -> {
                            int idx = Math.min(Math.max(dataInsertIndex, 1), list.size());
                            list.add(idx, TooltipComponent.of(data));
                        });

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
