package elocindev.tierify.forge.client;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.mixin.client.AbstractContainerScreenAccessor;
import elocindev.tierify.forge.network.ForgeNetwork;
import elocindev.tierify.forge.network.c2s.OpenAnvilFromReforgeC2S;
import elocindev.tierify.forge.network.c2s.OpenReforgeFromAnvilC2S;
import elocindev.tierify.forge.screen.client.ReforgeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ForgeScreenTabs {

    private static final ResourceLocation ANVIL_ICON =
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "textures/gui/anvil_tab_icon.png");
    private static final ResourceLocation REFORGE_ICON =
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "textures/gui/reforge_tab_icon.png");
    private static final ResourceLocation TAB_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("libz", "textures/gui/icons.png");

    private static final int TAB_WIDTH = 24;
    private static final int TAB_HEIGHT_SELECTED = 27;
    private static final int TAB_HEIGHT_FIRST = 25;
    private static final int TAB_HEIGHT_OTHER = 21;
    private static final int TAB_SPACING = 25;
    private static final int ICON_SIZE = 14;

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> acs)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        boolean isAnvil = e.getScreen() instanceof AnvilScreen || isModAnvilScreen(e.getScreen());
        boolean isReforge = e.getScreen() instanceof ReforgeScreen;
        if (!isAnvil && !isReforge) return;

        boolean showReforgeTab = ForgeTierifyConfig.showReforgingTab();
        var acc = (AbstractContainerScreenAccessor) acs;
        int left = acc.tierify$getLeftPos();
        int top = acc.tierify$getTopPos();

        int x = left;
        Component hoverTitle = null;

        hoverTitle = renderTab(e.getGuiGraphics(), left, top, x, true, isAnvil,
                ANVIL_ICON, Component.translatable("container.repair"), e.getMouseX(), e.getMouseY(), hoverTitle);
        x += TAB_SPACING;

        if (showReforgeTab) {
            hoverTitle = renderTab(e.getGuiGraphics(), left, top, x, false, isReforge,
                    REFORGE_ICON, Component.translatable("screen.tiered.reforging_screen"), e.getMouseX(), e.getMouseY(), hoverTitle);
        }

        if (hoverTitle != null) {
            e.getGuiGraphics().renderTooltip(mc.font, hoverTitle, e.getMouseX(), e.getMouseY());
        }
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!(e.getScreen() instanceof AbstractContainerScreen<?> acs)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        boolean isAnvil = e.getScreen() instanceof AnvilScreen || isModAnvilScreen(e.getScreen());
        boolean isReforge = e.getScreen() instanceof ReforgeScreen;
        if (!isAnvil && !isReforge) return;

        boolean showReforgeTab = ForgeTierifyConfig.showReforgingTab();
        var acc = (AbstractContainerScreenAccessor) acs;
        int left = acc.tierify$getLeftPos();
        int top = acc.tierify$getTopPos();

        int x = left;
        boolean clicked = false;

        if (!isAnvil && isPointWithinBounds(left, top, x - left + 1, -20, 22, 19, e.getMouseX(), e.getMouseY())) {
            ForgeNetwork.CHANNEL.sendToServer(new OpenAnvilFromReforgeC2S());
            clicked = true;
        }
        x += TAB_SPACING;

        if (showReforgeTab && !isReforge
                && isPointWithinBounds(left, top, x - left + 1, -20, 22, 19, e.getMouseX(), e.getMouseY())) {
            ForgeNetwork.CHANNEL.sendToServer(new OpenReforgeFromAnvilC2S());
            clicked = true;
        }

        if (clicked) {
            e.setCanceled(true);
        }
    }

    private static Component renderTab(
            GuiGraphics gg,
            int left,
            int top,
            int x,
            boolean first,
            boolean selected,
            ResourceLocation icon,
            Component title,
            double mouseX,
            double mouseY,
            Component hoverTitle
    ) {
        int u = first ? 24 : 72;
        if (selected) {
            u -= 24;
        }

        int drawY = selected ? (top - 23) : (top - 21);
        int height = selected ? TAB_HEIGHT_SELECTED : (first ? TAB_HEIGHT_FIRST : TAB_HEIGHT_OTHER);

        gg.blit(TAB_TEXTURE, x, drawY, u, 0, TAB_WIDTH, height, 256, 256);
        gg.blit(icon, x + 5, top - 16, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        if (!selected && hoverTitle == null
                && isPointWithinBounds(left, top, x - left + 1, -20, 22, 19, mouseX, mouseY)) {
            return title;
        }

        return hoverTitle;
    }

    private static boolean isPointWithinBounds(
            int left,
            int top,
            int x,
            int y,
            int width,
            int height,
            double mouseX,
            double mouseY
    ) {
        int leftEdge = left + x;
        int topEdge = top + y;
        return mouseX >= (double) leftEdge
                && mouseX < (double) (leftEdge + width)
                && mouseY >= (double) topEdge
                && mouseY < (double) (topEdge + height);
    }

    private static boolean isModAnvilScreen(Object screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return "fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen".equals(name);
    }

    private ForgeScreenTabs() {}
}
