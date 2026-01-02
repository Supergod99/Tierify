package elocindev.tierify.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.Registries;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.obscuria.obscureapi.client.TooltipBuilder$AttributeIcons", remap = false)
public class ObscureApiAttributeIconMathMixin {

    @Unique private static final UUID TIERIFY$SET_BONUS_ID =
        UUID.fromString("98765432-1234-1234-1234-987654321012"); // matches SetBonusLogic :contentReference[oaicite:2]{index=2}

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
        double[] sums = tierify$sumModifiers(modifiers);
        double add = sums[0];
        double multBase = sums[1];
        double multTotal = sums[2];

        // Apply set bonus delta ONLY for the three attributes Obscure shows on armor summary line
        double[] delta = tierify$computeSetBonusDelta(icon, isPercent);
        if (delta != null) {
            add += delta[0];
            multBase += delta[1];
            multTotal *= delta[2];
        }

        double value = tierify$computeVanillaLikeValue(base, add, multBase, multTotal);

        // If Obscure would have hidden it, keep original behavior
        if (Math.abs(value) < 1.0e-9) return "";

        // Rebuild the string in the same general shape Obscure uses: "<icon><number><optional %> "
        // (Obscure’s icon itself already contains styling codes)
        return tierify$render(icon, value, isPercent);
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

    /**
     * Returns {add, multBase, multTotal} where multTotal starts at 1.0 and compounds (1+amount).
     * Implemented as an array to avoid generating nested helper classes inside a mixin package.
     */
    @Unique
    private static double[] tierify$sumModifiers(Collection<?> modifiers) {
        double add = 0.0;
        double multBase = 0.0;
        double multTotal = 1.0;

        for (Object o : modifiers) {
            if (o == null) continue;

            double amount = tierify$readModifierAmount(o);
            int op = tierify$readModifierOperationOrdinal(o);

            switch (op) {
                case 0 -> add += amount;                 // ADDITION
                case 1 -> multBase += amount;            // MULTIPLY_BASE
                case 2 -> multTotal *= (1.0 + amount);   // MULTIPLY_TOTAL
                default -> { /* ignore unknown */ }
            }
        }

        return new double[] { add, multBase, multTotal };
    }

