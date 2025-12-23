package elocindev.tierify.mixin.compat; 

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

// Target the base classes used by Brutality for Curios. 
@Mixin(targets = {
    "net.goo.brutality.item.base.BrutalityCurioItem",
    "net.goo.brutality.item.base.BrutalityAnkletItem",
    "net.goo.brutality.item.curios.charm.Greed", 
    "net.goo.brutality.item.curios.charm.Lust",
    "net.goo.brutality.item.curios.charm.ResplendentFeather" 
    // Add the specific "Lethality" charm class name here if known (e.g. AssassinCharm)
})
public class BrutalityStackingFixMixin {

    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true, remap = false)
    private void fixHardcodedUUIDs(SlotContext slotContext, UUID uuid, ItemStack stack, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        Multimap<Attribute, AttributeModifier> originalMap = cir.getReturnValue();

        if (originalMap.isEmpty()) return;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> newMap = ImmutableMultimap.builder();

        // Iterate over all modifiers added by the item
        originalMap.forEach((attribute, modifier) -> {
            // Generate a NEW unique UUID.
            // Formula: Hash of (Original Hardcoded UUID + Slot Identifier + Slot Index)
            // This guarantees that the same item in a different slot gets a unique ID.
            String salt = modifier.getId().toString() + ":" + slotContext.identifier() + ":" + slotContext.index();
            UUID uniqueID = UUID.nameUUIDFromBytes(salt.getBytes());

            AttributeModifier newModifier = new AttributeModifier(
                uniqueID,
                modifier.getName(),
                modifier.getAmount(),
                modifier.getOperation()
            );

            newMap.put(attribute, newModifier);
        });

        // Return the fixed map
        cir.setReturnValue(newMap.build());
    }
}
