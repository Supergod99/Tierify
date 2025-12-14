package elocindev.tierify.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.block.Blocks;

import java.util.Locale;

public final class TagFallbackMatcher {

    private TagFallbackMatcher() {}

    public static boolean matches(Identifier tagId, ItemStack stack) {
        if (tagId == null || stack == null || stack.isEmpty()) return false;

        Item item = stack.getItem();
        Identifier itemId = Registries.ITEM.getId(item);

        // if registry cannot resolve, do not guess.
        if (itemId == null) return false;

        String tag = tagId.toString();
        String path = itemId.getPath().toLowerCase(Locale.ROOT);

        // Tools
        if (tag.equals("minecraft:pickaxes") || tag.equals("c:pickaxes") || tag.equals("forge:tools/pickaxes")) {
            return (item instanceof PickaxeItem) || path.endsWith("pickaxe");
        }

        if (tag.equals("c:shovels") || tag.equals("forge:tools/shovels")) {
            return (item instanceof ShovelItem) || path.endsWith("shovel");
        }

        if (tag.equals("c:hoes") || tag.equals("forge:tools/hoes")) {
            return (item instanceof HoeItem) || path.endsWith("hoe");
        }

        // Paxels
        if (tag.equals("forge:tools/paxels")) {
            return path.contains("paxel");
        }

        // Ranged
        if (tag.equals("c:bows") || tag.equals("forge:weapons/bows")) {
            return (item instanceof BowItem) || path.endsWith("bow");
        }

        if (tag.equals("c:crossbows") || tag.equals("forge:weapons/crossbows")) {
            return (item instanceof CrossbowItem) || path.contains("crossbow");
        }

        // Shields
        if (tag.equals("c:shields") || tag.equals("forge:tools/shields")) {
            return (item instanceof ShieldItem) || path.endsWith("shield");
        }

        // Armor
        if (tag.equals("c:armors") || tag.startsWith("forge:armor/") || tag.endsWith("_armor")) {
            return (item instanceof ArmorItem) || (item instanceof ElytraItem) || (item instanceof HorseArmorItem);
        }

        // Melee weapons
        if (tag.equals("c:tools/melee_weapons") || tag.equals("c:melee_weapons") || tag.equals("forge:tools/weapons")) {
            if (item instanceof SwordItem) return true;
            if (item instanceof TridentItem) return true;
            if (item instanceof AxeItem) return true;
            if (item instanceof MiningToolItem) return false;
            
            // Exclude mining tools
            try {
                float stoneSpeed = stack.getMiningSpeedMultiplier(Blocks.STONE.getDefaultState());
                if (stoneSpeed > 1.0F) return false;
            } catch (Throwable t) {
                return false;
            }
        
            // if positive dmg in mainhand, classify as melee
            try {
                return stack.getAttributeModifiers(EquipmentSlot.MAINHAND)
                        .get(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                        .stream()
                        .anyMatch(mod -> mod.getValue() > 0.0D);
            } catch (Throwable t) {
                return false;
            }
        }

        return false;
    }
}
