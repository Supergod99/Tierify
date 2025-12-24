package elocindev.tierify;

import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;

/**
 * Helper to avoid duplicate attribute injection in ModifyItemAttributeModifiersCallback,
 * without depending on AttributeTemplate internals.
 */
public final class TierifyCompatUtil {
    private TierifyCompatUtil() {}

    public static boolean hasSameModifierAlready(
            Multimap<EntityAttribute, EntityAttributeModifier> modifiers,
            EntityAttributeModifier candidate
    ) {
        if (modifiers == null || candidate == null) return false;

        for (EntityAttributeModifier existing : modifiers.values()) {
            // Best-case: stable UUID match
            if (existing.getId().equals(candidate.getId())) return true;

            // Fallback: name + op + amount match (handles cases where UUID differs/salted)
            if (existing.getOperation() == candidate.getOperation()
                    && Double.compare(existing.getValue(), candidate.getValue()) == 0
                    && existing.getName().equals(candidate.getName())) {
                return true;
            }
        }

        return false;
    }
}
