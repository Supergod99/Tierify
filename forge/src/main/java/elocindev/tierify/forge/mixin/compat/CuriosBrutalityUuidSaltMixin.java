package elocindev.tierify.forge.mixin.compat;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Pseudo
@Mixin(targets = "top.theillusivec4.curios.mixin.CuriosImplMixinHooks", remap = false)
public abstract class CuriosBrutalityUuidSaltMixin {

    private static final ResourceLocation BRUTALITY_LETHALITY = new ResourceLocation("brutality", "lethality");

    @Inject(
            method = "getAttributeModifiers",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void echelon$saltBrutalityLethalityUuids(
            @Coerce Object slotContext,
            UUID slotUuid,
            ItemStack stack,
            CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir
    ) {
        if (stack == null || stack.isEmpty()) return;

        Multimap<Attribute, AttributeModifier> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId == null || !"brutality".equals(itemId.getNamespace())) {
            return;
        }

        String slotIdentifier = invokeString(slotContext, "identifier");
        int slotIndex = invokeInt(slotContext, "index");

        boolean changed = false;
        Multimap<Attribute, AttributeModifier> out = LinkedHashMultimap.create();

        for (Map.Entry<Attribute, AttributeModifier> e : original.entries()) {
            Attribute attr = e.getKey();
            AttributeModifier mod = e.getValue();

            if (attr == null || mod == null) {
                out.put(attr, mod);
                continue;
            }

            ResourceLocation attrId = ForgeRegistries.ATTRIBUTES.getKey(attr);

            if (BRUTALITY_LETHALITY.equals(attrId)) {
                String salt = slotUuid.toString()
                        + "|" + mod.getId()
                        + "|" + slotIdentifier
                        + "|" + slotIndex
                        + "|" + itemId;

                UUID unique = UUID.nameUUIDFromBytes(salt.getBytes(StandardCharsets.UTF_8));

                mod = new AttributeModifier(
                        unique,
                        mod.getName(),
                        mod.getAmount(),
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

    private static String invokeString(Object target, String methodName) {
        if (target == null) return "unknown";
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result == null ? "unknown" : String.valueOf(result);
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static int invokeInt(Object target, String methodName) {
        if (target == null) return -1;
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }
}
