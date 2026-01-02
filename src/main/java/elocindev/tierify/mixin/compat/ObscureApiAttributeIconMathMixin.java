package elocindev.tierify.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.SetBonusUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.obscuria.obscureapi.client.TooltipBuilder$AttributeIcons", remap = false)
public class ObscureApiAttributeIconMathMixin {

    @Unique private static final ThreadLocal<ItemStack> TIERIFY$CURRENT_STACK = new ThreadLocal<>();

    @Unique private static final DecimalFormat TIERIFY$FMT = new DecimalFormat("0.##");

    // Lazily resolved Obscure API icon strings (so we don’t hard-depend on the enum at compile-time)
    @Unique private static String TIERIFY$ICON_ARMOR;
    @Unique private static String TIERIFY$ICON_TOUGHNESS;
    @Unique private static String TIERIFY$ICON_KNOCKBACK;

    @Inject(method = "putIcons", at = @At("HEAD"))
    private static void tierify$captureStack(List<?> list, @Coerce Object stackObj, CallbackInfo ci) {
        if (stackObj instanceof ItemStack stack) {
            TIERIFY$CURRENT_STACK.set(stack);
        } else {
            TIERIFY$CURRENT_STACK.remove();
        }
    }

    @Inject(method = "putIcons", at = @At("RETURN"))
    private static void tierify$clearStack(List<?> list, @Coerce Object stackObj, CallbackInfo ci) {
        TIERIFY$CURRENT_STACK.remove();
    }

    /**
     * Obscure’s AttributeIcons#getIcon returns the final rendered string for the icon + value.
     * We keep Obscure’s presentation, but correct the underlying math and optionally add Tierify set-bonus delta.
     */
    @ModifyReturnValue(method = "getIcon", at = @At("RETURN"))
    private static String tierify$fixIconMathAndApplySetBonus(
            String original,
            boolean isPercent,
            String icon,
            double base,
            Collection<?> modifiers
    ) {
        // If there are no modifiers, keep original (and avoid any surprises)
        if (icon == null || modifiers == null || modifiers.isEmpty()) return original;

        // Compute correct vanilla-like value from the modifier collection
        VanillaSums sums = tierify$sumModifiers(modifiers);

        // Apply set bonus delta ONLY for the three attributes Obscure shows on armor summary line
        SetBonusDelta delta = tierify$computeSetBonusDelta(icon, isPercent);
        if (delta != null) {
            sums.add += delta.add;
            sums.multBase += delta.multBase;
            sums.multTotal *= delta.multTotalFactor;
        }

        double value = tierify$computeVanillaLikeValue(base, sums.add, sums.multBase, sums.multTotal);

        // If Obscure would have hidden it, keep original behavior
        if (Math.abs(value) < 1.0e-9) return "";

        // Rebuild the string in the same general shape Obscure uses: "<icon><number><optional %> "
        // (Obscure’s icon itself already contains styling codes)
        String rendered = tierify$render(icon, value, isPercent);

        return rendered;
    }

    @Unique
    private static String tierify$render(String icon, double value, boolean percent) {
        double display = percent ? (value * 100.0) : value;

        // Match typical Obscure look: no explicit '+' for positive values; negatives keep '-'
        String num;
        if (Math.abs(display - Math.rint(display)) < 1.0e-9) {
            num = Long.toString(Math.round(display));
        } else {
            num = TIERIFY$FMT.format(display);
        }

        if (percent) num = num + "%";

        return icon + num + " ";
    }

    @Unique
    private static double tierify$computeVanillaLikeValue(double base, double add, double multBase, double multTotal) {
        double d0 = base + add;
        double d1 = d0 + (d0 * multBase);
        return d1 * multTotal;
    }

    @Unique
    private static VanillaSums tierify$sumModifiers(Collection<?> modifiers) {
        VanillaSums sums = new VanillaSums();
    
        for (Object o : modifiers) {
            if (o == null) continue;
    
            double amount = tierify$readModifierAmount(o);
            int op = tierify$readModifierOperationOrdinal(o);
    
            switch (op) {
                case 0 -> sums.add += amount;                 // ADDITION
                case 1 -> sums.multBase += amount;            // MULTIPLY_BASE
                case 2 -> sums.multTotal *= (1.0 + amount);   // MULTIPLY_TOTAL
                default -> { /* ignore unknown */ }
            }
        }
    
        return sums;
    }

    @Unique
    private static double tierify$readModifierAmount(Object mod) {
        // Works across mappings by trying common method names
        Double v =
                (Double) tierify$invokeFirst(mod, "getValue", "getAmount", "m_22218_");
        return v != null ? v : 0.0;
    }

