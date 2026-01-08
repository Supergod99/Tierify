package elocindev.tierify.forge.mixin.client;

import com.google.common.collect.Multimap;
import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.client.TierGradientAnimatorForge;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Forge client mixin that:
 *  - Prefixes hover-name with animated tier label (existing behavior)
 *  - Scales Tierify-generated positive attribute tooltip values by the active set-bonus factor (Fabric parity)
 *
 * IMPORTANT: Tooltip scaling adjusts only the Tierify-generated portion of the displayed value,
 * identified by TierifyConstants.MODIFIERS UUIDs (per-slot UUIDs). Vanilla/base modifiers are untouched.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    private static final DecimalFormat MODIFIER_FORMAT = new DecimalFormat("0.##");
    static {
        DecimalFormatSymbols s = DecimalFormatSymbols.getInstance(Locale.ROOT);
        s.setDecimalSeparator('.');
        MODIFIER_FORMAT.setDecimalFormatSymbols(s);
    }

    @Shadow public abstract Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot);

    /**
     * Existing: animated tier label prefix on the hover name.
     */
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void tierify$prefixModifierLabel(CallbackInfoReturnable<Component> cir) {
        ItemStack self = (ItemStack) (Object) this;

        // Match Fabric behavior: don’t override custom names
        if (self.hasCustomHoverName()) return;

        CompoundTag tiered = self.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tiered == null) return;

        String tierId = tiered.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (tierId == null || tierId.isEmpty()) return;

        // The lang keys are of the form "<tierId>.label" (e.g. "tiered:legendary_armor_1.label")
        MutableComponent label = Component.translatable(tierId + ".label");
        int tierIdx = TierGradientAnimatorForge.getTierFromId(tierId);
        MutableComponent animated = TierGradientAnimatorForge.animate(label, tierIdx);

        Component baseName = cir.getReturnValue();

        // Prefix: "<animated label> <vanilla name>"
        MutableComponent out = Component.empty()
                .append(animated)
                .append(" ")
                .append(baseName.copy());

        cir.setReturnValue(out);
    }

    /**
     * New: mutate tooltip lines so *positive tiered* attribute values reflect the active set-bonus factor.
     *
     * This is the Forge equivalent of Fabric's tooltip mutation pipeline:
     *   newTotal = rawTotal + tierAmount * (factor - 1)
     *
     * where tierAmount is the sum of Tierify UUID modifiers (per slot/attr/op) and rawTotal is
     * the vanilla tooltip's displayed sum for that line.
     */
    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void tierify$mutateTooltip(Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        if (Boolean.TRUE.equals(TIERIFY_BASELINE_PASS.get())) return;
        ItemStack self = (ItemStack)(Object)this;

        CompoundTag data = self.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (data == null) return;

        String tierId = data.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (tierId == null || tierId.isEmpty()) return;

        List<Component> tooltip = new ArrayList<>(cir.getReturnValue());

        // 1) Qualitative attack-speed label parity (from Fabric)
        fixAttackSpeedText(self, tooltip);

        // 2) Guard against “+ -X” translation key mismatches (defensive)
        fixAttributeModifierSignMismatches(tooltip);

        // 3) Apply set-bonus scaling (tier UUIDs only)
        double factor = getSetBonusFactor(player, self);
        if (factor != 1.0D) {
            applySetBonusScalingToAttributeLines(self, tooltip, factor, player, flag);
            stripDisplayedZeros(tooltip);
        }

        cir.setReturnValue(tooltip);
    }

    // ------------------------------------------------------------
    // Set bonus factor (client-side mirror of Fabric SetBonusUtils)
    // ------------------------------------------------------------
    private static double getSetBonusFactor(Player player, ItemStack hovered) {
        if (!ForgeTierifyConfig.enableArmorSetBonuses()) return 1.0D;
        if (player == null) player = Minecraft.getInstance().player;
        if (player == null) return 1.0D;

        // Fabric parity: scale whenever the hovered stack is part of an active set bonus.
        if (!hasSetBonus(player, hovered)) return 1.0D;

        boolean perfect = hasPerfectSetBonus(player, hovered);
        double pct = perfect
                ? ForgeTierifyConfig.armorSetPerfectBonusPercent()
                : ForgeTierifyConfig.armorSetBonusMultiplier();
        if (pct < 0.0D) pct = 0.0D;

        return 1.0D + pct;
    }

    private static boolean isEquippedArmorStack(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof ArmorItem armor)) return false;

        ItemStack equipped = player.getItemBySlot(armor.getEquipmentSlot());

        // Fast path: exact instance.
        if (equipped == stack) return true;

        // Forge sometimes builds tooltips from a stack copy; fall back to semantic equality.
        // Note: this can match identical copies outside the equipment slot, but is necessary for tooltip parity.
        return !equipped.isEmpty() && ItemStack.isSameItemSameTags(equipped, stack);
    }

    private static boolean hasSetBonus(Player player, ItemStack hovered) {
        if (player == null || hovered == null || hovered.isEmpty()) return false;
        if (!(hovered.getItem() instanceof ArmorItem)) return false;
        if (!isEquippedArmorStack(player, hovered)) return false;

        CompoundTag hoveredData = hovered.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (hoveredData == null) return false;

        String tierId = hoveredData.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (tierId == null || tierId.isEmpty()) return false;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) return false;

            CompoundTag d = armor.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (d == null) return false;

            if (!tierId.equals(d.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY))) return false;
        }
        return true;
    }

    private static boolean hasPerfectSetBonus(Player player, ItemStack hovered) {
        if (!hasSetBonus(player, hovered)) return false;

        CompoundTag hoveredData = hovered.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (hoveredData == null) return false;

        String tierId = hoveredData.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (tierId == null || tierId.isEmpty()) return false;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        }) {
            ItemStack armor = player.getItemBySlot(slot);
            CompoundTag d = armor.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (d == null) return false;
            if (!tierId.equals(d.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY))) return false;
            if (!d.getBoolean("Perfect")) return false;
        }
        return true;
    }

    // ------------------------------------------------------------
    // Attribute tooltip scaling (Tierify UUIDs only)
    // ------------------------------------------------------------

    private static void applySetBonusScalingToAttributeLines(ItemStack self, List<Component> tooltip, double factor, Player player, TooltipFlag flag) {
        if (tooltip == null || tooltip.isEmpty()) return;

        // Fabric parity: only do work if there is an active bonus
        boolean hasSetBonus = factor > 1.000001D;
        if (!hasSetBonus) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<Attribute, AttributeModifier> modifiers = self.getAttributeModifiers(slot);
            if (modifiers == null || modifiers.isEmpty()) continue;

            for (var group : modifiers.asMap().entrySet()) {
                Attribute attribute = group.getKey();
                var modsCollection = group.getValue();
                if (modsCollection == null || modsCollection.isEmpty()) continue;

                // Fabric parity assumes all modifiers for this attribute share an operation (true for tiered bonuses)
                AttributeModifier.Operation op = modsCollection.iterator().next().getOperation();
                boolean isMultiplier = op != AttributeModifier.Operation.ADDITION;

                double totalBase = 0.0D;
                double totalWithBonus = 0.0D;
                boolean hasTiered = false;

                for (AttributeModifier mod : modsCollection) {
                    double value = mod.getAmount();

                    // FABRIC PARITY: detect tiered portion by NAME, not UUID
                    boolean isTiered = mod.getName() != null && mod.getName().contains("tiered:");
                    if (isTiered) hasTiered = true;

                    totalBase += value;

                    // Only positive tiered mods get set bonus
                    if (isTiered && value > 0.0D) {
                        totalWithBonus += (value * factor);
                    } else {
                        totalWithBonus += value;
                    }
                }

                // Only adjust lines where tiered modifiers exist and the value actually changes
                if (!hasTiered) continue;
                if (Math.abs(totalWithBonus - totalBase) <= 0.0001D) continue;

                double displayBase = totalBase;
                double displayBonus = totalWithBonus;

                // Match vanilla/fabric display conventions
                if (isMultiplier) {
                    displayBase *= 100.0D;
                    displayBonus *= 100.0D;
                } else if (attribute == Attributes.KNOCKBACK_RESISTANCE) {
                    displayBase *= 10.0D;
                    displayBonus *= 10.0D;
                }

                String oldString = MODIFIER_FORMAT.format(displayBase);
                String newString = MODIFIER_FORMAT.format(displayBonus);

                // Replace the numeric token in the tooltip and apply gold to the mutated line
                updateTooltipRecursive(tooltip, oldString, newString, true);
            }
        }
    }

    private static void updateTooltipRecursive(List<Component> tooltip, String target, String replacement, boolean applyGold) {
        for (int i = 0; i < tooltip.size(); i++) {
            Component originalLine = tooltip.get(i);
            if (originalLine == null) continue;

            String plain = originalLine.getString();
            if (plain == null || !plain.contains(target)) continue;

            MutableComponent newLine = processNodeRecursive(originalLine, target, replacement);

            if (applyGold) {
                newLine = forceColorRecursive(newLine, ChatFormatting.GOLD);
            }

            tooltip.set(i, newLine);
        }
    }

    private static MutableComponent processNodeRecursive(Component node, String target, String replacement) {
        MutableComponent out;

        if (node.getContents() instanceof TranslatableContents tc) {
            Object[] args = tc.getArgs();
            Object[] newArgs = (args == null) ? new Object[0] : java.util.Arrays.copyOf(args, args.length);

            for (int j = 0; j < newArgs.length; j++) {
                Object arg = newArgs[j];

                if (arg instanceof String s && s.contains(target)) {
                    newArgs[j] = s.replace(target, replacement);
                } else if (arg instanceof Component c) {
                    MutableComponent newC = processNodeRecursive(c, target, replacement);
                    newArgs[j] = newC;
                }
            }

            out = Component.translatable(tc.getKey(), newArgs);
        } else if (node.getContents() instanceof net.minecraft.network.chat.contents.LiteralContents lc) {
            String s = lc.text();
            out = Component.literal(s.contains(target) ? s.replace(target, replacement) : s);
        } else {
            // Fallback: keep visible string (rare, but safe)
            String s = node.getString();
            out = Component.literal(s.contains(target) ? s.replace(target, replacement) : s);
        }

        out.setStyle(node.getStyle());
        for (Component sib : node.getSiblings()) {
            out.append(processNodeRecursive(sib, target, replacement));
        }
        return out;
    }

    private static MutableComponent forceColorRecursive(Component node, ChatFormatting color) {
        MutableComponent out;

        if (node.getContents() instanceof TranslatableContents tc) {
            Object[] args = tc.getArgs();
            Object[] newArgs = (args == null) ? new Object[0] : java.util.Arrays.copyOf(args, args.length);

            for (int j = 0; j < newArgs.length; j++) {
                Object arg = newArgs[j];
                if (arg instanceof Component c) {
                    newArgs[j] = forceColorRecursive(c, color);
                }
            }

            out = Component.translatable(tc.getKey(), newArgs);
        } else if (node.getContents() instanceof net.minecraft.network.chat.contents.LiteralContents lc) {
            out = Component.literal(lc.text());
        } else {
            out = Component.literal(node.getString());
        }

        out.setStyle(node.getStyle().withColor(color));
        for (Component sib : node.getSiblings()) {
            out.append(forceColorRecursive(sib, color));
        }
        return out;
    }
    

    // ------------------------------------------------------------
    // Tooltip parsing helpers
    // ------------------------------------------------------------

    private static TranslatableContents asTranslatable(Component c) {
        if (c == null) return null;

        // Direct translatable line
        if (c.getContents() instanceof TranslatableContents tc) return tc;

        // Some mods wrap translatable components inside a parent component (style wrappers, extra spaces, etc.).
        // Walk siblings (and one extra level) to find the underlying translatable payload.
        for (Component sib : c.getSiblings()) {
            if (sib == null) continue;
            if (sib.getContents() instanceof TranslatableContents tc2) return tc2;
            for (Component sib2 : sib.getSiblings()) {
                if (sib2 == null) continue;
                if (sib2.getContents() instanceof TranslatableContents tc3) return tc3;
            }
        }

        return null;
    }

    private static Integer opIndexFromAttrModifierKey(String key) {
        if (key == null) return null;
        if (!key.startsWith("attribute.modifier.plus.") && !key.startsWith("attribute.modifier.take.")) return null;
        int lastDot = key.lastIndexOf('.');
        if (lastDot < 0 || lastDot == key.length() - 1) return null;
        try {
            return Integer.parseInt(key.substring(lastDot + 1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }



    private static final ThreadLocal<Boolean> TIERIFY_BASELINE_PASS =
        ThreadLocal.withInitial(() -> Boolean.FALSE);


    private static Double parseDisplayedNumber(Object arg0) {
        if (arg0 == null) return null;
        String s = (arg0 instanceof Component c) ? c.getString() : arg0.toString();
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        // Normalize comma decimals to dot; strip percent symbols if present.
        s = s.replace("%", "").replace(",", ".");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    // ------------------------------------------------------------
    // Sanitizers + attack speed label parity
    // ------------------------------------------------------------

    private static void fixAttributeModifierSignMismatches(List<Component> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            TranslatableContents tc = asTranslatable(line);
            if (tc == null) continue;

            String key = tc.getKey();
            Integer opIdx = opIndexFromAttrModifierKey(key);
            if (opIdx == null) continue;

            Object[] args = tc.getArgs();
            if (args.length < 2) continue;

            String tokenStr = (args[0] instanceof Component c) ? c.getString() : String.valueOf(args[0]);
            if (tokenStr == null) continue;

            boolean tokenNeg = tokenStr.trim().startsWith("-");
            boolean isPlusKey = key.startsWith("attribute.modifier.plus.");
            boolean isTakeKey = key.startsWith("attribute.modifier.take.");

            if (tokenNeg && isPlusKey) {
                Double abs = parseDisplayedNumber(tokenStr.replace("-", ""));
                if (abs == null) continue;

                MutableComponent rebuilt = Component.translatable(
                        "attribute.modifier.take." + opIdx,
                        MODIFIER_FORMAT.format(Math.abs(abs)),
                        args[1]
                ).withStyle(ChatFormatting.RED);

                tooltip.set(i, rebuilt);
            }

            if (!tokenNeg && isTakeKey) {
                Double abs = parseDisplayedNumber(tokenStr);
                if (abs == null) continue;

                MutableComponent rebuilt = Component.translatable(
                        "attribute.modifier.plus." + opIdx,
                        MODIFIER_FORMAT.format(Math.abs(abs)),
                        args[1]
                ).withStyle(ChatFormatting.BLUE);

                tooltip.set(i, rebuilt);
            }
        }
    }

    private static void stripDisplayedZeros(List<Component> tooltip) {
        tooltip.removeIf(c -> {
            String s = c.getString();
            if (s == null) return false;
            return isDisplayedNumericZero(s.trim());
        });
    }

    private static boolean isDisplayedNumericZero(String trimmed) {
        var m = java.util.regex.Pattern
                .compile("^[+\\-]\\s*([0-9]+(?:[\\.,][0-9]+)?)%?.*")
                .matcher(trimmed);
        if (!m.matches()) return false;

        String num = m.group(1);
        String digitsOnly = num.replace(".", "").replace(",", "");
        return !digitsOnly.isEmpty() && digitsOnly.chars().allMatch(ch -> ch == '0');
    }

    /**
     * Fabric parity: show a qualitative attack-speed label based on the real computed attack speed.
     * Replaces the first line containing "Fast", "Slow", or "Medium".
     */
    private static void fixAttackSpeedText(ItemStack self, List<Component> tooltip) {
        Multimap<Attribute, AttributeModifier> mods = self.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (!mods.containsKey(Attributes.ATTACK_SPEED)) return;

        double baseSpeed = 4.0D;
        double added = 0.0D;
        double mulBase = 0.0D;
        double mulTotal = 0.0D;

        for (AttributeModifier m : mods.get(Attributes.ATTACK_SPEED)) {
            switch (m.getOperation()) {
                case ADDITION -> added += m.getAmount();
                case MULTIPLY_BASE -> mulBase += m.getAmount();
                case MULTIPLY_TOTAL -> mulTotal += m.getAmount();
            }
        }

        double speed = (baseSpeed + added) * (1.0D + mulBase) * (1.0D + mulTotal);

        String label;
        ChatFormatting color;
        if (speed >= 3.0D) { label = "Very Fast"; color = ChatFormatting.DARK_GREEN; }
        else if (speed >= 2.0D) { label = "Fast"; color = ChatFormatting.GREEN; }
        else if (speed >= 1.2D) { label = "Medium"; color = ChatFormatting.WHITE; }
        else if (speed > 0.6D) { label = "Slow"; color = ChatFormatting.RED; }
        else { label = "Very Slow"; color = ChatFormatting.DARK_RED; }

        for (int i = 0; i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString();
            if (text == null) continue;

            if (text.contains("Fast") || text.contains("Slow") || text.contains("Medium")) {
                tooltip.set(i, Component.literal(label).withStyle(color));
                break;
            }
        }
    }
}
