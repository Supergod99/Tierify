package draylar.tiered.api;

import net.levelz.access.PlayerStatsManagerAccess;
import net.levelz.stats.Skill;
import net.libz.util.SortList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import elocindev.tierify.Tierify;
import elocindev.tierify.compat.ItemBordersCompat;

public class ModifierUtils {

    /**
     * Returns the ID of a random attribute that is valid for the given {@link Item} in {@link Identifier} form.
     * <p>
     * If there is no valid attribute for the given {@link Item}, null is returned.
     *
     * @param item      {@link Item} to generate a random attribute for
     * @return          id of random attribute for item in {@link Identifier} form, or null if there are no valid options
     */
    @Nullable
    public static Identifier getRandomAttributeIDFor(@Nullable PlayerEntity playerEntity, Item item, boolean reforge) {
        List<Identifier> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();
        // collect all valid attributes for the given item and their weights

        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(Registries.ITEM.getId(item)) && (attribute.getWeight() > 0 || reforge)) {
                potentialAttributes.add(new Identifier(attribute.getID()));
                attributeWeights.add(reforge ? attribute.getWeight() + 1 : attribute.getWeight());
            }
        });
        if (potentialAttributes.size() <= 0) {
            return null;
        }

        if (reforge && attributeWeights.size() > 2) {
            SortList.concurrentSort(attributeWeights, attributeWeights, potentialAttributes);
            int maxWeight = attributeWeights.get(attributeWeights.size() - 1);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > maxWeight / 2) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * Tierify.CONFIG.reforgeModifier));
                }
            }
        }
        // LevelZ
        if (Tierify.isLevelZLoaded && playerEntity != null) {
            int newMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > newMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i)
                            * (1.0f - Tierify.CONFIG.levelzReforgeModifier * ((PlayerStatsManagerAccess) playerEntity).getPlayerStatsManager().getSkillLevel(Skill.SMITHING))));
                }
            }
        }
        // Luck
        if (playerEntity != null) {
            int luckMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > luckMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * (1.0f - Tierify.CONFIG.luckReforgeModifier * playerEntity.getLuck())));
                }
            }
        }

        if (potentialAttributes.size() > 0) {
            int totalWeight = 0;
            for (Integer weight : attributeWeights) {
                totalWeight += weight.intValue();
            }
            int randomChoice = new Random().nextInt(totalWeight);
            SortList.concurrentSort(attributeWeights, attributeWeights, potentialAttributes);

            for (int i = 0; i < attributeWeights.size(); i++) {
                if (randomChoice < attributeWeights.get(i)) {
                    return potentialAttributes.get(i);
                }
                randomChoice -= attributeWeights.get(i);
            }
            // If random choice didn't work
            return potentialAttributes.get(new Random().nextInt(potentialAttributes.size()));
        } else
            return null;
    }

    /**
     * Returns a list of all attribute IDs that contain the specified quality in their identifier.
     *
     * @param quality       The quality substring to look for in the attribute identifiers (e.g., "mythic").
     * @return              List of attribute IDs that contain the specified quality substring.
     */
    public static List<Identifier> getAttributeIDsForQuality(String quality, Item item) {
        List<Identifier> matchingAttributes = new ArrayList<>();
        
        // iterate over all attributes and add matching ones to the list
        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(Registries.ITEM.getId(item)) && id.toString().contains(quality.toLowerCase())) {
                matchingAttributes.add(id);
            }
        });
        
        return matchingAttributes;
    }