    @Unique
    private static int tierify$readModifierOperationOrdinal(Object mod) {
        Object op = tierify$invokeFirst(mod, "getOperation", "m_22219_");
        if (op == null) return 0;

        // Yarn: EntityAttributeModifier.Operation is an enum; Mojmap: AttributeModifier.Operation is an enum.
        if (op instanceof Enum<?> e) {
            return e.ordinal();
        }
        return 0;
    }

    @Unique
    private static Object tierify$invokeFirst(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    @Unique
    private static void tierify$ensureIconsResolved() {
        if (TIERIFY$ICON_ARMOR != null) return;

        TIERIFY$ICON_ARMOR = tierify$resolveObscureIcon("ARMOR");
        TIERIFY$ICON_TOUGHNESS = tierify$resolveObscureIcon("ARMOR_TOUGHNESS");
        TIERIFY$ICON_KNOCKBACK = tierify$resolveObscureIcon("KNOCKBACK_RESISTANCE");
    }

    @Unique
    private static String tierify$resolveObscureIcon(String enumName) {
        try {
            Class<?> icons = Class.forName("com.obscuria.obscureapi.api.utils.Icons");
            Object e = Enum.valueOf((Class<? extends Enum>) icons.asSubclass(Enum.class), enumName);
            Method get = icons.getMethod("get");
            Object out = get.invoke(e);
            return (out instanceof String s) ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Computes the *per-piece* extra contribution caused by the set bonus for THIS stack,
     * for the attribute represented by the Obscure icon string.
     */
    @Unique
    private static SetBonusDelta tierify$computeSetBonusDelta(String icon, boolean isPercent) {
        // Fast rejects
        if (!Tierify.CONFIG.enableArmorSetBonuses) return null;

        ItemStack stack = TIERIFY$CURRENT_STACK.get();
        if (stack == null || stack.isEmpty()) return null;

        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return null;

        // Only apply when the set bonus is active for this piece
        if (!SetBonusUtils.hasSetBonus(player, stack)) return null;

        tierify$ensureIconsResolved();

        String attributeId = null;
        if (TIERIFY$ICON_ARMOR != null && TIERIFY$ICON_ARMOR.equals(icon)) {
            attributeId = "minecraft:generic.armor";
        } else if (TIERIFY$ICON_TOUGHNESS != null && TIERIFY$ICON_TOUGHNESS.equals(icon)) {
            attributeId = "minecraft:generic.armor_toughness";
        } else if (TIERIFY$ICON_KNOCKBACK != null && TIERIFY$ICON_KNOCKBACK.equals(icon)) {
            attributeId = "minecraft:generic.knockback_resistance";
        } else {
            return null; // Not one of the summary-line attributes we need to correct
        }

        // Determine bonus percent (0.20) used by the set
        double pct = SetBonusUtils.hasPerfectSetBonus(player, stack)
                ? Tierify.CONFIG.armorSetPerfectBonusPercent
                : Tierify.CONFIG.armorSetBonusMultiplier;

        if (pct <= 0.0) return null;

        Identifier tierId = ModifierUtils.getAttributeID(stack);
        if (tierId == null) return null;

        PotentialAttribute pa = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
        if (pa == null) return null;

        double add = 0.0;
        double multBase = 0.0;
        double multTotalFactor = 1.0;

        for (AttributeTemplate t : pa.getAttributes()) {
            if (!attributeId.equals(t.getAttributeTypeID())) continue;

            EntityAttributeModifier m = t.getEntityAttributeModifier();
            double baseValue = m.getValue();

            // Only boost positive stats (matches your set-bonus rules)
            if (!(baseValue > 0.0)) continue;

            double extra = baseValue * pct;

            switch (m.getOperation()) {
                case ADDITION -> add += extra;
                case MULTIPLY_BASE -> multBase += extra;
                case MULTIPLY_TOTAL -> multTotalFactor *= (1.0 + extra);
            }
        }

        if (Math.abs(add) < 1.0e-9 && Math.abs(multBase) < 1.0e-9 && Math.abs(multTotalFactor - 1.0) < 1.0e-9) {
            return null;
        }

        return new SetBonusDelta(add, multBase, multTotalFactor);
    }

    @Unique
    private static final class VanillaSums {
        double add;
        double multBase;
        double multTotal;
    
        VanillaSums() {
            this.add = 0.0;
            this.multBase = 0.0;
            this.multTotal = 1.0; // multiplicative identity
        }
    }

    @Unique
    private record SetBonusDelta(double add, double multBase, double multTotalFactor) {}
}
