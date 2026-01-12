package elocindev.tierify.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public final class TagFallbackMatcher {

    private TagFallbackMatcher() {}

    public static boolean matches(ResourceLocation tagId, ItemStack stack) {
        if (tagId == null || stack == null || stack.isEmpty()) return false;

        Item item = stack.getItem();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);

        if (itemId == null) return false;

        String tag = tagId.toString();
        String path = itemId.getPath().toLowerCase(Locale.ROOT);

        if (tag.equals("minecraft:pickaxes")
                || tag.equals("c:pickaxes")
                || tag.equals("forge:tools/pickaxes")) {
            return (item instanceof PickaxeItem) || path.endsWith("pickaxe");
        }

        if (tag.equals("c:shovels") || tag.equals("forge:tools/shovels")) {
            return (item instanceof ShovelItem) || path.endsWith("shovel");
        }

        if (tag.equals("c:hoes") || tag.equals("forge:tools/hoes")) {
            return (item instanceof HoeItem) || path.endsWith("hoe");
        }

        if (tag.equals("forge:tools/paxels")) {
            return path.contains("paxel");
        }

        if (tag.equals("c:bows") || tag.equals("forge:weapons/bows")) {
            return (item instanceof BowItem) || path.endsWith("bow");
        }

        if (tag.equals("c:crossbows") || tag.equals("forge:weapons/crossbows")) {
            return (item instanceof CrossbowItem) || path.contains("crossbow");
        }

        if (tag.equals("c:shields") || tag.equals("forge:tools/shields")) {
            return (item instanceof ShieldItem) || path.endsWith("shield");
        }

        if (tag.equals("c:armors") || tag.startsWith("forge:armor/") || tag.endsWith("_armor")) {
            return (item instanceof ArmorItem)
                    || (item instanceof ElytraItem)
                    || (item instanceof HorseArmorItem);
        }

        if (tag.equals("c:tools/melee_weapons")
                || tag.equals("c:melee_weapons")
                || tag.equals("forge:tools/weapons")) {
            if (item instanceof SwordItem) return true;
            if (item instanceof TridentItem) return true;
            if (item instanceof AxeItem) return true;
            if (item instanceof DiggerItem) return false;

            try {
                float stoneSpeed = stack.getDestroySpeed(Blocks.STONE.defaultBlockState());
                if (stoneSpeed > 1.0F) return false;
            } catch (Throwable t) {
                return false;
            }

            try {
                return stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                        .get(Attributes.ATTACK_DAMAGE)
                        .stream()
                        .anyMatch(mod -> mod.getAmount() > 0.0D);
            } catch (Throwable t) {
                return false;
            }
        }

        return false;
    }
}