/**
     * Returns a random attribute ID from the attributes that contain any of the specified quality substrings in their identifier,
     * considering the weights of the attributes.
     *
     * @param qualities A list of quality substrings to look for in the attribute identifiers (e.g., "mythic", "legendary").
     * @param item      The item for which the attribute is being searched.
     * 
     * @return A random attribute ID that contains one of the specified quality substrings, considering attribute weights, or null if none are found.
     */
    public static Identifier getRandomAttributeForQuality(List<String> qualities, Item item, boolean reforge) {
        List<Identifier> matchingAttributes = new ArrayList<>();
        List<Integer> matchingAttributeWeights = new ArrayList<>();

        // Collect all matching attributes for the given qualities and their weights
        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(Registries.ITEM.getId(item)) && qualities.stream().anyMatch(quality -> id.toString().contains(quality.toLowerCase())) && (attribute.getWeight() > 0 || reforge)) {
                matchingAttributes.add(id);
                matchingAttributeWeights.add(reforge ? attribute.getWeight() + 1 : attribute.getWeight());
            }
        });

        // Return null if no matching attributes are found
        if (matchingAttributes.isEmpty()) {
            return null;
        }

        // Calculate the total weight
        int totalWeight = matchingAttributeWeights.stream().mapToInt(Integer::intValue).sum();
        int randomIndex = new Random().nextInt(totalWeight);
        
        // Choose a random attribute based on weight
        for (int i = 0; i < matchingAttributes.size(); i++) {
            randomIndex -= matchingAttributeWeights.get(i);
            if (randomIndex < 0) {
                return matchingAttributes.get(i);
            }
        }

        // Fallback, should not be reached due to the weight calculation
        return null;
    }

    public static void setItemStackAttributeEntityWeighted(@Nullable PlayerEntity playerEntity, ItemStack stack) {
        setItemStackAttributeEntityWeighted(playerEntity, stack, null);
    }
    
    public static void setItemStackAttributeEntityWeighted(@Nullable PlayerEntity playerEntity, ItemStack stack, @Nullable RegistryKey<World> dimensionKey) {
        if (stack == null || stack.isEmpty()) return;
    
        // Don't overwrite an already-tiered item
        NbtCompound tierTag = stack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag != null && tierTag.contains(Tierify.NBT_SUBTAG_DATA_KEY)) return;
    
        int w1, w2, w3, w4, w5, w6;
    
        if (Tierify.CONFIG.useDimensionTierWeights && dimensionKey != null) {
            if (dimensionKey.equals(World.OVERWORLD)) {
                w1 = Math.max(0, Tierify.CONFIG.overworldTier1Weight);
                w2 = Math.max(0, Tierify.CONFIG.overworldTier2Weight);
                w3 = Math.max(0, Tierify.CONFIG.overworldTier3Weight);
                w4 = Math.max(0, Tierify.CONFIG.overworldTier4Weight);
                w5 = Math.max(0, Tierify.CONFIG.overworldTier5Weight);
                w6 = Math.max(0, Tierify.CONFIG.overworldTier6Weight);
            } else if (dimensionKey.equals(World.NETHER)) {
                w1 = Math.max(0, Tierify.CONFIG.netherTier1Weight);
                w2 = Math.max(0, Tierify.CONFIG.netherTier2Weight);
                w3 = Math.max(0, Tierify.CONFIG.netherTier3Weight);
                w4 = Math.max(0, Tierify.CONFIG.netherTier4Weight);
                w5 = Math.max(0, Tierify.CONFIG.netherTier5Weight);
                w6 = Math.max(0, Tierify.CONFIG.netherTier6Weight);
            } else if (dimensionKey.equals(World.END)) {
                w1 = Math.max(0, Tierify.CONFIG.endTier1Weight);
                w2 = Math.max(0, Tierify.CONFIG.endTier2Weight);
                w3 = Math.max(0, Tierify.CONFIG.endTier3Weight);
                w4 = Math.max(0, Tierify.CONFIG.endTier4Weight);
                w5 = Math.max(0, Tierify.CONFIG.endTier5Weight);
                w6 = Math.max(0, Tierify.CONFIG.endTier6Weight);
            } else {
                // Modded dimensions: fall back to the global weights unless you later decide otherwise
                w1 = Math.max(0, Tierify.CONFIG.entityTier1Weight);
                w2 = Math.max(0, Tierify.CONFIG.entityTier2Weight);
                w3 = Math.max(0, Tierify.CONFIG.entityTier3Weight);
                w4 = Math.max(0, Tierify.CONFIG.entityTier4Weight);
                w5 = Math.max(0, Tierify.CONFIG.entityTier5Weight);
                w6 = Math.max(0, Tierify.CONFIG.entityTier6Weight);
            }
        } else {
            // existing global behavior
            w1 = Math.max(0, Tierify.CONFIG.entityTier1Weight);
            w2 = Math.max(0, Tierify.CONFIG.entityTier2Weight);
            w3 = Math.max(0, Tierify.CONFIG.entityTier3Weight);
            w4 = Math.max(0, Tierify.CONFIG.entityTier4Weight);
            w5 = Math.max(0, Tierify.CONFIG.entityTier5Weight);
            w6 = Math.max(0, Tierify.CONFIG.entityTier6Weight);
        }
    
        int total = w1 + w2 + w3 + w4 + w5 + w6;
    
        // Important: in dimension mode, "all zero" can mean "disable drops entirely in this dimension"
        if (Tierify.CONFIG.useDimensionTierWeights && Tierify.CONFIG.dimensionTierWeightsZeroMeansNoModifier && total <= 0) {
            return;
        }
    
        Identifier chosen = null;
    
        if (total > 0) {
            for (int attempt = 0; attempt < 10 && chosen == null; attempt++) {
                List<String> qualities = pickEntityTierQualities(w1, w2, w3, w4, w5, w6, total);
                if (qualities == null || qualities.isEmpty()) break;
    
                chosen = getRandomAttributeForQuality(qualities, stack.getItem(), false);
            }
        }
    
        // Preserve your existing fallback semantics outside the "all-zero means none" rule above
        if (chosen == null) {
            chosen = getRandomAttributeIDFor(playerEntity, stack.getItem(), false);
        }
    
        if (chosen != null) {
            setItemStackAttribute(chosen, stack);
        }
    }

    @Nullable
    private static List<String> pickEntityTierQualities(int w1, int w2, int w3, int w4, int w5, int w6, int total) {
        int roll = new Random().nextInt(total);

        if (roll < w1) return Tierify.CONFIG.tier_1_qualities;
        roll -= w1;

        if (roll < w2) return Tierify.CONFIG.tier_2_qualities;
        roll -= w2;

        if (roll < w3) return Tierify.CONFIG.tier_3_qualities;
        roll -= w3;

        if (roll < w4) return Tierify.CONFIG.tier_4_qualities;
        roll -= w4;

        if (roll < w5) return Tierify.CONFIG.tier_5_qualities;
        roll -= w5;

        return Tierify.CONFIG.tier_6_qualities;
    }

    private static final String STORED_CUSTOM_NAME_KEY = "StoredCustomName";

    /**
     * If the stack has a vanilla custom name (display.Name), stash it into Tierify extra NBT
     * and remove display.Name so Tierify can prepend its modifier prefix again.
     * This lets players rename items to remove the prefix, but reforging re-applies it.
     */
    
    private static void stashCustomNameForReforge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
    
        NbtCompound display = stack.getSubNbt("display");
        if (display == null) return;
        // display.Name is a JSON string in NBT 
        if (!display.contains("Name", 8)) return;
    
        String nameJson = display.getString("Name");
        if (nameJson == null || nameJson.isEmpty()) return;
        // Store under namespaced extra tag
        NbtCompound extra = stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_EXTRA_KEY);
        extra.putString(STORED_CUSTOM_NAME_KEY, nameJson);
        // Remove vanilla custom name so our prefix logic can apply again
        display.remove("Name");
        // Do NOT remove the whole display tag (dyed items use it).
    }

    public static void setItemStackAttribute(Identifier potentialAttributeID, ItemStack stack) {
        if (potentialAttributeID != null) {

            stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY).putString(Tierify.NBT_SUBTAG_DATA_KEY, potentialAttributeID.toString());
            
            // Generate a random unique ID for this specific item instance -
            stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY).putUuid("TierUUID", UUID.randomUUID());
            stack.getOrCreateSubNbt("itemborders_colors").putString("top", ItemBordersCompat.getColorForIdentifier(potentialAttributeID));
            stack.getOrCreateSubNbt("itemborders_colors").putString("bottom", ItemBordersCompat.getColorForIdentifier(potentialAttributeID));

            HashMap<String, Object> nbtMap = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(new Identifier(potentialAttributeID.toString())).getNbtValues();
            // add durability nbt
            List<AttributeTemplate> attributeList = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(new Identifier(potentialAttributeID.toString())).getAttributes();
            for (int i = 0; i < attributeList.size(); i++) {
                if (attributeList.get(i).getAttributeTypeID().equals("tiered:generic.durable")) {
                    if (nbtMap == null) {
                        nbtMap = new HashMap<String, Object>();
                    }
                    nbtMap.put("durable", (double) Math.round(attributeList.get(i).getEntityAttributeModifier().getValue() * 100.0) / 100.0);
                    break;
                }
            }
            // add nbtMap (namespaced, do NOT touch root keys)
            if (nbtMap != null) {
                // Root exists because getOrCreateSubNbt below will create it as needed
                NbtCompound extra = stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_EXTRA_KEY);
                for (HashMap.Entry<String, Object> entry : nbtMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (key == null || value == null) continue;
                    if (value instanceof String s) {
                        extra.putString(key, s);
                    } else if (value instanceof Boolean b) {
                        extra.putBoolean(key, b);
                    } else if (value instanceof Double d) {
                        if (Math.abs(d) % 1.0 < 0.0001D) {
                            extra.putInt(key, (int) Math.round(d));
                        } else {
                            extra.putDouble(key, Math.round(d * 100.0) / 100.0);
                        }
                    } else if (value instanceof Integer i) {
                        extra.putInt(key, i);
                    } else if (value instanceof Float f) {
                        extra.putFloat(key, f);
                    } else if (value instanceof Long l) {
                        extra.putLong(key, l);
                    }
                }
            }
        }
    }


    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge, ItemStack reforgeMaterial) {
        if (reforge && reforgeMaterial != null) {
            List<String> qualities = null;
    
            if (reforgeMaterial.isIn(TieredItemTags.TIER_1_ITEM)) {
                qualities = Tierify.CONFIG.tier_1_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_2_ITEM)) {
                qualities = Tierify.CONFIG.tier_2_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_3_ITEM)) {
                qualities = Tierify.CONFIG.tier_3_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_4_ITEM)) {
                qualities = Tierify.CONFIG.tier_4_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_5_ITEM)) {
                qualities = Tierify.CONFIG.tier_5_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_6_ITEM)) {
                qualities = Tierify.CONFIG.tier_6_qualities;
            }
    
            if (qualities != null) {
                Identifier possibleAttribute = getRandomAttributeForQuality(qualities, stack.getItem(), reforge);
                if (possibleAttribute != null) {
    
                    // if the item was renamed, stash that name so we can still prefix after reforge
                    stashCustomNameForReforge(stack);
    
                    boolean isPerfect = new java.util.Random().nextDouble() < Tierify.CONFIG.perfectRollChance;
    
                    NbtCompound tierTag = stack.getOrCreateSubNbt(Tierify.NBT_SUBTAG_KEY);
                    if (isPerfect) tierTag.putBoolean("Perfect", true);
                    else tierTag.remove("Perfect");
    
                    setItemStackAttribute(possibleAttribute, stack);
                    return;
                }
            }
        }
    
        setItemStackAttribute(playerEntity, stack, reforge);
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge) {
        if (stack.getSubNbt(Tierify.NBT_SUBTAG_KEY) == null) {
            setItemStackAttribute(ModifierUtils.getRandomAttributeIDFor(playerEntity, stack.getItem(), reforge), stack);   
        }
    }

    public static void removeItemStackAttribute(ItemStack itemStack) {
        if (!itemStack.hasNbt()) return;
    
        NbtCompound tierTag = itemStack.getSubNbt(Tierify.NBT_SUBTAG_KEY);
        if (tierTag == null) return;
        // Restore stashed custom name (if present) back into vanilla display.Name,
        // but dont overwrite an actively renamed item
        NbtCompound extra = itemStack.getSubNbt(Tierify.NBT_SUBTAG_EXTRA_KEY);
        if (extra != null && extra.contains(STORED_CUSTOM_NAME_KEY, 8 /* STRING */)) {
            NbtCompound display = itemStack.getOrCreateSubNbt("display");
            if (!display.contains("Name", 8 /* STRING */)) {
                display.putString("Name", extra.getString(STORED_CUSTOM_NAME_KEY));
            }
        }
        // Capture legacy durable presence BEFORE removing tier tag
        NbtCompound root = itemStack.getNbt();
        boolean hadLegacyDurable = root != null && root.contains("durable");
        // Remove Tierify managed subtags only 
        itemStack.removeSubNbt(Tierify.NBT_SUBTAG_EXTRA_KEY);
        itemStack.removeSubNbt(Tierify.NBT_SUBTAG_KEY);
    
        if (hadLegacyDurable) {
            NbtCompound r = itemStack.getNbt();
            if (r != null) r.remove("durable");
        }
    }

    @Nullable
    public static Identifier getAttributeID(ItemStack itemStack) {
        if (itemStack.getSubNbt(Tierify.NBT_SUBTAG_KEY) != null) {
            return new Identifier(itemStack.getSubNbt(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
        }
        return null;
    }

}
