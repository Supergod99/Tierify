package elocindev.tierify.forge.mixin.compat;

import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(targets = "com.obscuria.obscureapi.client.TooltipBuilder$AttributeIcons", remap = false)
public class ObscureApiAttributeIconMathMixin {

    @Unique
    private static final UUID TIERIFY_SET_BONUS_ID =
            UUID.fromString("98765432-1234-1234-1234-987654321012");

    @Unique
    private static final ThreadLocal<ItemStack> CURRENT_STACK = new ThreadLocal<>();

    @Unique
    private static final DecimalFormat FORMAT = new DecimalFormat("##.#");

    @Unique
    private static String ICON_ARMOR;
    @Unique
    private static String ICON_TOUGHNESS;
    @Unique
    private static String ICON_KNOCKBACK;

    @Inject(method = "putIcons", at = @At("HEAD"))
    private static void tierify$captureStack(List<?> list, @Coerce Object stackObj, CallbackInfo ci) {
        if (stackObj instanceof ItemStack stack) {
            CURRENT_STACK.set(stack);
        } else {
            CURRENT_STACK.remove();
        }
    }

    @Inject(method = "putIcons", at = @At("RETURN"))
    private static void tierify$clearStack(List<?> list, @Coerce Object stackObj, CallbackInfo ci) {
        CURRENT_STACK.remove();
    }

    @Inject(method = "getIcon", at = @At("RETURN"), cancellable = true)
    private static void tierify$fixIconMathAndApplySetBonus(boolean isPercent,
                                                            String icon,
                                                            double base,
                                                            Collection<?> modifiers,
                                                            CallbackInfoReturnable<String> cir) {
        if (icon == null || modifiers == null || modifiers.isEmpty()) return;

        double[] sums = sumModifiers(modifiers);
        double add = sums[0];
        double multBase = sums[1];
        double multTotal = sums[2];

        double[] delta = computeSetBonusDelta(icon, modifiers);
        if (delta != null) {
            add += delta[0];
            multBase += delta[1];
            multTotal *= delta[2];
        }

        double value = computeVanillaLikeValue(base, add, multBase, multTotal);
        if (Math.abs(value) < 1.0e-9) {
            cir.setReturnValue("");
            return;
        }

        String green = (multBase > 0.0) ? "\u00A72" : "";
        cir.setReturnValue(render(icon, green, value, isPercent));
    }

    @Unique
    private static String render(String icon, String green, double value, boolean percent) {
        double shown = percent ? (value * 100.0) : value;
        String formatted = FORMAT.format(shown).replace(".0", "");
        return icon + green + formatted + (percent ? "% " : " ");
    }

    @Unique
    private static double computeVanillaLikeValue(double base, double add, double multBase, double multTotal) {
        double d0 = base + add;
        double d1 = d0 + (d0 * multBase);
        return d1 * multTotal;
    }

    @Unique
    private static double[] sumModifiers(Collection<?> modifiers) {
        double add = 0.0;
        double multBase = 0.0;
        double multTotal = 1.0;

        for (Object o : modifiers) {
            if (o == null) continue;

            double amount = readModifierAmount(o);
            int op = readModifierOperationOrdinal(o);

            switch (op) {
                case 0 -> add += amount;
                case 1 -> multBase += amount;
                case 2 -> multTotal *= (1.0 + amount);
                default -> {
                }
            }
        }

        return new double[] { add, multBase, multTotal };
    }

    @Unique
    private static double readModifierAmount(Object mod) {
        Double v = (Double) invokeFirst(mod, "getValue", "getAmount", "m_22218_");
        return v != null ? v : 0.0;
    }

    @Unique
    private static int readModifierOperationOrdinal(Object mod) {
        Object op = invokeFirst(mod, "getOperation", "m_22217_", "m_22219_");
        if (op instanceof Enum<?> e) return e.ordinal();
        return 0;
    }

    @Unique
    private static boolean isTieredModifier(Object mod) {
        Object name = invokeFirst(mod, "getName", "m_22220_");
        if (!(name instanceof String s)) return false;
        return s.contains("tiered:");
    }

