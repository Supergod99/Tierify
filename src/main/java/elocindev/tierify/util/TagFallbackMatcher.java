package elocindev.tierify.util;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Second-layer category matcher used when item tags are missing or incomplete.
 *
 * This is intentionally conservative: it only attempts to infer membership for a small set of
 * "broad" categories that Tierify uses as verifier tags (pickaxes/shovels/hoes/shields/armors/bows/melee).
 */
public final class TagFallbackMatcher {

    private TagFallbackMatcher() {}

    public static boolean matches(Identifier tagId, ItemStack stack) {
        if (tagId == null || stack == null || stack.isEmpty()) return false;

        Item item = stack.getItem();
        Identifier itemId = Registries.ITEM.getId(item);

        // Defensive: if registry cannot resolve, do not guess.
        if (itemId == null) return false;

        String tag = tagId.toString();
        String path = itemId.getPath().toLowerCase(Locale.ROOT);

        // --- Tools ---
        if (tag.equals("minecraft:pickaxes") || tag.equals("c:pickaxes") || tag.equals("forge:tools/pickaxes")) {
            return (item instanceof PickaxeItem) || path.endsWith("pickaxe");
        }

        if (tag.equals("c:shovels") || tag.equals("forge:tools/shovels")) {
            return (item instanceof ShovelItem) || path.endsWith("shovel");
        }

        if (tag.equals("c:hoes") || tag.equals("forge:tools/hoes")) {
            return (item instanceof HoeItem) || path.endsWith("hoe");
        }

        // Paxels show up frequently in modpacks but are inconsistent about tagging.
        if (tag.equals("forge:tools/paxels")) {
            return path.contains("paxel");
        }

        // --- Ranged ---
        if (tag.equals("c:bows") || tag.equals("forge:weapons/bows")) {
            return (item instanceof BowItem) || (stack.getUseAction() == UseAction.BOW && !path.contains("crossbow"));
        }

        if (tag.equals("c:crossbows") || tag.equals("forge:weapons/crossbows")) {
            return (item instanceof CrossbowItem) || path.contains("crossbow");
        }

        // --- Shields ---
        if (tag.equals("c:shields") || tag.equals("forge:tools/shields")) {
            return (item instanceof ShieldItem) || path.endsWith("shield");
        }

        // --- Armor / equipables ---
        // Handles the majority of "real armor" items out of the box. Weird chest-slot items
        // (elytras, backpacks, jetpacks) should be covered by data tags you ship (values required:false).
        if (tag.equals("c:armors") || tag.startsWith("forge:armor/") || tag.endsWith("_armor")) {
            return (item instanceof ArmorItem) || (item instanceof ElytraItem) || (item instanceof HorseArmorItem);
        }

        // --- Melee weapons ---
        // First, hard-typed melee.
        if (tag.equals("c:tools/melee_weapons") || tag.equals("c:melee_weapons") || tag.equals("forge:tools/weapons")) {
            if (item instanceof SwordItem) return true;
            if (item instanceof AxeItem) return true;
            if (item instanceof TridentItem) return true;

            // Do NOT treat generic mining tools as melee in the fallback (axes are already handled above).
            if (item instanceof MiningToolItem) return false;

            // Attribute-based heuristic: if an item provides positive attack damage in mainhand, treat as melee.
            // This catches many modded "weapon" classes that aren't SwordItem.
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
