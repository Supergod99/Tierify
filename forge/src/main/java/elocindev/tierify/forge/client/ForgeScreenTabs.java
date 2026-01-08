package elocindev.tierify.forge.client;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.mixin.client.AbstractContainerScreenAccessor;
import elocindev.tierify.forge.network.ForgeNetwork;
import elocindev.tierify.forge.network.c2s.OpenAnvilFromReforgeC2S;
import elocindev.tierify.forge.network.c2s.OpenReforgeFromAnvilC2S;
import elocindev.tierify.forge.screen.client.ReforgeScreen;
import net.minecraft.client.gui.components.ImageButton;
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

    // Placement: “just outside” the top-left of the container background
    private static int tabX(int leftPos) { return leftPos - 26 + ForgeTierifyConfig.xIconPosition(); }
    private static int anvilTabY(int topPos) { return topPos + 4 + ForgeTierifyConfig.yIconPosition(); }
    private static int reforgeTabY(int topPos) { return topPos + 26 + ForgeTierifyConfig.yIconPosition(); }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> acs)) return;
        if (e.getScreen().getClass().getName().contains("ReforgeTabInjected")) return;


        var acc = (AbstractContainerScreenAccessor) acs;
        int left = acc.tierify$getLeftPos();
        int top  = acc.tierify$getTopPos();
        boolean showReforgeTab = ForgeTierifyConfig.showReforgingTab();

        boolean isAnvil = e.getScreen() instanceof AnvilScreen || isModAnvilScreen(e.getScreen());

        // In Anvil: show both tabs, Reforge tab is clickable
        if (isAnvil) {
            ImageButton anvilTab = new ImageButton(
                    tabX(left), anvilTabY(top), 20, 20,
                    0, 0, 20,
                    ANVIL_ICON, 20, 20,
                    b -> {},
                    Component.translatable("container.repair")
            );
            anvilTab.active = false;
            e.addListener(anvilTab);

            if (showReforgeTab) {
                ImageButton reforgeTab = new ImageButton(
                        tabX(left), reforgeTabY(top), 20, 20,
                        0, 0, 20,
                        REFORGE_ICON, 20, 20,
                        b -> ForgeNetwork.CHANNEL.sendToServer(new OpenReforgeFromAnvilC2S()),
                        Component.translatable("container.reforge")
                );
                e.addListener(reforgeTab);
            }
        }

        // In Reforge: show both tabs, Anvil tab is clickable
        if (e.getScreen() instanceof ReforgeScreen) {
            ImageButton anvilTab = new ImageButton(
                    tabX(left), anvilTabY(top), 20, 20,
                    0, 0, 20,
                    ANVIL_ICON, 20, 20,
                    b -> ForgeNetwork.CHANNEL.sendToServer(new OpenAnvilFromReforgeC2S()),
                    Component.translatable("container.repair")
            );
            e.addListener(anvilTab);

            if (showReforgeTab) {
                ImageButton reforgeTab = new ImageButton(
                        tabX(left), reforgeTabY(top), 20, 20,
                        0, 0, 20,
                        REFORGE_ICON, 20, 20,
                        b -> {},
                        Component.translatable("container.reforge")
                );
                reforgeTab.active = false;
                e.addListener(reforgeTab);
            }
        }
    }

    private static boolean isModAnvilScreen(Object screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        return "fuzs.easyanvils.client.gui.screens.inventory.ModAnvilScreen".equals(name);
    }

    private ForgeScreenTabs() {}
}
