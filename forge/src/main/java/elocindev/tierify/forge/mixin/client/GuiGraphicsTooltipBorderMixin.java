package elocindev.tierify.forge.mixin.client;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.client.PerfectLabelAnimatorForge;
import elocindev.tierify.forge.client.TierGradientAnimatorForge;
import elocindev.tierify.forge.client.TierifyTooltipBorderRendererForge;
import elocindev.tierify.forge.compat.TooltipOverhaulCompatForge;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector2ic;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge tooltip hook:
 * - draws Tierify tooltip borders above the vanilla background
 * - reserves space and renders Perfect label below the item name (centered + scaled + animated gradient)
 * - renders Set Bonus label (centered + scaled) in the top band (not as a normal tooltip line)
 *
 * IMPORTANT: Forge/vanilla sometimes passes an immutable tooltip component list (List.of(...)).
 * We must replace it with a mutable copy before inserting spacer components, or the game will crash.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTooltipBorderMixin {

    @Shadow public abstract int guiWidth();
    @Shadow public abstract int guiHeight();

    // Added by Forge patch on 1.20.x; used to track which ItemStack the tooltip belongs to.
    @Shadow private ItemStack tooltipStack;

    @Unique private static final float TIERIFY_LABEL_SCALE = 0.65f;

    @Unique private static final int TIERIFY_SETBONUS_SPACER_PX = 7; // tuned to match Fabric's tighter header spacing at 0.65 scale
    @Unique private static final int TIERIFY_PERFECT_SPACER_PX = 7;

    @Unique private int tierify$textRenderIndex;
    @Unique private int tierify$tooltipX;
    @Unique private int tierify$tooltipWidth;
    @Unique private int tierify$centerTitleIndex = -1;

    /**
     * Lightweight tooltip component used only to reserve a small amount of vertical space.
     * Vanilla's ClientTextTooltip reserves a full font line (9px) even when empty, which is too tall for 0.65-scaled labels.
     */
    @Unique
    private static final class TierifySpacerComponent implements ClientTooltipComponent {
        private final int height;

        private TierifySpacerComponent(int height) {
            this.height = Math.max(0, height);
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getWidth(Font font) {
            return 0;
        }
    }


    /**
     * Ensure tooltip components list is mutable, and inject a spacer after the title if Perfect.
     *
     * This fixes the crash:
     * UnsupportedOperationException when calling List.add(...) on an immutable list.
     */
    @ModifyVariable(
            method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
        private List<ClientTooltipComponent> tierify$makeComponentsMutable(List<ClientTooltipComponent> components) {
        if (!ForgeTierifyConfig.tieredTooltip()) return components;
        if (components == null || components.isEmpty()) return components;

        ItemStack stack = this.tooltipStack;
        if (stack == null || stack.isEmpty()) return components;

        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return components;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        boolean allowSetBonusSpacer = !TooltipOverhaulCompatForge.isLoaded();
        boolean hasSetBonusLabel = allowSetBonusSpacer && buildSetBonusLabel(stack, tierId) != null;
        boolean isPerfect = tiered.getBoolean("Perfect");

        // Only allocate/copy if we actually need to mutate the list.
        if (!hasSetBonusLabel && !isPerfect) return components;

        List<ClientTooltipComponent> copy = new ArrayList<>(components);

        // 1) If Set Bonus label is going to render, reserve a full line ABOVE the title.
        //    This matches Fabric behavior (label is inside the tooltip box, not floating above it).
        if (hasSetBonusLabel) {
            copy.add(0, new TierifySpacerComponent(TIERIFY_SETBONUS_SPACER_PX));
        }

        // 2) If Perfect label is going to render, reserve a full line AFTER the title.
        //    Title is at index 0 normally, or index 1 if we inserted Set Bonus spacer.
        if (isPerfect) {
            int titleIndex = hasSetBonusLabel ? 1 : 0;
            int insertAt = Math.min(titleIndex + 1, copy.size());
            copy.add(insertAt, new TierifySpacerComponent(TIERIFY_PERFECT_SPACER_PX));
        }

        return copy;
    }

    @Inject(
            method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At("HEAD")
    )
    private void tierify$prepareTitleCentering(Font font,
                                               List<ClientTooltipComponent> components,
                                               int mouseX,
                                               int mouseY,
                                               ClientTooltipPositioner positioner,
                                               CallbackInfo ci) {
        tierify$textRenderIndex = 0;
        tierify$centerTitleIndex = -1;

        if (!ForgeTierifyConfig.centerName()) return;
        if (!ForgeTierifyConfig.tieredTooltip() || TooltipOverhaulCompatForge.isLoaded()) return;
        if (components == null || components.isEmpty()) return;

        ItemStack stack = this.tooltipStack;
        if (stack == null || stack.isEmpty()) return;

        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = tiered.getBoolean("Perfect");
        if ((tierId == null || tierId.isEmpty()) && !isPerfect) return;

        int titleIndex = (components.get(0) instanceof TierifySpacerComponent) ? 1 : 0;
        if (titleIndex >= components.size()) return;

        int w = 0;
        int h = (components.size() == 1) ? -2 : 0;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            w = Math.max(w, c.getWidth(font));
            h += c.getHeight();
            if (i == 0) h += 2;
        }
        w = Math.max(w, 64);
        h = Math.max(h, 16);

        Vector2ic pos = positioner.positionTooltip(guiWidth(), guiHeight(), mouseX, mouseY, w, h);
        tierify$tooltipX = pos.x();
        tierify$tooltipWidth = w;
        tierify$centerTitleIndex = titleIndex;
    }

    @Redirect(
            method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;renderText(Lnet/minecraft/client/gui/Font;IILorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)V"
            )
    )
    private void tierify$centerTitleText(ClientTooltipComponent component,
                                         Font font,
                                         int x,
                                         int y,
                                         Matrix4f matrix,
                                         MultiBufferSource.BufferSource buffer) {
        int index = tierify$textRenderIndex++;
        if (index == tierify$centerTitleIndex && tierify$centerTitleIndex >= 0) {
            int textW = component.getWidth(font);
            int centeredX = tierify$tooltipX + (tierify$tooltipWidth - textW) / 2;
            component.renderText(font, centeredX, y, matrix, buffer);
            return;
        }
        component.renderText(font, x, y, matrix, buffer);
    }

    @Inject(
            method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawManaged(Ljava/lang/Runnable;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void tierify$afterVanillaTooltipBackground(Font font,
                                                      List<ClientTooltipComponent> components,
                                                      int mouseX,
                                                      int mouseY,
                                                      ClientTooltipPositioner positioner,
                                                      CallbackInfo ci) {
        if (!ForgeTierifyConfig.tieredTooltip() || TooltipOverhaulCompatForge.isLoaded()) return;
        if (components == null || components.isEmpty()) return;

        ItemStack stack = this.tooltipStack;
        if (stack == null || stack.isEmpty()) return;

        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = tiered.getBoolean("Perfect");

        if ((tierId == null || tierId.isEmpty()) && !isPerfect) return;

        // Compute tooltip bounds (mirrors the vanilla logic used for positioning).
        int w = 0;
        int h = (components.size() == 1) ? -2 : 0;

        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            w = Math.max(w, c.getWidth(font));
            h += c.getHeight();
            if (i == 0) h += 2;
        }

        // Vanilla minimums (keeps consistent positioner behavior).
        w = Math.max(w, 64);
        h = Math.max(h, 16);

        Vector2ic pos = positioner.positionTooltip(guiWidth(), guiHeight(), mouseX, mouseY, w, h);
        int x = pos.x();
        int y = pos.y();

        GuiGraphics gg = (GuiGraphics) (Object) this;

        // 1) Border overlay
        int tierIndex = TierGradientAnimatorForge.getTierFromId(tierId);
        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, 400.0F);
        try {
            TierifyTooltipBorderRendererForge.render(
                    gg,
                    x, y,
                    w, h,
                    tierId,
                    tierIndex,
                    isPerfect
            );
        } finally {
            gg.pose().popPose();
        }

        // 2) Overlay labels (scaled/centered).
        renderSetBonusLabel(gg, font, x, y, w, components, stack, tierId);
        renderPerfectLabel(gg, font, x, y, w, components, stack, tierId);
    }


    @Unique
    private static float textTopYForIndex(int tooltipTopY, List<ClientTooltipComponent> components, int index) {
        if (components == null || components.isEmpty()) return tooltipTopY + 4.0f;

        // Vanilla inserts an extra +2px gap after the *title* component (the first real text line).
        // If we inject a Tierify spacer above the title, the title becomes index 1.
        int titleIndex = (components.get(0) instanceof TierifySpacerComponent) ? 1 : 0;

        int clamped = Math.max(0, Math.min(index, components.size()));
        float y = tooltipTopY + 4.0f;
        for (int i = 0; i < clamped; i++) {
            y += components.get(i).getHeight();
            if (i == titleIndex) y += 2.0f;
        }
        return y;
    }


    private static void renderSetBonusLabel(GuiGraphics gg, Font font, int x, int y, int w, List<ClientTooltipComponent> components, ItemStack stack, String tierId) {
        Component label = buildSetBonusLabel(stack, tierId);
        if (label == null) return;

        float scale = TIERIFY_LABEL_SCALE;
        int textW = font.width(label);
        float scaledW = textW * scale;

        float xPos = x + (w - scaledW) / 2.0f;

        float lineH = font.lineHeight;
        float scaledH = lineH * scale;

        // Center within our injected spacer height (not a full vanilla text line).
        int spacerH = (components.get(0) instanceof TierifySpacerComponent s) ? s.getHeight() : font.lineHeight;

        float lineTopY = textTopYForIndex(y, components, 0);
        float yPos = lineTopY + (spacerH - scaledH) / 2.0f - 3.0f; // nudge up 2px (slightly more breathing room from title)

        gg.pose().pushPose();
        gg.pose().translate(xPos, yPos, 450.0f);
        gg.pose().scale(scale, scale, 1.0f);
        gg.drawString(font, label, 0, 0, 0xFFFFFF, false);
        gg.pose().popPose();
    }

    private static void renderPerfectLabel(GuiGraphics gg, Font font, int x, int y, int w, List<ClientTooltipComponent> components, ItemStack stack, String tierId) {
        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return;
        if (!tiered.getBoolean("Perfect")) return;

        boolean hasSetBonusLabel = buildSetBonusLabel(stack, tierId) != null;
        int titleIndex = hasSetBonusLabel ? 1 : 0;
        int perfectSpacerIndex = titleIndex + 1;

        // Render centered within the spacer line injected after the title.
        float scale = TIERIFY_LABEL_SCALE;

        MutableComponent perfect = PerfectLabelAnimatorForge.animatedLabel(Util.getMillis());

        int textW = font.width(perfect);
        float scaledW = textW * scale;

        float xPos = x + (w - scaledW) / 2.0f;

        float lineH = font.lineHeight;
        float scaledH = lineH * scale;

        int spacerH = (perfectSpacerIndex >= 0 && perfectSpacerIndex < components.size() && components.get(perfectSpacerIndex) instanceof TierifySpacerComponent s)
                ? s.getHeight()
                : font.lineHeight;

        float lineTopY = textTopYForIndex(y, components, perfectSpacerIndex);
        float yPos = lineTopY + (spacerH - scaledH) / 2.0f - 2.0f; // nudge up 2px (closer to title)

        gg.pose().pushPose();
        gg.pose().translate(xPos, yPos, 450.0f);
        gg.pose().scale(scale, scale, 1.0f);
        gg.drawString(font, perfect, 0, 0, 0xFFFFFF, false);
        gg.pose().popPose();
    }

    @Nullable
    private static Component buildSetBonusLabel(ItemStack stack, String tierId) {
        if (tierId == null || tierId.isEmpty()) return null;
        if (!(stack.getItem() instanceof ArmorItem armor)) return null;
        if (!ForgeTierifyConfig.enableArmorSetBonuses()) return null;

        Player player = Minecraft.getInstance().player;
        if (player == null) return null;

        // Only show if the hovered stack IS the equipped stack instance.
        EquipmentSlot slot = armor.getEquipmentSlot();
        if (player.getItemBySlot(slot) != stack) return null;

        if (hasPerfectSetBonus(player, tierId)) {
            int pct = Math.round(ForgeTierifyConfig.armorSetPerfectBonusPercent() * 100.0f);
            return Component.literal("Perfect Set Bonus (+" + pct + "%)")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }

        if (hasSetBonus(player, tierId)) {
            int pct = Math.round(ForgeTierifyConfig.armorSetBonusMultiplier() * 100.0f);
            return Component.literal("Set Bonus (+" + pct + "%)")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }

        return null;
    }

    private static boolean hasSetBonus(Player player, String tierId) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) return false;

            CompoundTag tiered = armor.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (tiered == null) return false;

            String id = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
            if (!tierId.equals(id)) return false;
        }
        return true;
    }

    private static boolean hasPerfectSetBonus(Player player, String tierId) {
        if (!hasSetBonus(player, tierId)) return false;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) return false;

            CompoundTag tiered = armor.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (tiered == null) return false;

            if (!tiered.getBoolean("Perfect")) return false;
        }

        return true;
    }
}
