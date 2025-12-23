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
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

// We target the Interface. This safely applies to ALL Curios items without 
// forcing the Brutality mod to load early (avoiding the KubeJS crash).
@Mixin(ICurioItem.class)
public class CuriosInterfaceMixin {

    @Inject(method = "getAttributeModifiers(Ltop/theillusivec4/curios/api/SlotContext;Ljava/util/UUID;Lnet/minecraft/item/ItemStack;)Lcom/google/common/collect/Multimap;", 
            at = @At("RETURN"), 
            cancellable = true, 
            remap = false)
    private void fixBrutalityStacking(SlotContext slotContext, UUID uuid, ItemStack stack, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
        // 1. PERFORMANCE & SAFETY CHECK
        // Only run for Brutality items. Using String check avoids loading Brutality classes.
        if (stack == null || stack.isEmpty()) return;
        
        // We check the Item ID string. If it's not a brutality item, we exit immediately.
        String itemId = stack.getItem().toString();
        if (!itemId.contains("brutality")) {
            return;
        }

        Multimap<EntityAttribute, EntityAttributeModifier> originalMap = cir.getReturnValue();
        if (originalMap == null || originalMap.isEmpty()) return;

        // 2. REBUILD MAP WITH UNIQUE UUIDS
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> newMap = ImmutableMultimap.builder();

        originalMap.forEach((attribute, modifier) -> {
            // Generate unique salt: "UUID:SlotID:SlotIndex"
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
