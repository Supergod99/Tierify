package elocindev.tierify.mixin.client;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import elocindev.tierify.screen.client.TierGradientAnimator;
import elocindev.tierify.util.SetBonusUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.DecimalFormat;
import java.util.*;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Shadow public abstract NbtCompound getOrCreateSubNbt(String key);
    @Shadow public abstract boolean hasNbt();
    @Shadow public abstract NbtCompound getSubNbt(String key);
    @Shadow public abstract Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot);
    
    @Shadow @Final public static DecimalFormat MODIFIER_FORMAT;

    private static final boolean SIGN_FIX_DEBUG = true;

    private double getSetBonusFactor() {
        if (!Tierify.CONFIG.enableArmorSetBonuses) return 1.0D;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return 1.0D;
    
        ItemStack self = (ItemStack) (Object) this;
    
        if (!SetBonusUtils.hasSetBonus(player, self)) {
            return 1.0D;
        }
    
        boolean perfect = SetBonusUtils.hasPerfectSetBonus(player, self);
        double pct = perfect
                ? Tierify.CONFIG.armorSetPerfectBonusPercent
                : Tierify.CONFIG.armorSetBonusMultiplier;
    
        if (pct < 0.0D) pct = 0.0D;
    
        return 1.0D + pct;
    }

    private EquipmentSlot inferDefaultSlotFromItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) {
            return armor.getSlotType();
        }
        // Good default for most attribute tooltips
        return EquipmentSlot.MAINHAND;
    }
    
    private EquipmentSlot resolveSlotForAttributeLine(
            Map<EquipmentSlot, Map<String, double[]>> sums,
            EquipmentSlot currentSlot,
            EquipmentSlot defaultSlot,
            String attrTranslationKey,
            int opIdx
    ) {
        if (currentSlot != null) {
            Map<String, double[]> per = sums.get(currentSlot);
            if (per != null) {
                double[] ops = per.get(attrTranslationKey);
                if (ops != null && Math.abs(ops[opIdx]) > 1.0e-9) {
                    return currentSlot;
                }
            }
        }
    
        EquipmentSlot onlyCandidate = null;
        int candidates = 0;
    
        for (Map.Entry<EquipmentSlot, Map<String, double[]>> e : sums.entrySet()) {
            Map<String, double[]> per = e.getValue();
            if (per == null) continue;
    
            double[] ops = per.get(attrTranslationKey);
            if (ops == null) continue;
    
            if (Math.abs(ops[opIdx]) > 1.0e-9) {
                onlyCandidate = e.getKey();
                candidates++;
                if (candidates > 1) break;
            }
        }
    
        if (candidates == 1) return onlyCandidate;
    
        if (defaultSlot != null) {
            Map<String, double[]> per = sums.get(defaultSlot);
            if (per != null) {
                double[] ops = per.get(attrTranslationKey);
                if (ops != null && Math.abs(ops[opIdx]) > 1.0e-9) {
                    return defaultSlot;
                }
            }
        }
    
        if (sums.containsKey(EquipmentSlot.MAINHAND)) return EquipmentSlot.MAINHAND;
    
        return null;
    }

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void getNameMixin(CallbackInfoReturnable<Text> info) {
        if (this.hasNbt() && this.getSubNbt("display") == null && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null) {
            Identifier tier = new Identifier(getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (potentialAttribute != null) {
                MutableText text = Text.translatable(potentialAttribute.getID() + ".label");
                String tierKey = TierGradientAnimator.getTierFromId(potentialAttribute.getID());
                text = TierGradientAnimator.animate(text, tierKey);
                MutableText vanilla = info.getReturnValue().copy();
                info.setReturnValue(text.append(" ").append(vanilla));
            }
        }
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void modifyTooltipFinal(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null || tooltip.isEmpty()) return;

        if (this.hasNbt()) {
            NbtCompound tierTag = this.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (tierTag != null && tierTag.getBoolean("Perfect")) {
                tierTag.putString("BorderTier", "tiered:perfect");
            }
        }

        if (this.hasNbt() && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null) {
            applyAttributeLogic(tooltip);
            fixAttributeModifierSignMismatches(tooltip);
            fixRedPlusLines(tooltip);
        }

        fixAttackSpeedText(tooltip);
    }

    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;", ordinal = 1))
    private MutableText modifyTooltipEquipmentSlot(MutableText original) {
        if (this.hasNbt() && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null 
                && this.getAttributeModifiers(EquipmentSlot.MAINHAND) != null && !this.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty()
                && this.getAttributeModifiers(EquipmentSlot.OFFHAND) != null && !this.getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty()) {
            return Text.translatable("item.modifiers.hand").formatted(Formatting.GRAY);
        }
        return original;
    }

    private void applyAttributeLogic(List<Text> tooltip) {
        double setBonusFactor = getSetBonusFactor();
        boolean hasSetBonus = setBonusFactor > 1.000001D;
    
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<EntityAttribute, EntityAttributeModifier> modifiers = this.getAttributeModifiers(slot);
            if (modifiers.isEmpty()) continue;
    
            Map<EntityAttribute, List<EntityAttributeModifier>> grouped = new HashMap<>();
            for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : modifiers.entries()) {
                grouped.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue());
            }
    
            for (Map.Entry<EntityAttribute, List<EntityAttributeModifier>> group : grouped.entrySet()) {
                EntityAttribute attribute = group.getKey();
                List<EntityAttributeModifier> mods = group.getValue();
    
                double totalBase = 0;
                double totalWithBonus = 0;
                boolean hasTiered = false;
    
                for (EntityAttributeModifier mod : mods) {
                    double value = mod.getValue();
                    boolean isTiered = mod.getName().contains("tiered:");
                    if (isTiered) hasTiered = true;
    
                    totalBase += value;
    
                    if (isTiered && hasSetBonus && value > 0) {
                        totalWithBonus += (value * setBonusFactor);
                    } else {
                        totalWithBonus += value;
                    }
                }

                if (hasTiered && Math.abs(totalWithBonus - totalBase) > 0.0001) {
                    boolean isMultiplier = mods.get(0).getOperation() != EntityAttributeModifier.Operation.ADDITION;
                    double displayBase = totalBase;
                    double displayBonus = totalWithBonus;

                    if (isMultiplier) {
                        displayBase *= 100.0;
                        displayBonus *= 100.0;
                    } else if (attribute.equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE)) {
                        displayBase *= 10.0;
                        displayBonus *= 10.0;
                    }

                    String oldString = MODIFIER_FORMAT.format(displayBase);
                    String newString = MODIFIER_FORMAT.format(displayBonus);

                    updateTooltipRecursive(tooltip, oldString, newString, true);
                }
            }
        }

        // Final Pass: Safely Apply RED to negatives
        for (int i = 0; i < tooltip.size(); i++) {
            Text line = tooltip.get(i);
            String content = line.getString();
            
            if (content.contains("-") && content.matches(".*-[0-9].*")) {
                if (line instanceof MutableText mutable) {
                    TextColor redColor = TextColor.fromFormatting(Formatting.RED);
                    if (!Objects.equals(mutable.getStyle().getColor(), redColor)) {
                        tooltip.set(i, mutable.setStyle(mutable.getStyle().withColor(Formatting.RED)));
                    }
                } else {
                    tooltip.set(i, line.copy().setStyle(line.getStyle().withColor(Formatting.RED)));
                }
            }
        }
    }

    private void fixAttributeModifierSignMismatches(List<Text> tooltip) {
        Map<EquipmentSlot, Map<String, double[]>> sums = new EnumMap<>(EquipmentSlot.class);
    
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<EntityAttribute, EntityAttributeModifier> modifiers = this.getAttributeModifiers(slot);
            if (modifiers == null || modifiers.isEmpty()) continue;
    
            Map<String, double[]> perAttr = new HashMap<>();
            for (Map.Entry<EntityAttribute, EntityAttributeModifier> e : modifiers.entries()) {
                EntityAttribute attr = e.getKey();
                EntityAttributeModifier mod = e.getValue();
    
                int opIdx = switch (mod.getOperation()) {
                    case ADDITION -> 0;
                    case MULTIPLY_BASE -> 1;
                    case MULTIPLY_TOTAL -> 2;
                };
    
                String attrKey = attr.getTranslationKey();
                perAttr.computeIfAbsent(attrKey, k -> new double[3])[opIdx] += mod.getValue();
            }
    
            sums.put(slot, perAttr);
        }
    
        ItemStack self = (ItemStack) (Object) this;
        EquipmentSlot defaultSlot = inferDefaultSlotFromItem(self);
    
        EquipmentSlot currentSlot = null;
    
        for (int i = 0; i < tooltip.size(); i++) {
            Text line = tooltip.get(i);
            if (!(line.getContent() instanceof TranslatableTextContent tr)) continue;
    
            String key = tr.getKey();
    
            // Header detection (still useful when present, but not required anymore)
            if (key.startsWith("item.modifiers.")) {
                String suffix = key.substring("item.modifiers.".length());
                currentSlot = switch (suffix) {
                    case "mainhand" -> EquipmentSlot.MAINHAND;
                    case "offhand"  -> EquipmentSlot.OFFHAND;
                    case "head"     -> EquipmentSlot.HEAD;
                    case "chest"    -> EquipmentSlot.CHEST;
                    case "legs"     -> EquipmentSlot.LEGS;
                    case "feet"     -> EquipmentSlot.FEET;
    
                    // If your mixin rewrites to "item.modifiers.hand", treat as "unknown section"
                    // and let the resolver pick based on actual sums.
                    case "hand"     -> null;
    
                    default         -> null;
                };
                continue;
            }
    
            boolean isPlus = key.startsWith("attribute.modifier.plus.");
            boolean isTake = key.startsWith("attribute.modifier.take.");
            if (!isPlus && !isTake) continue;
    
            int opIdx;
            try {
                opIdx = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
            } catch (Exception ignored) {
                continue;
            }
    
            String attrTranslationKey = null;
            for (Object arg : tr.getArgs()) {
                if (arg instanceof Text t && t.getContent() instanceof TranslatableTextContent at) {
                    attrTranslationKey = at.getKey();
                    break;
                }
            }
            if (attrTranslationKey == null) continue;
    
            // NEW: resolve the slot even if headers are missing/altered by Tooltip Overhaul.
            EquipmentSlot resolvedSlot = resolveSlotForAttributeLine(
                    sums, currentSlot, defaultSlot, attrTranslationKey, opIdx
            );
            if (resolvedSlot == null) continue;
    
            Map<String, double[]> perAttr = sums.get(resolvedSlot);
            if (perAttr == null) continue;
    
            double[] ops = perAttr.get(attrTranslationKey);
            if (ops == null) continue;
    
            boolean shouldBeNegative = ops[opIdx] < -1.0e-9;
            if ((shouldBeNegative && isTake) || (!shouldBeNegative && isPlus)) continue;
    
            String newKey = (shouldBeNegative ? "attribute.modifier.take." : "attribute.modifier.plus.") + opIdx;
    
            Object[] args = tr.getArgs();
            Object[] newArgs = new Object[args.length];
            for (int j = 0; j < args.length; j++) {
                Object a = args[j];
                if (a instanceof String s) {
                    newArgs[j] = s.replaceFirst("^[+\\-]", "");
                } else if (a instanceof Text t) {
                    String ts = t.getString();
                    if (ts.startsWith("-") || ts.startsWith("+")) {
                        newArgs[j] = Text.literal(ts.replaceFirst("^[+\\-]", "")).setStyle(t.getStyle());
                    } else {
                        newArgs[j] = a;
                    }
                } else {
                    newArgs[j] = a;
                }
            }
    
            MutableText fixed = Text.translatable(newKey, newArgs).setStyle(line.getStyle());
            for (Text sibling : line.getSiblings()) fixed.append(sibling);
            tooltip.set(i, fixed);
        }
    }

    private boolean isRedLike(TextColor c) {
        if (c == null) return false;
    
        int rgb = c.getRgb();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
    
        // "Red-ish" heuristic: red channel dominates and G/B are relatively low.
        // This catches Formatting.RED, Formatting.DARK_RED, and custom dark reds used by other renderers.
        return r >= 0x80 && g <= 0x60 && b <= 0x60;
    }
    
    private boolean hasRedRecursive(Text t, TextColor red, TextColor darkRed) {
        if (t == null) return false;
    
        TextColor c = t.getStyle().getColor();
        if (c != null) {
            // Exact matches (vanilla Formatting.RED / DARK_RED)
            if (Objects.equals(c, red) || Objects.equals(c, darkRed)) return true;
    
            // Heuristic match (catches Tooltip Overhaul / custom dark reds)
            if (isRedLike(c)) return true;
        }
    
        if (t.getContent() instanceof TranslatableTextContent tr) {
            for (Object arg : tr.getArgs()) {
                if (arg instanceof Text at && hasRedRecursive(at, red, darkRed)) return true;
            }
        }
    
        for (Text sib : t.getSiblings()) {
            if (hasRedRecursive(sib, red, darkRed)) return true;
        }
    
        return false;
    }
    
    private Object[] stripLeadingSigns(Object[] args) {
        Object[] out = new Object[args.length];
        for (int j = 0; j < args.length; j++) {
            Object a = args[j];
            if (a instanceof String s) {
                out[j] = s.replaceFirst("^[+\\-]", "");
            } else if (a instanceof Text t) {
                String ts = t.getString();
                if (ts.startsWith("-") || ts.startsWith("+")) {
                    out[j] = Text.literal(ts.replaceFirst("^[+\\-]", "")).setStyle(t.getStyle());
                } else {
                    out[j] = a;
                }
            } else {
                out[j] = a;
            }
        }
        return out;
    }
    
    private void fixRedPlusLines(List<Text> tooltip) {
        TextColor red = TextColor.fromFormatting(Formatting.RED);
        TextColor darkRed = TextColor.fromFormatting(Formatting.DARK_RED);
    
        for (int i = 0; i < tooltip.size(); i++) {
            Text line = tooltip.get(i);
            if (line == null) continue;
    
            String trimmed = line.getString().trim();
    
            // Only touch attribute-like lines that begin with "+"
            boolean startsPlusDigit = trimmed.matches("^\\+\\s*\\d.*");
            if (!startsPlusDigit) continue;
    
            // Must be visibly red/dark-red somewhere in the component tree
            boolean redLike = hasRedRecursive(line, red, darkRed);
            if (!redLike) {
                if (SIGN_FIX_DEBUG) {
                    String k = (line.getContent() instanceof TranslatableTextContent tr2) ? tr2.getKey() : "<non-translatable>";
                    TextColor topColor = line.getStyle().getColor();
                    Tierify.LOGGER.info("[SignFix][fixRedPlusLines] Skipped (not redLike) idx={} key={} topColor={} text='{}'",
                            i, k, topColor, trimmed);
                }
                continue;
            }
    
            if (line.getContent() instanceof TranslatableTextContent tr) {
                String key = tr.getKey();
    
                if (key.startsWith("attribute.modifier.plus.")) {
                    String suffix = key.substring("attribute.modifier.plus.".length());
                    Object[] newArgs = stripLeadingSigns(tr.getArgs());
    
                    MutableText fixed = Text.translatable("attribute.modifier.take." + suffix, newArgs)
                            .setStyle(line.getStyle());
    
                    for (Text sibling : line.getSiblings()) fixed.append(sibling);
                    tooltip.set(i, fixed);
    
                    if (SIGN_FIX_DEBUG) {
                        Tierify.LOGGER.info("[SignFix][fixRedPlusLines] Applied vanilla translatable flip idx={} fromKey={} toKey={}",
                                i, key, "attribute.modifier.take." + suffix);
                    }
                    continue;
                }
    
                // apothic attributes modifier lines
                if ("attributeslib.modifier.plus".equals(key)) {
                    Object[] newArgs = stripLeadingSigns(tr.getArgs());
    
                    MutableText fixed = Text.translatable("attributeslib.modifier.take", newArgs)
                            .setStyle(line.getStyle());
    
                    for (Text sibling : line.getSiblings()) fixed.append(sibling);
                    tooltip.set(i, fixed);
    
                    if (SIGN_FIX_DEBUG) {
                        Tierify.LOGGER.info("[SignFix][fixRedPlusLines] Applied AttributesLib translatable flip idx={} fromKey={} toKey={}",
                                i, key, "attributeslib.modifier.take");
                    }
                    continue;
                }
    
                if (SIGN_FIX_DEBUG) {
                    Tierify.LOGGER.info("[SignFix][fixRedPlusLines] Candidate was translatable but unsupported idx={} key={} text='{}'",
                            i, key, trimmed);
                }
                continue;
            }

            if (line.getSiblings().isEmpty()) {
                String fixedString = line.getString().replaceFirst("^\\s*\\+", "-");
                tooltip.set(i, Text.literal(fixedString).setStyle(line.getStyle()));
    
                if (SIGN_FIX_DEBUG) {
                    Tierify.LOGGER.info("[SignFix][fixRedPlusLines] Applied literal fallback idx={} result='{}'",
                            i, fixedString.trim());
                }
            } else if (SIGN_FIX_DEBUG) {
                Tierify.LOGGER.info("[SignFix][fixRedPlusLines] Candidate skipped (non-translatable and has siblings) idx={}", i);
            }
        }
    }

    private void updateTooltipRecursive(List<Text> tooltip, String target, String replacement, boolean applyGold) {
        for (int i = 0; i < tooltip.size(); i++) {
            Text originalLine = tooltip.get(i);
            if (originalLine == null) continue;
    
            String plain = originalLine.getString();
            if (!plain.contains(target)) continue;
    
            // Only touch attribute modifier lines
            String trimmed = plain.trim();
            if (!(trimmed.startsWith("+") || trimmed.startsWith("-"))) {
                continue;
            }
    
            Text newLine = processNodeRecursive(originalLine, target, replacement);
    
            if (applyGold && newLine instanceof MutableText mutable) {
                forceColorRecursive(mutable, Formatting.GOLD);
            }
    
            tooltip.set(i, newLine);
        }
    }

    // Recursively forces a color on a text component and all its children
    private void forceColorRecursive(MutableText text, Formatting color) {
        text.setStyle(text.getStyle().withColor(color));
        for (Text sibling : text.getSiblings()) {
            if (sibling instanceof MutableText mutableSibling) {
                forceColorRecursive(mutableSibling, color);
            }
        }
    }

    private MutableText processNodeRecursive(Text node, String target, String replacement) {
        // Handle Translatable (Vanilla)
        if (node.getContent() instanceof TranslatableTextContent translatable) {
            Object[] args = translatable.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean changed = false;

            for (int j = 0; j < args.length; j++) {
                Object arg = args[j];
                if (arg instanceof String s && s.contains(target)) {
                    newArgs[j] = s.replace(target, replacement);
                    changed = true;
                } else if (arg instanceof Text t && t.getString().contains(target)) {
                    newArgs[j] = processNodeRecursive(t, target, replacement);
                    changed = true;
                } else {
                    newArgs[j] = arg;
                }
            }

            if (changed) {
                String key = translatable.getKey();
            
                boolean isAttrModifier =
                        key.startsWith("attribute.modifier.plus.") ||
                        key.startsWith("attribute.modifier.take.");
            
                boolean replacementNeg = replacement.startsWith("-");
                String replacementAbs = replacementNeg ? replacement.substring(1) : replacement;
            
                String newKey = key;
                if (isAttrModifier) {
                    if (replacementNeg && key.startsWith("attribute.modifier.plus.")) {
                        newKey = "attribute.modifier.take." + key.substring("attribute.modifier.plus.".length());
                    } else if (!replacementNeg && key.startsWith("attribute.modifier.take.")) {
                        newKey = "attribute.modifier.plus." + key.substring("attribute.modifier.take.".length());
                    }
            
                    for (int j = 0; j < newArgs.length; j++) {
                        Object a = newArgs[j];
                        if (a instanceof Text t) {
                            String ts = t.getString();
                            if (ts.contains(replacement)) {
                                newArgs[j] = Text.literal(ts.replace(replacement, replacementAbs)).setStyle(t.getStyle());
                            }
                        }
                    }
                }
            
                MutableText newTranslatable = Text.translatable(newKey, newArgs).setStyle(node.getStyle());
                for (Text sibling : node.getSiblings()) {
                    newTranslatable.append(processNodeRecursive(sibling, target, replacement));
                }
                return newTranslatable;
            }
        }

        // Handle Literal (Modded)
        MutableText newNode = node.copy();
        if (node.getString().contains(target) && (node.getContent() instanceof net.minecraft.text.LiteralTextContent)) {
            String oldText = node.getString();
        
            boolean replacementNeg = replacement.startsWith("-");
            String replacementAbs = replacementNeg ? replacement.substring(1) : replacement;
        
            String newText = oldText.replace(target, replacementAbs);
        
            // If the line is a "+ ..." line but the replacement is negative, flip leading '+' to '-'
            if (oldText.trim().startsWith("+") && replacementNeg) {
                newText = newText.replaceFirst("^\\s*\\+", "-");
            }
        
            // If the line is a "- ..." line but the replacement is positive, flip leading '-' to '+'
            if (oldText.trim().startsWith("-") && !replacementNeg) {
                newText = newText.replaceFirst("^\\s*-", "+");
            }
        
            newNode = Text.literal(newText).setStyle(node.getStyle());
        }
        
        // Handle Siblings
        newNode.getSiblings().clear();
        for (Text sibling : node.getSiblings()) {
            newNode.append(processNodeRecursive(sibling, target, replacement));
        }

        return newNode;
    }

    private boolean checkSetBonus() {
        PlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        if (clientPlayer == null || !this.hasNbt()) return false;

        NbtCompound nbt = this.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (nbt == null) return false;

        String myTier = nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY);
        if (myTier.isEmpty()) return false;

        int matchCount = 0;
        for (ItemStack armor : clientPlayer.getInventory().armor) {
            NbtCompound aNbt = armor.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (aNbt != null && aNbt.getString(Tierify.NBT_SUBTAG_DATA_KEY).equals(myTier)) matchCount++;
        }
        return matchCount >= 4;
    }

    private void fixAttackSpeedText(List<Text> tooltip) {
        double baseSpeed = 4.0;
        double addedValue = 0.0;
        double multiplyBase = 0.0;
        double multiplyTotal = 0.0;

        Multimap<EntityAttribute, EntityAttributeModifier> modifiers = this.getAttributeModifiers(EquipmentSlot.MAINHAND);

        if (modifiers.containsKey(EntityAttributes.GENERIC_ATTACK_SPEED)) {
            for (EntityAttributeModifier mod : modifiers.get(EntityAttributes.GENERIC_ATTACK_SPEED)) {
                if (mod.getOperation() == EntityAttributeModifier.Operation.ADDITION) {
                    addedValue += mod.getValue();
                } else if (mod.getOperation() == EntityAttributeModifier.Operation.MULTIPLY_BASE) {
                    multiplyBase += mod.getValue();
                } else if (mod.getOperation() == EntityAttributeModifier.Operation.MULTIPLY_TOTAL) {
                    multiplyTotal += mod.getValue();
                }
            }
        }

        double speed = (baseSpeed + addedValue) * (1.0 + multiplyBase) * (1.0 + multiplyTotal);

        String correctLabel;
        if (speed >= 3.0) correctLabel = "§2Very Fast";
        else if (speed >= 2.0) correctLabel = "§aFast";
        else if (speed >= 1.2) correctLabel = "§fMedium";
        else if (speed > 0.6) correctLabel = "§cSlow";
        else correctLabel = "§4Very Slow";

        for (int i = 0; i < tooltip.size(); i++) {
            Text line = tooltip.get(i);
            String text = line.getString();

            if (text.contains("Fast") || text.contains("Slow") || text.contains("Medium")) {
                tooltip.set(i, replaceSpeedTextRecursively(line, correctLabel));
                break;
            }
        }
    }

    private Text replaceSpeedTextRecursively(Text original, String replacementLabel) {
        MutableText newNode = processSingleNode(original, replacementLabel);
        for (Text sibling : original.getSiblings()) {
            newNode.append(replaceSpeedTextRecursively(sibling, replacementLabel));
        }
        return newNode;
    }

    private MutableText processSingleNode(Text node, String replacementLabel) {
        MutableText copy = MutableText.of(node.getContent()).setStyle(node.getStyle());
        String content = copy.getString();
        String[] targets = {"Very Fast", "Very Slow", "Fast", "Slow", "Medium"};

        for (String target : targets) {
            if (content.contains(target)) {
                String newContent = content.replace(target, replacementLabel);
                return Text.literal(newContent).setStyle(node.getStyle());
            }
        }
        return copy;
    }
}
