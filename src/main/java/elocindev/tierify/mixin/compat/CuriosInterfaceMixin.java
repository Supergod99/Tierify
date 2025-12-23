package elocindev.tierify.mixin.compat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

// CRITICAL FIX:
// 1. We use 'targets' (String) instead of ICurioItem.class to prevent the "is an interface" crash.
// 2. We set 'remap = false' so Mixin doesn't try to find Fabric mappings for this Forge interface.
@Mixin(targets = "top.theillusivec4.curios.api.type.capability.ICurioItem", remap = false)
public class CuriosInterfaceMixin {
    // We use the simple method name "getAttributeModifiers" to avoid signature mismatch errors.
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true, remap = false)
    private void fixBrutalityStacking(SlotContext slotContext, UUID uuid, ItemStack stack, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
        // 1. SAFETY CHECK
        // If stack is null/empty, or NOT a Brutality item, stop immediately.
        if (stack == null || stack.isEmpty()) return;
        String itemId = stack.getItem().toString(); 
        if (!itemId.contains("brutality")) {
            return;
        }
        Multimap<EntityAttribute, EntityAttributeModifier> originalMap = cir.getReturnValue();
        if (originalMap == null || originalMap.isEmpty()) return;
        // 2. THE FIX: Rebuild map with Unique UUIDs
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> newMap = ImmutableMultimap.builder();

        originalMap.forEach((attribute, modifier) -> {
            // Salt: Original UUID + Slot Name + Slot Index
            // This ensures every slot has a unique UUID for the attribute.
            String salt = modifier.getId().toString() + ":" + slotContext.identifier() + ":" + slotContext.index();
            UUID uniqueID = UUID.nameUUIDFromBytes(salt.getBytes());

            EntityAttributeModifier newModifier = new EntityAttributeModifier(
                uniqueID,
                modifier.getName(),
                modifier.getValue(),
                modifier.getOperation()
            );

            newMap.put(attribute, newModifier);
        });

        cir.setReturnValue(newMap.build());
    }
}
