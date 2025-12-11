package elocindev.tierify.mixin.client;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import elocindev.tierify.screen.client.TierGradientAnimator;
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

    // 1. NAME MODIFICATION
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

    // 2. SAFE TOOLTIP MODIFICATION
    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void modifyTooltipFinal(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null || tooltip.isEmpty()) return;

        // -- BORDER LOGIC --
        if (this.hasNbt()) {
            NbtCompound tierTag = this.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (tierTag != null && tierTag.getBoolean("Perfect")) {
                tierTag.putString("BorderTier", "tiered:perfect");
            }
        }

        // -- ATTRIBUTE LOGIC (Colors & Values) --
        if (this.hasNbt() && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null) {
            applyAttributeLogic(tooltip);
        }

        // -- ATTACK SPEED FIX --
        fixAttackSpeedText(tooltip);
    }

    // 3. EQUIPMENT SLOT HEADER FIX
    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;", ordinal = 1))
    private MutableText modifyTooltipEquipmentSlot(MutableText original) {
        if (this.hasNbt() && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null 
                && this.getAttributeModifiers(EquipmentSlot.MAINHAND) != null && !this.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty()
                && this.getAttributeModifiers(EquipmentSlot.OFFHAND) != null && !this.getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty()) {
            return Text.translatable("item.modifiers.hand").formatted(Formatting.GRAY);
        }
        return original;
    }

    // --- HELPER METHODS ---

    private void applyAttributeLogic(List<Text> tooltip) {
        boolean hasSetBonus = checkSetBonus();

        // 1. Calculate Expected Values
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
                        totalWithBonus += (value * 1.25D); // Boost only the Tiered portion
                    } else {
                        totalWithBonus += value; // Base stats stay the same
                    }
                }

                // If buffed, update the tooltip
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
                    
                    // Recursive update to preserve styles (icons)
                    updateTooltipRecursive(tooltip, oldString, newString, true, false);
                }
            }
        }

        // 2. Final Pass: Force RED for negatives
        // Uses the same recursive logic to avoid breaking icons
        updateTooltipRecursive(tooltip, "-", "-", false, true);
    }

    /**
     * Walks through the Tooltip list and updates nodes safely.
     * @param tooltip The tooltip list
     * @param target The string to look for (e.g., "12") or "-"
     * @param replacement The string to replace it with (e.g., "15")
     * @param applyGold If true, applies Gold color to the replacement
     * @param forceRed If true, forces Red color if the node contains the target (used for negatives)
     */
    private void updateTooltipRecursive(List<Text> tooltip, String target, String replacement, boolean applyGold, boolean forceRed) {
        for (int i = 0; i < tooltip.size(); i++) {
            Text originalLine = tooltip.get(i);
            
            // Optimization: Skip lines that don't contain the target to avoid object creation
            if (!originalLine.getString().contains(target)) continue;
            
            // Process the line recursively
            Text newLine = processNodeRecursive(originalLine, target, replacement, applyGold, forceRed);
            tooltip.set(i, newLine);
        }
    }

    private MutableText processNodeRecursive(Text node, String target, String replacement, boolean applyGold, boolean forceRed) {
        // 1. Create a shallow copy of the current node (Preserves content + style)
        MutableText newNode = MutableText.of(node.getContent()).setStyle(node.getStyle());

        // 2. Modify the content if this specific node has text
        String content = newNode.getContent().toString(); // Use toString of content, not getString() which flattens children
        
        // LiteralTextContent returns the string, Translatable returns key, etc. 
        // We generally only want to replace literals that contain numbers.
        if (!content.isEmpty() && content.contains(target)) {
            // Check heuristic for negatives (Must look like a number: "-1" or "-.5")
            boolean isNegativeNumber = forceRed && content.matches(".*-.*[0-9].*");

            if (isNegativeNumber) {
                newNode.setStyle(newNode.getStyle().withColor(Formatting.RED));
            } 
            else if (!forceRed) {
                // Perform Value Replacement
                // Heuristic: Check if it's an attribute line (space or +)
                if (content.contains(" ") || content.contains("+") || content.matches(".*[0-9].*")) {
                     String newContentText = content.replace(target, replacement);
                     newNode = Text.literal(newContentText).setStyle(newNode.getStyle());
                     
                     if (applyGold) {
                         newNode.setStyle(newNode.getStyle().withColor(Formatting.GOLD));
                     }
                }
            }
        }

        // 3. Process Siblings Recursively
        for (Text sibling : node.getSiblings()) {
            newNode.append(processNodeRecursive(sibling, target, replacement, applyGold, forceRed));
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
