package elocindev.tierify.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Collection;

@Mixin(targets = "com.obscuria.obscureapi.client.TooltipBuilder$AttributeIcons", remap = false)
public class ObscureApiAttributeIconMathMixin {

    @Inject(method = "getIcon", at = @At("HEAD"), cancellable = true)
    private static void tierify$fixGetIcon(boolean percent, String icon, double base, Collection modifier,
                                          CallbackInfoReturnable<String> cir) {
        if (modifier == null || modifier.isEmpty()) {
            cir.setReturnValue("");
            return;
        }

        Calc c = computeVanillaLike(base, modifier);
        if (!c.readAny || c.value == 0.0) {
            cir.setReturnValue("");
            return;
        }

        double shown = percent ? (c.value * 100.0) : c.value;
        String formatted = new DecimalFormat("##.#").format(shown).replace(".0", "");

        // Preserve Obscure API’s green if multiply_base > 0 behavior.
        String green = (c.multBase > 0.0) ? "§2" : "";

        cir.setReturnValue(icon + green + formatted + (percent ? "% " : " "));
    }

    private static final class Calc {
        boolean readAny;
        double add;
        double multBase;
        double multTotal = 1.0;
        double value;
    }

    private static Calc computeVanillaLike(double base, Collection mods) {
        Calc c = new Calc();

        for (Object m : mods) {
            if (m == null) continue;

            Double amount = readModifierAmount(m);
            String op = readModifierOperationName(m);

            if (amount == null || op == null) continue;
            c.readAny = true;

            switch (op) {
                case "ADDITION" -> c.add += amount;
                case "MULTIPLY_BASE" -> c.multBase += amount;
                case "MULTIPLY_TOTAL" -> c.multTotal *= (1.0 + amount);
                default -> { /* ignore */ }
            }
        }

        double d0 = base + c.add;
        double d1 = d0 + (d0 * c.multBase);
        d1 *= c.multTotal;
        c.value = d1;

        return c;
    }

    private static Double readModifierAmount(Object mod) {
        Double v = invokeDoubleNoArgs(mod, "getValue");
        if (v != null) return v;

        v = invokeDoubleNoArgs(mod, "getAmount");
        if (v != null) return v;

        v = invokeDoubleNoArgs(mod, "m_22218_");
        if (v != null) return v;

        return null;
    }

    private static String readModifierOperationName(Object mod) {
        Object op = invokeNoArgs(mod, "getOperation");
        if (op == null) op = invokeNoArgs(mod, "m_22217_");
        if (op == null) return null;

        if (op instanceof Enum<?> e) return e.name();

        // Defensive fallback: try name()
        try {
            Method name = op.getClass().getMethod("name");
            Object out = name.invoke(op);
            return (out instanceof String s) ? s : null;
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
        return (out instanceof Number n) ? n.doubleValue() : null;
    }
}