    @Unique
    private static double tierify$readModifierAmount(Object mod) {
        // Works across mappings by trying common method names
        Double v = (Double) tierify$invokeFirst(mod, "getValue", "getAmount", "m_22218_");
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
    @SuppressWarnings({"rawtypes", "unchecked"})
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
     *
     * Returns {addDelta, multBaseDelta, multTotalFactor} OR null.
     * Implemented as an array to avoid generating nested helper classes inside a mixin package.
     */
    @Unique
    private static double[] tierify$computeSetBonusDelta(String icon, boolean isPercent) {
        if (!Tierify.CONFIG.enableArmorSetBonuses) return null;
    
        ItemStack hovered = TIERIFY$CURRENT_STACK.get();
        if (hovered == null || hovered.isEmpty()) return null;
        if (!(hovered.getItem() instanceof ArmorItem armor)) return null;
    
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return null;
    
        // Tier id from NBT: "Tiered" -> "Tier" :contentReference[oaicite:3]{index=3}
        String hoveredTier = tierify$getTierId(hovered);
        if (hoveredTier.isEmpty()) return null;
    
        // Require that the equipped item in THIS slot is the same tier.
        // (Prevents showing set-bonus deltas for unrelated armor pieces.)
        ItemStack equippedSameSlot = player.getEquippedStack(armor.getSlotType());
        if (equippedSameSlot == null || equippedSameSlot.isEmpty()) return null;
        String equippedTier = tierify$getTierId(equippedSameSlot);
        if (!hoveredTier.equals(equippedTier)) return null;
    
        // Must be a full set of this tier (by tier id, not object identity)
        if (!tierify$hasFullTierSetEquipped(player, hoveredTier)) return null;
    
        tierify$ensureIconsResolved();
    
        // Determine which attribute this icon represents (robust match: strip formatting)
        String attributeId;
        if (tierify$iconMatch(icon, TIERIFY$ICON_ARMOR)) {
            attributeId = "minecraft:generic.armor";
        } else if (tierify$iconMatch(icon, TIERIFY$ICON_TOUGHNESS)) {
            attributeId = "minecraft:generic.armor_toughness";
        } else if (tierify$iconMatch(icon, TIERIFY$ICON_KNOCKBACK)) {
            attributeId = "minecraft:generic.knockback_resistance";
        } else {
            return null;
        }
    
        EntityAttribute attr = Registries.ATTRIBUTE.get(new Identifier(attributeId));
        if (attr == null) return null;
    
        EntityAttributeInstance inst = player.getAttributeInstance(attr);
        if (inst == null) return null;
    
        // This is the *actual* set bonus modifier applied by SetBonusLogic (server),
        // synced to the client when active. :contentReference[oaicite:4]{index=4}
        EntityAttributeModifier bonus = inst.getModifier(TIERIFY$SET_BONUS_ID);
        if (bonus == null) return null;
    
        double totalAmount = bonus.getValue();
        if (totalAmount <= 0.0) return null;
    
        // SetBonusLogic multiplies by 4.0 when applying to the player. :contentReference[oaicite:5]{index=5}
        // We want the per-piece contribution for a single armor tooltip icon.
        EntityAttributeModifier.Operation op = bonus.getOperation();
    
        double add = 0.0;
        double multBase = 0.0;
        double multTotalFactor = 1.0;
    
        switch (op) {
            case ADDITION -> {
                double perPiece = totalAmount / 4.0;
                add += perPiece;
            }
            case MULTIPLY_BASE -> {
                double perPiece = totalAmount / 4.0;
                multBase += perPiece;
            }
            case MULTIPLY_TOTAL -> {
                // Preserve exact total effect: (perPieceFactor)^4 == (1 + totalAmount)
                // perPieceFactor = (1 + totalAmount)^(1/4)
                double totalFactor = 1.0 + totalAmount;
                if (totalFactor <= 0.0) return null;
    
                double perPieceFactor = Math.pow(totalFactor, 0.25);
                multTotalFactor *= perPieceFactor;
            }
        }
    
        if (Math.abs(add) < 1.0e-9
                && Math.abs(multBase) < 1.0e-9
                && Math.abs(multTotalFactor - 1.0) < 1.0e-9) {
            return null;
        }
    
        return new double[]{add, multBase, multTotalFactor};
    }
    
    // Add these helpers (or equivalent) to make icon matching resilient:
    @Unique
    private static boolean tierify$iconMatch(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
    
        String sa = tierify$stripFormatting(a);
        String sb = tierify$stripFormatting(b);
    
        return sa.equals(sb) || sa.contains(sb) || sb.contains(sa);
    }
    
    @Unique
    private static String tierify$stripFormatting(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i++; // skip format code
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
    
    @Unique
    private static String tierify$getTierId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        NbtCompound nbt = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (nbt == null) return "";
        return nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY);
    }
    
    @Unique
    private static boolean tierify$hasFullTierSetEquipped(PlayerEntity player, String targetTier) {
        if (player == null || targetTier == null || targetTier.isEmpty()) return false;
    
        int matchCount = 0;
        for (ItemStack armorPiece : player.getInventory().armor) {
            if (armorPiece == null || armorPiece.isEmpty()) return false;
    
            String pieceTier = tierify$getTierId(armorPiece);
            if (targetTier.equals(pieceTier)) {
                matchCount++;
            }
        }
        return matchCount >= 4;
    }
    
    @Unique
    private static boolean tierify$hasPerfectFullTierSetEquipped(PlayerEntity player, String targetTier) {
        if (!tierify$hasFullTierSetEquipped(player, targetTier)) return false;
    
        for (ItemStack armorPiece : player.getInventory().armor) {
            if (armorPiece == null || armorPiece.isEmpty()) return false;
    
            NbtCompound nbt = armorPiece.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (nbt == null) return false;
    
            if (!targetTier.equals(nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY))) return false;
    
            // Your project uses "Perfect" boolean in the Tier subtag. :contentReference[oaicite:3]{index=3}
            if (!nbt.getBoolean("Perfect")) return false;
        }

        return true;
    }
}
