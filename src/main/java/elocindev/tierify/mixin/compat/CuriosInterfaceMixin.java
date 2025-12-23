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

@Mixin(ICurioItem.class)
public class CuriosInterfaceMixin {

    // We use remap = false because this is an external library method.
    // We strictly use the NAME only, to avoid descriptor mismatches (Yarn vs Intermediary).
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true, remap = false)
    private void fixBrutalityStacking(SlotContext slotContext, UUID uuid, ItemStack stack, CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir) {
        
        // 1. SAFETY CHECK
        if (stack == null || stack.isEmpty()) return;
        String itemId = stack.getItem().toString(); 
        if (!itemId.contains("brutality")) {
            return;
        }

        Multimap<EntityAttribute, EntityAttributeModifier> originalMap = cir.getReturnValue();
        if (originalMap == null || originalMap.isEmpty()) return;
        // 2. THE FIX
        ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> newMap = ImmutableMultimap.builder();
        originalMap.forEach((attribute, modifier) -> {
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
