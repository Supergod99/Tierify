package elocindev.tierify.forge.screen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.network.ForgeNetwork;
import elocindev.tierify.forge.network.c2s.TryReforgeC2S;
import elocindev.tierify.forge.reforge.ForgeReforgeData;
import elocindev.tierify.forge.screen.ReforgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ReforgeScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<ReforgeMenu> {

    private static final ResourceLocation TEX =
            ResourceLocation.fromNamespaceAndPath("tiered", "textures/gui/reforging_screen.png");

    private static final TagKey<Item> TAG_REFORGE_BASE_ITEM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_base_item")
    );

    private ReforgeButton reforgeButton;
    private ItemStack lastTarget = ItemStack.EMPTY;
    private List<Item> baseItems = Collections.emptyList();

    public ReforgeScreen(ReforgeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 60;
    }

    @Override
    protected void init() {
        super.init();   

        int left = this.leftPos;
        int top = this.topPos;

        // Fabric: (i + 79, j + 56), 18x18 sprite-button :contentReference[oaicite:3]{index=3}
        this.reforgeButton = new ReforgeButton(left + 79, top + 56);
        this.addRenderableWidget(this.reforgeButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (reforgeButton != null) {
            reforgeButton.setDisabled(!menu.isReforgeReady());
        }
    }


    @Override
    protected void renderBg(GuiGraphics gg, float partial, int mouseX, int mouseY) {
        gg.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partial) {
        renderBackground(gg);
        super.render(gg, mouseX, mouseY, partial);
        renderTooltip(gg, mouseX, mouseY);

        if (isPointWithinBounds(79, 56, 18, 18, mouseX, mouseY)) {
            ItemStack target = menu.getSlot(1).getItem();
            if (target.isEmpty()) {
                baseItems = Collections.emptyList();
                lastTarget = target;
            } else if (target != lastTarget) {
                lastTarget = target;
                baseItems = new ArrayList<>();

                Set<Item> items = ForgeReforgeData.getBaseItemsForTarget(target.getItem());
                if (!items.isEmpty()) {
                    baseItems.addAll(items);
                } else if (target.getItem() instanceof TieredItem tool) {
                    var ing = tool.getTier().getRepairIngredient();
                    ItemStack[] matches = (ing == null) ? new ItemStack[0] : ing.getItems();
                    if (matches.length > 0) {
                        for (ItemStack stack : matches) {
                            baseItems.add(stack.getItem());
                        }
                    } else {
                        BuiltInRegistries.ITEM.getTag(TAG_REFORGE_BASE_ITEM)
                                .ifPresent(tag -> tag.forEach(holder -> baseItems.add(holder.value())));
                    }
                } else if (target.getItem() instanceof ArmorItem armor) {
                    var ing = armor.getMaterial().getRepairIngredient();
                    ItemStack[] matches = (ing == null) ? new ItemStack[0] : ing.getItems();
                    if (matches.length > 0) {
                        for (ItemStack stack : matches) {
                            baseItems.add(stack.getItem());
                        }
                    } else {
                        BuiltInRegistries.ITEM.getTag(TAG_REFORGE_BASE_ITEM)
                                .ifPresent(tag -> tag.forEach(holder -> baseItems.add(holder.value())));
                    }
                } else {
                    BuiltInRegistries.ITEM.getTag(TAG_REFORGE_BASE_ITEM)
                            .ifPresent(tag -> tag.forEach(holder -> baseItems.add(holder.value())));
                }
            }

            List<Component> tooltip = new ArrayList<>();
            if (!baseItems.isEmpty()) {
                ItemStack ingredient = menu.getSlot(0).getItem();
                if (ingredient.isEmpty() || !baseItems.contains(ingredient.getItem())) {
                    tooltip.add(Component.translatable("screen.tiered.reforge_ingredient"));
                    for (Item item : baseItems) {
                        tooltip.add(new ItemStack(item).getHoverName());
                    }
                }
            }

            if (!ForgeTierifyConfig.allowReforgingDamaged()
                    && target.isDamageableItem()
                    && target.isDamaged()) {
                tooltip.add(Component.translatable("screen.tiered.reforge_damaged"));
            }

            if (!tooltip.isEmpty()) {
                List<FormattedCharSequence> lines = tooltip.stream()
                        .flatMap(c -> this.font.split(c, 200).stream())
                        .toList();
                gg.renderTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }

    /**
     * Forge port of Fabric's inner ReforgeButton:
     * - 18x18
     * - uses reforging_screen.png right-strip sprites:
     *   u = 176 + (hover ? 18 : 0) + (disabled ? 36 : 0) :contentReference[oaicite:5]{index=5}
     */
    private final class ReforgeButton extends AbstractWidget {
        private boolean disabled = true;

        private ReforgeButton(int x, int y) {
            super(x, y, 18, 18, Component.empty());
        }

        void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }

        @Override
        protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            int u = 176;
            if (disabled) {
                u += this.width * 2;
            } else if (this.isHovered()) {
                u += this.width;
            }

            // Texture is 256x256 in your asset; specify full size to avoid any blit overload mismatch.
            gg.blit(TEX, getX(), getY(), u, 0, this.width, this.height, 256, 256);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (!disabled && menu.isReforgeReady()) {
                ForgeNetwork.CHANNEL.sendToServer(new TryReforgeC2S());
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            // Optional: keep empty (Fabric uses ScreenTexts.EMPTY as label)
        }
    }

    private boolean isPointWithinBounds(int x, int y, int width, int height, int mouseX, int mouseY) {
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        return relX >= x && relX < x + width && relY >= y && relY < y + height;
    }
}
