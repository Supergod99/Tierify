package elocindev.tierify.mixin.client;

import draylar.tiered.api.BorderTemplate;
import elocindev.tierify.Tierify;
import elocindev.tierify.TierifyClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

    @Inject(method = "renderGuiItemOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void renderOverlayMixin(DrawContext context, TextRenderer renderer, ItemStack stack, int x, int y, @Nullable String countLabel, CallbackInfo info) {
        if (stack.hasNbt() && stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY) != null) {
            NbtCompound tierTag = stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY);
            

            String lookupKey;
            if (tierTag.contains("BorderTier")) {
                lookupKey = "{BorderTier:\"tiered:perfect\"}";
            } else if (tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) {
                lookupKey = "{Tier:\"" + tierTag.getString(Tierify.NBT_SUBTAG_DATA_KEY) + "\"}";
            } else {
                return;
            }

            BorderTemplate match = null;
            for (BorderTemplate template : TierifyClient.BORDER_TEMPLATES) {
                if (template.containsDecider(lookupKey)) {
                    match = template;
                    break;
                }
            }

            if (match != null) {
                context.drawTexture(match.getIdentifier(), x, y, 0, 0, 16, 16, 16, 16);
            }
        }
    }
}
