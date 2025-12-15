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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
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
                        if (a instanceof String s) {
                            if (s.contains(replacement)) {
                                newArgs[j] = s.replace(replacement, replacementAbs);
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
                newText = newText.replaceFirst("\\+", "-");
            }
        
            // If the line is a "- ..." line but the replacement is positive, flip leading '-' to '+'
            if (oldText.trim().startsWith("-") && !replacementNeg) {
                newText = newText.replaceFirst("-", "+");
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
