package elocindev.tierify.forge.compat;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.client.PerfectLabelAnimatorForge;
import elocindev.tierify.forge.client.TierifyTooltipBorderRendererForge;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.awt.Point;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public final class TooltipOverhaulCompatForge {
    private static final String MOD_ID = "tooltipoverhaul";
    private static final boolean LOADED = ModList.get().isLoaded(MOD_ID);
    private static final float SET_BONUS_LABEL_NUDGE_Y = 4.0f;

    private static Object LAYER_PROXY;

    private TooltipOverhaulCompatForge() {}

    public static boolean isLoaded() {
        return LOADED;
    }

    public static void init() {
        if (!LOADED || LAYER_PROXY != null) return;
        try {
            Class<?> rendererClass = Class.forName("dev.xylonity.tooltipoverhaul.client.TooltipRenderer");
            Field layersField = rendererClass.getDeclaredField("LAYERS_MAIN");
            layersField.setAccessible(true);
            Object layersObj = layersField.get(null);
            if (!(layersObj instanceof List<?>)) return;
            @SuppressWarnings("unchecked")
            List<Object> layers = (List<Object>) layersObj;

            Class<?> layerInterface = Class.forName("dev.xylonity.tooltipoverhaul.client.layer.ITooltipLayer");
            Object proxy = Proxy.newProxyInstance(
                    layerInterface.getClassLoader(),
                    new Class<?>[]{layerInterface},
                    new LayerHandler()
            );

            layers.add(proxy);
            LAYER_PROXY = proxy;
        } catch (Throwable ignored) {
        }
    }

    private static final class LayerHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("render".equals(name) && args != null && args.length >= 7) {
                renderLayer(args);
                return null;
            }

            if ("getDepth".equals(name) || "depth".equals(name) || "layerDepth".equals(name)) {
                Object depth = resolveLayerDepth();
                if (depth != null && method.getReturnType().isInstance(depth)) return depth;
                return null;
            }

            Class<?> ret = method.getReturnType();
            if (ret == boolean.class) return false;
            if (ret == byte.class) return (byte) 0;
            if (ret == short.class) return (short) 0;
            if (ret == int.class) return 0;
            if (ret == long.class) return 0L;
            if (ret == float.class) return 0.0f;
            if (ret == double.class) return 0.0d;
            if (ret == char.class) return '\0';
            return null;
        }
    }

    private static Object resolveLayerDepth() {
        try {
            Class<?> depthClass = Class.forName("dev.xylonity.tooltipoverhaul.client.layer.LayerDepth");
            if (depthClass.isEnum()) {
                @SuppressWarnings("unchecked")
                Object val = Enum.valueOf((Class<Enum>) depthClass, "BACKGROUND_OVERLAY");
                return val;
            }
            Field f = depthClass.getField("BACKGROUND_OVERLAY");
            return f.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float resolveLayerDepthZ() {
        Object depth = resolveLayerDepth();
        if (depth == null) return 400.0f;

        Float value = readDepthValue(depth, "getZ", "getZLevel", "getZIndex", "z");
        if (value != null) return value;

        value = readDepthField(depth, "z", "Z", "depth", "level");
        return value != null ? value : 400.0f;
    }

    private static Float readDepthValue(Object depth, String... names) {
        for (String name : names) {
            try {
                Method m = depth.getClass().getMethod(name);
                m.setAccessible(true);
                Object out = m.invoke(depth);
                if (out instanceof Number number) return number.floatValue();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Float readDepthField(Object depth, String... names) {
        for (String name : names) {
            try {
                Field f = depth.getClass().getField(name);
                f.setAccessible(true);
                Object out = f.get(depth);
                if (out instanceof Number number) return number.floatValue();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void renderLayer(Object[] args) {
        if (!ForgeTierifyConfig.tieredTooltip()) return;

        Object ctx = args[0];
        Object pos = args[1];
        Object size = args[2];
        Object fontObj = args[5];

        ItemStack stack = getItemStack(ctx);
        if (stack == null || stack.isEmpty()) return;

        CompoundTag tiered = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        boolean isPerfect = tiered.getBoolean("Perfect");
        if ((tierId == null || tierId.isEmpty()) && !isPerfect) return;

        TierifyTooltipBorderRendererForge.Template template = TierifyTooltipBorderRendererForge.findTemplate(tierId, isPerfect);
        if (template == null) return;

        GuiGraphics gg = getGuiGraphics(ctx);
        if (gg == null) return;

        int x = (int) Math.floor(readNumber(pos, "x", "getX"));
        int y = (int) Math.floor(readNumber(pos, "y", "getY"));
        int width = readPointValue(size, "x", "getX");
        int height = readPointValue(size, "y", "getY");

        float baseZ = resolveLayerDepthZ();

        gg.pose().pushPose();
        gg.pose().translate(0.0F, 0.0F, baseZ);
        TierifyTooltipBorderRendererForge.renderOverlay(gg, x, y, width, height, template);
        gg.pose().popPose();

        if (fontObj instanceof Font font) {
            renderSetBonusLabel(gg, font, x, y, width, stack, tierId, baseZ);
            if (isPerfect) {
                renderPerfectLabel(gg, font, x, y, width, baseZ);
            }
        }
    }

    private static ItemStack getItemStack(Object ctx) {
        Object stack = callNoArg(ctx, "stack", "getStack");
        if (stack instanceof ItemStack itemStack) return itemStack;
        stack = readField(ctx, "stack");
        return (stack instanceof ItemStack itemStack) ? itemStack : null;
    }

    private static GuiGraphics getGuiGraphics(Object ctx) {
        Object gg = callNoArg(ctx, "graphics", "getGraphics");
        if (gg instanceof GuiGraphics guiGraphics) return guiGraphics;
        gg = readField(ctx, "graphics");
        return (gg instanceof GuiGraphics guiGraphics) ? guiGraphics : null;
    }

    private static Object callNoArg(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object readField(Object target, String name) {
        if (target == null) return null;
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static float readNumber(Object target, String... names) {
        if (target == null) return 0.0f;
        for (String name : names) {
            Object value = readField(target, name);
            if (value instanceof Number number) return number.floatValue();
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                Object out = m.invoke(target);
                if (out instanceof Number number) return number.floatValue();
            } catch (Throwable ignored) {
            }
        }
        return 0.0f;
    }

    private static int readPointValue(Object target, String... names) {
        if (target instanceof Point point) {
            return "y".equalsIgnoreCase(names[0]) ? point.y : point.x;
        }
        float out = readNumber(target, names);
        return Math.round(out);
    }

    private static void renderSetBonusLabel(GuiGraphics gg, Font font, int bgX, int bgY, int bgWidth, ItemStack stack, String tierId, float baseZ) {
        Component label = buildSetBonusLabel(stack, tierId);
        if (label == null) return;

        float scale = 0.65f;
        int textWidth = font.width(label);
        float scaledWidth = textWidth * scale;
        float xPos = bgX + (bgWidth - scaledWidth) / 2f;

        float baseHeight = 9f;
        float scaledHeight = baseHeight * scale;

        float topPadding = 4f;
        float gapTop = bgY - 3f;
        float gapBottom = bgY + topPadding;

        float yPos = gapTop + ((gapBottom - gapTop) - scaledHeight) / 2f;
        float yOffset = (baseHeight - scaledHeight) / 2f;
        yPos += yOffset;
        yPos += SET_BONUS_LABEL_NUDGE_Y;

        gg.pose().pushPose();
        gg.pose().translate(xPos, yPos, baseZ + 10.0f);
        gg.pose().scale(scale, scale, 1.0f);
        gg.drawString(font, label, 0, 0, 0xFFFFFF, true);
        gg.pose().popPose();
    }

    private static void renderPerfectLabel(GuiGraphics gg, Font font, int bgX, int bgY, int bgWidth, float baseZ) {
        Component label = PerfectLabelAnimatorForge.animatedLabel(Util.getMillis());
        float scale = 0.65f;
        int textWidth = font.width(label);
        float centeredX = bgX + (bgWidth / 2.0f) - ((textWidth * scale) / 2.0f);
        float fixedY = bgY + 22.0f;

        gg.pose().pushPose();
        gg.pose().translate(centeredX, fixedY, baseZ + 10.0f);
        gg.pose().scale(scale, scale, 1.0f);
        gg.drawString(font, label, 0, 0, 0xFFFFFF, true);
        gg.pose().popPose();
    }

    private static Component buildSetBonusLabel(ItemStack stack, String tierId) {
        if (tierId == null || tierId.isEmpty()) return null;
        if (!(stack.getItem() instanceof ArmorItem armor)) return null;
        if (!ForgeTierifyConfig.enableArmorSetBonuses()) return null;

        Player player = Minecraft.getInstance().player;
        if (player == null) return null;

        EquipmentSlot slot = armor.getEquipmentSlot();
        if (player.getItemBySlot(slot) != stack) return null;

        if (hasPerfectSetBonus(player, tierId)) {
            int pct = Math.round(ForgeTierifyConfig.armorSetPerfectBonusPercent() * 100.0f);
            return Component.literal("Perfect Set Bonus (+" + pct + "%)")
                    .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
        }

        if (hasSetBonus(player, tierId)) {
            int pct = Math.round(ForgeTierifyConfig.armorSetBonusMultiplier() * 100.0f);
            return Component.literal("Set Bonus (+" + pct + "%)")
                    .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD);
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
