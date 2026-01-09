package elocindev.tierify.forge.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

@Mixin(targets = "com.obscuria.obscureapi.client.TooltipBuilder$AttributeIcons", remap = false)
public class ObscureApiAttackSpeedIconMixin {

    @Inject(method = "getAttackSpeedIcon", at = @At("HEAD"), cancellable = true)
    private static void tierify$fixAttackSpeedIcon(Collection<?> modifier, CallbackInfoReturnable<String> cir) {
        if (modifier == null || modifier.isEmpty()) return;

        Double speed = computeAttackSpeedVanillaLike(modifier);
        if (speed == null) return;

        final String iconEnumName;
        if (speed >= 3.0) iconEnumName = "ATTACK_SPEED_VERY_FAST";
        else if (speed >= 2.0) iconEnumName = "ATTACK_SPEED_FAST";
        else if (speed >= 1.0) iconEnumName = "ATTACK_SPEED_MEDIUM";
        else if (speed > 0.6) iconEnumName = "ATTACK_SPEED_SLOW";
        else iconEnumName = "ATTACK_SPEED_VERY_SLOW";

        String icon = getObscureIcon(iconEnumName);
        if (icon == null || icon.isEmpty()) return;

        cir.setReturnValue(icon + "\u00A7r ");
    }

    private static Double computeAttackSpeedVanillaLike(Collection<?> mods) {
        final double base = 4.0;

        double add = 0.0;
        double multBase = 0.0;
        double multTotal = 1.0;

        boolean readAny = false;

        for (Object o : mods) {
            if (o == null) continue;

            Double amount = readModifierAmount(o);
            String opName = readModifierOperationName(o);

            if (amount == null || opName == null) continue;
            readAny = true;

            switch (opName) {
                case "ADDITION" -> add += amount;
                case "MULTIPLY_BASE" -> multBase += amount;
                case "MULTIPLY_TOTAL" -> multTotal *= (1.0 + amount);
                default -> {
                }
            }
        }

        if (!readAny) return null;

        double d0 = base + add;
        double d1 = d0 + (d0 * multBase);
        d1 *= multTotal;
        return d1;
    }

    private static Double readModifierAmount(Object mod) {
        Double v = invokeDoubleNoArgs(mod, "getValue");
        if (v != null) return v;

        v = invokeDoubleNoArgs(mod, "getAmount");
        if (v != null) return v;

        v = invokeDoubleNoArgs(mod, "m_22218_");
        if (v != null) return v;

        v = readDoubleField(mod, "value");
        if (v != null) return v;

        v = readDoubleField(mod, "amount");
        if (v != null) return v;

        return null;
    }

    private static String readModifierOperationName(Object mod) {
        Object op = invokeNoArgs(mod, "getOperation");
        if (op == null) {
            op = invokeNoArgs(mod, "m_22217_");
        }
        if (op == null) return null;

        if (op instanceof Enum<?> e) return e.name();

        try {
            Method name = op.getClass().getMethod("name");
            Object n = name.invoke(op);
            return (n instanceof String s) ? s : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Double invokeDoubleNoArgs(Object target, String methodName) {
        Object out = invokeNoArgs(target, methodName);
        if (out instanceof Number n) return n.doubleValue();
        return null;
    }

    private static Double readDoubleField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object out = f.get(target);
            if (out instanceof Number n) return n.doubleValue();
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String getObscureIcon(String enumConstantName) {
        try {
            Class<?> icons = Class.forName("com.obscuria.obscureapi.api.utils.Icons");
            Object constant = Enum.valueOf((Class<? extends Enum>) icons, enumConstantName);
            return (String) icons.getMethod("get").invoke(constant);
        } catch (Throwable t) {
            return null;
        }
    }
}
