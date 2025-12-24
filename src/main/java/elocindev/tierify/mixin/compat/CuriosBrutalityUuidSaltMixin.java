package elocindev.tierify.mixin.compat;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

@Mixin(targets = "top.theillusivec4.curios.mixin.CuriosImplMixinHooks", remap = false)
public abstract class CuriosBrutalityUuidSaltMixin {

    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true, require = 0)
    private static void echelon$rewriteBrutalityCurioModifierUuids(
            SlotContext slotContext, UUID slotUuid, ItemStack stack,
            CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir
    ) {
        if (stack == null || stack.isEmpty()) return;

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return;

        // Only touch Brutality items to minimize side effects.
        if (!"brutality".equals(itemId.getNamespace())) return;

        Multimap<Attribute, AttributeModifier> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        Multimap<Attribute, AttributeModifier> out = LinkedHashMultimap.create();

        // Slot UUID from Curios is already per-slot/per-stack stable; use it as the salt base.
        String saltBase = slotUuid.toString() + "|" + itemId;

        for (Map.Entry<Attribute, AttributeModifier> e : original.entries()) {
            Attribute attr = e.getKey();
            AttributeModifier mod = e.getValue();

            // Derive a stable, unique UUID per-slot per-modifier.
            UUID newId = UUID.nameUUIDFromBytes(
                    (saltBase + "|" + mod.getId().toString()).getBytes(StandardCharsets.UTF_8)
            );

            out.put(attr, new AttributeModifier(newId, mod.getName(), mod.getAmount(), mod.getOperation()));
        }

        cir.setReturnValue(out);
    }
}