    @Unique
    private static Object invokeFirst(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Unique
    private static void ensureIconsResolved() {
        if (ICON_ARMOR != null) return;
        ICON_ARMOR = resolveObscureIcon("ARMOR");
        ICON_TOUGHNESS = resolveObscureIcon("ARMOR_TOUGHNESS");
        ICON_KNOCKBACK = resolveObscureIcon("KNOCKBACK_RESISTANCE");
    }

    @Unique
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String resolveObscureIcon(String enumName) {
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

    @Unique
    private static double[] computeSetBonusDelta(String icon, Collection<?> modifiers) {
        if (!ForgeTierifyConfig.enableArmorSetBonuses()) return null;
        if (modifiers == null || modifiers.isEmpty()) return null;

        ItemStack hovered = CURRENT_STACK.get();
        if (hovered == null || hovered.isEmpty()) return null;
        if (!(hovered.getItem() instanceof ArmorItem armor)) return null;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return null;

        String hoveredTier = getTierId(hovered);
        if (hoveredTier.isEmpty()) return null;

        ItemStack equippedSameSlot = player.getItemBySlot(armor.getEquipmentSlot());
        if (equippedSameSlot == null || equippedSameSlot.isEmpty()) return null;
        if (hovered != equippedSameSlot) {
            UUID hoveredUuid = getTierUuid(hovered);
            UUID equippedUuid = getTierUuid(equippedSameSlot);
            if (hoveredUuid != null && equippedUuid != null) {
                if (!hoveredUuid.equals(equippedUuid)) return null;
            } else if (!ItemStack.isSameItemSameTags(hovered, equippedSameSlot)) {
                return null;
            }
        }
        if (!hoveredTier.equals(getTierId(equippedSameSlot))) return null;

        if (!hasFullTierSetEquipped(player, hoveredTier)) return null;

        double pct = hasPerfectTierSetEquipped(player, hoveredTier)
                ? ForgeTierifyConfig.armorSetPerfectBonusPercent()
                : ForgeTierifyConfig.armorSetBonusMultiplier();
        if (pct <= 0.0) return null;

        ensureIconsResolved();

        if (iconEquals(icon, ICON_ARMOR)) {
            // ok
        } else if (iconEquals(icon, ICON_TOUGHNESS)) {
            // ok
        } else if (iconEquals(icon, ICON_KNOCKBACK)) {
            // ok
        } else {
            return null;
        }

        double add = 0.0;
        double multBase = 0.0;
        double multTotalFactor = 1.0;

        for (Object mod : modifiers) {
            if (mod == null) continue;
            if (!isTieredModifier(mod)) continue;

            double amount = readModifierAmount(mod);
            if (amount <= 0.0) continue;

            int op = readModifierOperationOrdinal(mod);
            switch (op) {
                case 0 -> add += (amount * pct);
                case 1 -> multBase += (amount * pct);
                case 2 -> multTotalFactor *= (1.0 + (amount * pct));
                default -> {
                }
            }
        }

        if (Math.abs(add) < 1.0e-9
                && Math.abs(multBase) < 1.0e-9
                && Math.abs(multTotalFactor - 1.0) < 1.0e-9) {
            return null;
        }

        return new double[] { add, multBase, multTotalFactor };
    }

    @Unique
    private static boolean iconEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        return stripFormatting(a).trim().equals(stripFormatting(b).trim());
    }

    @Unique
    private static String stripFormatting(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00A7' && i + 1 < s.length()) {
                i++;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    @Unique
    private static String getTierId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CompoundTag nbt = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (nbt == null) return "";
        return nbt.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
    }

    @Unique
    private static UUID getTierUuid(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CompoundTag nbt = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (nbt == null || !nbt.hasUUID("TierUUID")) return null;
        return nbt.getUUID("TierUUID");
    }

    @Unique
    private static boolean hasFullTierSetEquipped(Player player, String targetTier) {
        if (player == null || targetTier == null || targetTier.isEmpty()) return false;

        int matchCount = 0;
        for (ItemStack armorPiece : player.getInventory().armor) {
            if (armorPiece == null || armorPiece.isEmpty()) return false;
            if (targetTier.equals(getTierId(armorPiece))) matchCount++;
        }
        return matchCount >= 4;
    }

    @Unique
    private static boolean hasPerfectTierSetEquipped(Player player, String targetTier) {
        if (!hasFullTierSetEquipped(player, targetTier)) return false;

        for (ItemStack armorPiece : player.getInventory().armor) {
            if (armorPiece == null || armorPiece.isEmpty()) return false;
            CompoundTag nbt = armorPiece.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (nbt == null) return false;
            if (!targetTier.equals(nbt.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY))) return false;
            if (!nbt.getBoolean("Perfect")) return false;
        }
        return true;
    }
}
