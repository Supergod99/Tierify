package elocindev.tierify.mixin.compat;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Pseudo
@Mixin(targets = "top.theillusivec4.curios.mixin.CuriosImplMixinHooks", remap = false)
public abstract class CuriosBrutalityUuidSaltMixin {

    private static final Identifier BRUTALITY_LETHALITY = new Identifier("brutality", "lethality");

    @Inject(
        method = "getAttributeModifiers",
        at = @At("RETURN"),
        cancellable = true,
        remap = false
    )
    private static void echelon$saltBrutalityLethalityUuids(
        SlotContext slotContext,
        UUID slotUuid,
        ItemStack stack,
        CallbackInfoReturnable<Multimap<EntityAttribute, EntityAttributeModifier>> cir
    ) {
        if (stack == null || stack.isEmpty()) return;

        Multimap<EntityAttribute, EntityAttributeModifier> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        // Optional extra narrowing: only do this for Brutality items.
        // (If you WANT non-brutality sources to stack too, remove this block.)
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (itemId == null || !"brutality".equals(itemId.getNamespace())) {
            return;
        }

        boolean changed = false;
        Multimap<EntityAttribute, EntityAttributeModifier> out = LinkedHashMultimap.create();

        for (Map.Entry<EntityAttribute, EntityAttributeModifier> e : original.entries()) {
            EntityAttribute attr = e.getKey();
            EntityAttributeModifier mod = e.getValue();

            if (attr == null || mod == null) {
                out.put(attr, mod);
                continue;
            }

            Identifier attrId = Registries.ATTRIBUTE.getId(attr);

            // Only rewrite Brutality lethality modifiers (minimal blast radius).
            if (BRUTALITY_LETHALITY.equals(attrId)) {
                // Deterministic per-slot salt:
                // - slotUuid: unique per equipped curio slot instance
                // - original modifier UUID: preserves separation between different modifiers
                // - slot identifier/index: extra entropy across Curios layouts
                // - itemId: prevents cross-item collisions in edge cases
                String salt = slotUuid.toString()
                    + "|" + mod.getId()
                    + "|" + slotContext.identifier()
                    + "|" + slotContext.index()
                    + "|" + itemId;

                UUID unique = UUID.nameUUIDFromBytes(salt.getBytes(StandardCharsets.UTF_8));

                mod = new EntityAttributeModifier(
                    unique,
                    mod.getName(),
                    mod.getValue(),
                    mod.getOperation()
                );
                changed = true;
            }

            out.put(attr, mod);
        }

        if (changed) {
            cir.setReturnValue(out);
        }
    }
}
