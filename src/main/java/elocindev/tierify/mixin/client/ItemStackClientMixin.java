package elocindev.tierify.mixin.client;

import com.google.common.collect.LinkedListMultimap;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.DecimalFormat;
import java.util.*;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackClientMixin {

    @Shadow public abstract NbtCompound getOrCreateSubNbt(String key);
    @Shadow public abstract boolean hasNbt();
    @Shadow public abstract NbtCompound getSubNbt(String key);
    @Shadow public abstract Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot);
    @Shadow @Final @Mutable public static DecimalFormat MODIFIER_FORMAT;

    private boolean isTiered = false;
    private String translationKey;
    private String armorModifierFormat;
    private Map<String, ArrayList> map = new HashMap<>();
    private boolean toughnessZero = false;

    @Inject(method = "getTooltip", at = @At("HEAD"))
    private void clearCache(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List> info) {
        this.map.clear();
        this.isTiered = this.hasNbt() && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null;
    }

    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 6))
    private void storeTooltipInformation(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List> info) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            Multimap<EntityAttribute, EntityAttributeModifier> modifiers = this.getAttributeModifiers(slot);
            for (Map.Entry<EntityAttribute, EntityAttributeModifier> entry : modifiers.entries()) {
                if (entry.getValue().getName().contains("tiered:") && !map.containsKey(entry.getKey().getTranslationKey())) {
                    
                    double value = entry.getValue().getValue();
                    boolean isSetBonus = false;
                    PlayerEntity clientPlayer = MinecraftClient.getInstance().player;

                    if (value > 0 && clientPlayer != null && this.hasNbt()) {
                        NbtCompound nbt = this.getSubNbt(Tierify.NBT_SUBTAG_KEY);
                        if (nbt != null) {
                            String myTier = nbt.getString(Tierify.NBT_SUBTAG_DATA_KEY);
                            if (!myTier.isEmpty()) {
                                int matchCount = 0;
                                for (ItemStack armor : clientPlayer.getInventory().armor) {
                                    NbtCompound aNbt = armor.getSubNbt(Tierify.NBT_SUBTAG_KEY);
                                    if (aNbt != null && aNbt.getString(Tierify.NBT_SUBTAG_DATA_KEY).equals(myTier)) matchCount++;
                                }
                                if (matchCount >= 4) {
                                    value *= 1.25D;
                                    isSetBonus = true;
                                }
                            }
                        }
                    }

                    String format = MODIFIER_FORMAT.format(
                            entry.getValue().getOperation() == EntityAttributeModifier.Operation.MULTIPLY_BASE || entry.getValue().getOperation() == EntityAttributeModifier.Operation.MULTIPLY_TOTAL
                                    ? value * 100.0
                                    : (entry.getKey().equals(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) ? value * 10.0 : value));

                    ArrayList collect = new ArrayList<>();
                    collect.add(entry.getValue().getOperation().getId());
                    collect.add(format);
                    collect.add(value > 0.0D);
                    collect.add(isSetBonus);
                    map.put(entry.getKey().getTranslationKey(), collect);
                }
            }
        }
    }

    @Redirect(method = "getTooltip", at = @At(value = "INVOKE", target = "Ljava/util/Map$Entry;getKey()Ljava/lang/Object;", ordinal = 0))
    private Object captureAttributeKey(Map.Entry entry) {
        Object key = entry.getKey();
        if (key instanceof EntityAttribute attr) {
            this.translationKey = attr.getTranslationKey();
        }
        return key;
    }

    @Redirect(method = "getTooltip", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 8))
    private boolean modifyTooltipPlus(List<Text> list, Object text) {
        String translationKey = this.translationKey;
        if (this.map != null && !this.map.isEmpty() && this.map.containsKey(translationKey)) {
            ArrayList collected = map.get(translationKey);
            boolean isSetBonus = collected.size() > 3 && (boolean) collected.get(3);
            Formatting format = isSetBonus ? Formatting.GOLD : Formatting.BLUE;

            list.add(Text.translatable("tiered.attribute.modifier.plus." + (int) collected.get(0), 
                    (isSetBonus ? "§6§l" : "§9") + "+" + this.armorModifierFormat,
                    ((boolean) collected.get(2) ? (isSetBonus ? "§6§l" : "§9") + "(+" : "§c(") + (String) collected.get(1) + ((int) collected.get(0) > 0 ? "%)" : ")"),
                    Text.translatable(translationKey).formatted(format)));
        } else {
            list.add((Text) text);
        }
        return true;
    }

    @Redirect(method = "getTooltip", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 9))
    private boolean modifyTooltipTake(List<Text> list, Object text) {
        if (this.map != null && !this.map.isEmpty() && this.map.containsKey(this.translationKey)) {
            
             list.add((Text) text);
        } else {
            list.add((Text) text);
        }
        return true;
    }

    @Redirect(method = "getTooltip", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 7))
    private boolean modifyTooltipEquals(List<Text> list, Object text) {
        if (this.map != null && !this.map.isEmpty() && this.map.containsKey(this.translationKey)) {
            ArrayList collected = map.get(translationKey);
            boolean isSetBonus = collected.size() > 3 && (boolean) collected.get(3);
            Formatting format = isSetBonus ? Formatting.GOLD : Formatting.DARK_GREEN;

            list.add(Text.translatable("tiered.attribute.modifier.equals." + (int) collected.get(0), 
                    (isSetBonus ? "§6§l" : "§2") + " " + this.armorModifierFormat,
                    ((boolean) collected.get(2) ? (isSetBonus ? "§6§l" : "§2") + "(+" : "§c(") + (String) collected.get(1) + ((int) collected.get(0) > 0 ? "%)" : ")"),
                    Text.translatable(translationKey).formatted(format)));
        } else {
            list.add((Text) text);
        }
        return true;
    }

    @Redirect(method = "getTooltip",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/text/MutableText;formatted(Lnet/minecraft/util/Formatting;)Lnet/minecraft/text/MutableText;",
        ordinal = 2))
    private MutableText getFormatting(MutableText text, Formatting formatting) {
        String raw = text.getString();
        if (raw.contains("§x")) {
            return text;
        }
        if (this.hasNbt() && this.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null) {
            Identifier tier = new Identifier(this.getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            PotentialAttribute attribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);
            if (attribute != null) {
                return text.setStyle(attribute.getStyle());
            }
        }
        return text.formatted(formatting);
    }

    @ModifyVariable(method = "getTooltip", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Multimap;isEmpty()Z", remap = false), index = 10)
    private Multimap<EntityAttribute, EntityAttributeModifier> sort(Multimap<EntityAttribute, EntityAttributeModifier> map) {
        Multimap<EntityAttribute, EntityAttributeModifier> vanillaFirst = LinkedListMultimap.create();
        Multimap<EntityAttribute, EntityAttributeModifier> remaining = LinkedListMultimap.create();

        map.forEach((entityAttribute, entityAttributeModifier) -> {
            if (!entityAttributeModifier.getName().contains("tiered")) {
                vanillaFirst.put(entityAttribute, entityAttributeModifier);
            } else {
                remaining.put(entityAttribute, entityAttributeModifier);
            }
        });

        vanillaFirst.putAll(remaining);
        return vanillaFirst;
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

    @Inject(method = "getTooltip", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/item/ItemStack;getAttributeModifiers(Lnet/minecraft/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;"))
    private void getTooltipMixin(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> info) {
        if (this.hasNbt()) {
            NbtCompound tierTag = this.getSubNbt(Tierify.NBT_SUBTAG_KEY);
            if (tierTag != null && tierTag.getBoolean("Perfect")) {
                tierTag.putString("BorderTier", "tiered:perfect");
            }
        }
    }

    @ModifyExpressionValue(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;", ordinal = 1))
    private MutableText modifyTooltipEquipmentSlot(MutableText original) {
        if (this.isTiered && this.getAttributeModifiers(EquipmentSlot.MAINHAND) != null && !this.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty()
                && this.getAttributeModifiers(EquipmentSlot.OFFHAND) != null && !this.getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty()) {
            return Text.translatable("item.modifiers.hand").formatted(Formatting.GRAY);
        }
        return original;
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void tierify$fixInvertedAttackSpeedText(PlayerEntity player, TooltipContext context, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> tooltip = cir.getReturnValue();
        if (tooltip == null || tooltip.isEmpty()) return;
    
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
