package elocindev.tierify.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ForgeTierifyConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_ARMOR_SET_BONUSES;
    public static final ForgeConfigSpec.DoubleValue ARMOR_SET_BONUS_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ARMOR_SET_PERFECT_BONUS_PERCENT;

    public static final ForgeConfigSpec.DoubleValue PERFECT_ROLL_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_REFORGING_DAMAGED;

    public static final ForgeConfigSpec.BooleanValue LOOT_CONTAINER_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue LOOT_CONTAINER_MODIFIER_CHANCE;
    public static final ForgeConfigSpec.BooleanValue TREASURE_BAG_DROP_MODIFIER;
    public static final ForgeConfigSpec.ConfigValue<String> TREASURE_BAG_PROFILES_FILE;
    public static final ForgeConfigSpec.BooleanValue ENTITY_ITEM_MODIFIER;
    public static final ForgeConfigSpec.BooleanValue ENTITY_LOOT_DROP_MODIFIER;
    public static final ForgeConfigSpec.ConfigValue<String> ENTITY_LOOT_DROP_PROFILES_FILE;
    public static final ForgeConfigSpec.BooleanValue ENTITY_EQUIPMENT_DROP_MODIFIER;

    public static final ForgeConfigSpec.IntValue ENTITY_TIER1_WEIGHT;
    public static final ForgeConfigSpec.IntValue ENTITY_TIER2_WEIGHT;
    public static final ForgeConfigSpec.IntValue ENTITY_TIER3_WEIGHT;
    public static final ForgeConfigSpec.IntValue ENTITY_TIER4_WEIGHT;
    public static final ForgeConfigSpec.IntValue ENTITY_TIER5_WEIGHT;
    public static final ForgeConfigSpec.IntValue ENTITY_TIER6_WEIGHT;

    public static final ForgeConfigSpec.BooleanValue USE_DIMENSION_TIER_WEIGHTS;
    public static final ForgeConfigSpec.BooleanValue DIMENSION_TIER_WEIGHTS_ZERO_MEANS_NO_MODIFIER;
    public static final ForgeConfigSpec.IntValue OVERWORLD_TIER1_WEIGHT;
    public static final ForgeConfigSpec.IntValue OVERWORLD_TIER2_WEIGHT;
    public static final ForgeConfigSpec.IntValue OVERWORLD_TIER3_WEIGHT;
    public static final ForgeConfigSpec.IntValue OVERWORLD_TIER4_WEIGHT;
    public static final ForgeConfigSpec.IntValue OVERWORLD_TIER5_WEIGHT;
    public static final ForgeConfigSpec.IntValue OVERWORLD_TIER6_WEIGHT;
    public static final ForgeConfigSpec.IntValue NETHER_TIER1_WEIGHT;
    public static final ForgeConfigSpec.IntValue NETHER_TIER2_WEIGHT;
    public static final ForgeConfigSpec.IntValue NETHER_TIER3_WEIGHT;
    public static final ForgeConfigSpec.IntValue NETHER_TIER4_WEIGHT;
    public static final ForgeConfigSpec.IntValue NETHER_TIER5_WEIGHT;
    public static final ForgeConfigSpec.IntValue NETHER_TIER6_WEIGHT;
    public static final ForgeConfigSpec.IntValue END_TIER1_WEIGHT;
    public static final ForgeConfigSpec.IntValue END_TIER2_WEIGHT;
    public static final ForgeConfigSpec.IntValue END_TIER3_WEIGHT;
    public static final ForgeConfigSpec.IntValue END_TIER4_WEIGHT;
    public static final ForgeConfigSpec.IntValue END_TIER5_WEIGHT;
    public static final ForgeConfigSpec.IntValue END_TIER6_WEIGHT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MODDED_DIMENSION_TIER_WEIGHT_OVERRIDES;
    public static final ForgeConfigSpec.BooleanValue CRAFTING_MODIFIER;
    public static final ForgeConfigSpec.BooleanValue MERCHANT_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue REFORGE_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue LEVELZ_REFORGE_MODIFIER;
    public static final ForgeConfigSpec.DoubleValue LUCK_REFORGE_MODIFIER;

    public static final ForgeConfigSpec.BooleanValue SHOW_REFORGING_TAB;
    public static final ForgeConfigSpec.IntValue X_ICON_POSITION;
    public static final ForgeConfigSpec.IntValue Y_ICON_POSITION;
    public static final ForgeConfigSpec.BooleanValue TIERED_TOOLTIP;
    public static final ForgeConfigSpec.BooleanValue SHOW_PLATES_ON_NAME;
    public static final ForgeConfigSpec.BooleanValue CENTER_NAME;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIER_1_QUALITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIER_2_QUALITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIER_3_QUALITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIER_4_QUALITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIER_5_QUALITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIER_6_QUALITIES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("tierify");

        ENABLE_ARMOR_SET_BONUSES = builder
                .comment("Enable/disable the entire armor set bonus system")
                .define("enableArmorSetBonuses", true);

        ARMOR_SET_BONUS_MULTIPLIER = builder
                .comment("Armor set bonus multiplier for a full 4-piece set. 0.01 = 1%, 1.0 = 100%")
                .defineInRange("armorSetBonusMultiplier", 0.2d, 0.0d, 10.0d);

        ARMOR_SET_PERFECT_BONUS_PERCENT = builder
                .comment("Armor set bonus percent when wearing 4 matching armor pieces AND all 4 are Perfect. 0.01 = 1%, 1.0 = 100%")
                .defineInRange("armorSetPerfectBonusPercent", 1.0d, 0.0d, 10.0d);

        PERFECT_ROLL_CHANCE = builder
                .comment("The chance for a reforge to be 'Perfect' (no negative attributes). 0.01 = 1%, 1.0 = 100%")
                .defineInRange("perfectRollChance", 0.02d, 0.0d, 1.0d);

        ALLOW_REFORGING_DAMAGED = builder
                .comment("If true, items can be reforged even if they are damaged.")
                .define("allowReforgingDamaged", true);

        LOOT_CONTAINER_MODIFIER = builder
                .comment("Items in for example mineshaft chests get modifiers")
                .define("lootContainerModifier", true);

        LOOT_CONTAINER_MODIFIER_CHANCE = builder
                .comment("Chance for loot-container items to receive a modifier when lootContainerModifier is true.")
                .comment("0.0 = never, 0.5 = 50%, 1.0 = always.")
                .defineInRange("lootContainerModifierChance", 0.1d, 0.0d, 1.0d);

        TREASURE_BAG_DROP_MODIFIER = builder
                .comment("Armageddon treasure bags gear can get modifiers")
                .define("treasureBagDropModifier", true);

        TREASURE_BAG_PROFILES_FILE = builder
                .comment("Treasure bag profiles file (in the config folder).")
                .define("treasureBagProfilesFile", "echelon-treasure-bag-profiles.txt");

        ENTITY_ITEM_MODIFIER = builder
                .comment("Equipped items on entities get modifiers")
                .define("entityItemModifier", false);

        ENTITY_LOOT_DROP_MODIFIER = builder
                .comment("If true, entity loot-table drops can receive modifiers based on a whitelist profile file.")
                .comment("This affects loot generated from an entity's loot table (e.g., bosses), not only worn equipment.")
                .define("entityLootDropModifier", false);

        ENTITY_LOOT_DROP_PROFILES_FILE = builder
                .comment("Whitelist profile file (in the config folder) for entity loot-table drop reforging.")
                .comment("Format per line: 'modid:entity=0.50|0,0,5,10,5,2' or 'modid:entity=0.50|overworld' (presets: overworld|nether|end|global).")
                .comment("You can also use '*=...' as a global default and 'modid:*=...' as a namespace wildcard.")
                .define("entityLootDropProfilesFile", "echelon-entity-drop-profiles.txt");

        ENTITY_EQUIPMENT_DROP_MODIFIER = builder
                .comment("If true, items dropped from entity equipment slots (dropEquipment path) can be reforged using the entity loot-drop profiles file.")
                .comment("Whitelist behavior: only entities with a matching profile entry (or wildcard entry) will affect equipment drops.")
                .define("entityEquipmentDropModifier", true);

        ENTITY_TIER1_WEIGHT = builder
                .comment("Tier weights for mob-equipped items/chests when entityItemModifier=true OR lootContainerModifier=true. Higher = more common. These are relative weights, not %.")
                .comment("Set all to 0 to disable weighting and fall back to the old fully-random behavior.")
                .defineInRange("entityTier1Weight", 2000, 0, Integer.MAX_VALUE);
        ENTITY_TIER2_WEIGHT = builder.defineInRange("entityTier2Weight", 200, 0, Integer.MAX_VALUE);
        ENTITY_TIER3_WEIGHT = builder.defineInRange("entityTier3Weight", 27, 0, Integer.MAX_VALUE);
        ENTITY_TIER4_WEIGHT = builder.defineInRange("entityTier4Weight", 9, 0, Integer.MAX_VALUE);
        ENTITY_TIER5_WEIGHT = builder.defineInRange("entityTier5Weight", 3, 0, Integer.MAX_VALUE);
        ENTITY_TIER6_WEIGHT = builder.defineInRange("entityTier6Weight", 1, 0, Integer.MAX_VALUE);

        USE_DIMENSION_TIER_WEIGHTS = builder
                .comment("If true, use dimension-specific tier weights for mob-equipped items and loot container rolls.")
                .define("useDimensionTierWeights", true);

        DIMENSION_TIER_WEIGHTS_ZERO_MEANS_NO_MODIFIER = builder
                .comment("When useDimensionTierWeights=true and the selected dimension profile has ALL weights set to 0, no modifier is applied (instead of falling back to random).")
                .define("dimensionTierWeightsZeroMeansNoModifier", true);

        OVERWORLD_TIER1_WEIGHT = builder
                .comment("Overworld tier weights (1=Common ... 6=Mythic). Set a tier weight to 0 to disable that tier in the Overworld.")
                .defineInRange("overworldTier1Weight", 100, 0, Integer.MAX_VALUE);
        OVERWORLD_TIER2_WEIGHT = builder.defineInRange("overworldTier2Weight", 10, 0, Integer.MAX_VALUE);
        OVERWORLD_TIER3_WEIGHT = builder.defineInRange("overworldTier3Weight", 1, 0, Integer.MAX_VALUE);
        OVERWORLD_TIER4_WEIGHT = builder.defineInRange("overworldTier4Weight", 0, 0, Integer.MAX_VALUE);
        OVERWORLD_TIER5_WEIGHT = builder.defineInRange("overworldTier5Weight", 0, 0, Integer.MAX_VALUE);
        OVERWORLD_TIER6_WEIGHT = builder.defineInRange("overworldTier6Weight", 0, 0, Integer.MAX_VALUE);

        NETHER_TIER1_WEIGHT = builder
                .comment("Nether tier weights (1=Common ... 6=Mythic).")
                .defineInRange("netherTier1Weight", 10, 0, Integer.MAX_VALUE);
        NETHER_TIER2_WEIGHT = builder.defineInRange("netherTier2Weight", 100, 0, Integer.MAX_VALUE);
        NETHER_TIER3_WEIGHT = builder.defineInRange("netherTier3Weight", 10, 0, Integer.MAX_VALUE);
        NETHER_TIER4_WEIGHT = builder.defineInRange("netherTier4Weight", 1, 0, Integer.MAX_VALUE);
        NETHER_TIER5_WEIGHT = builder.defineInRange("netherTier5Weight", 0, 0, Integer.MAX_VALUE);
        NETHER_TIER6_WEIGHT = builder.defineInRange("netherTier6Weight", 0, 0, Integer.MAX_VALUE);

        END_TIER1_WEIGHT = builder
                .comment("End tier weights (1=Common ... 6=Mythic).")
                .defineInRange("endTier1Weight", 10, 0, Integer.MAX_VALUE);
        END_TIER2_WEIGHT = builder.defineInRange("endTier2Weight", 100, 0, Integer.MAX_VALUE);
        END_TIER3_WEIGHT = builder.defineInRange("endTier3Weight", 1000, 0, Integer.MAX_VALUE);
        END_TIER4_WEIGHT = builder.defineInRange("endTier4Weight", 100, 0, Integer.MAX_VALUE);
        END_TIER5_WEIGHT = builder.defineInRange("endTier5Weight", 10, 0, Integer.MAX_VALUE);
        END_TIER6_WEIGHT = builder.defineInRange("endTier6Weight", 1, 0, Integer.MAX_VALUE);

        MODDED_DIMENSION_TIER_WEIGHT_OVERRIDES = builder
                .comment("Optional overrides for modded dimensions when useDimensionTierWeights=true.")
                .comment("Format: 'modid:dimension=500,125,20,6,3,1' (6 ints = Common..Mythic).")
                .comment("You can also use '*=overworld|nether|end|global' as a wildcard default for any modded dimension.")
                .comment("And you can use 'modid:*=...' to target all dimensions from a given namespace.")
                .defineList("moddedDimensionTierWeightOverrides", List.of(), ForgeTierifyConfig::isString);

        CRAFTING_MODIFIER = builder
                .comment("Crafted items get modifiers")
                .define("craftingModifier", false);

        MERCHANT_MODIFIER = builder
                .comment("Merchant items get modifiers")
                .define("merchantModifier", false);

        REFORGE_MODIFIER = builder
                .comment("Decreases the biggest weights by this modifier")
                .defineInRange("reforgeModifier", 0.0d, 0.0d, 10.0d);

        LEVELZ_REFORGE_MODIFIER = builder
                .comment("Modify the biggest weights by this modifier per smithing level")
                .defineInRange("levelzReforgeModifier", 0.0d, 0.0d, 10.0d);

        LUCK_REFORGE_MODIFIER = builder
                .comment("Modify the biggest weights by this modifier per luck")
                .defineInRange("luckReforgeModifier", 0.0d, 0.0d, 10.0d);

        builder.push("client_settings");

        SHOW_REFORGING_TAB = builder
                .comment("Whether or not to show the reforging tab in the anvil screen.")
                .define("showReforgingTab", true);

        X_ICON_POSITION = builder
                .defineInRange("xIconPosition", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

        Y_ICON_POSITION = builder
                .defineInRange("yIconPosition", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

        TIERED_TOOLTIP = builder
                .comment("Enable Tierify tooltip borders/labels.")
                .define("tieredTooltip", true);

        SHOW_PLATES_ON_NAME = builder
                .comment("Swaps the text with a plate displayed on the item's name.")
                .define("showPlatesOnName", false);

        CENTER_NAME = builder
                .comment("Centers the item name in the tooltip.")
                .define("centerName", true);

        builder.pop();

        TIER_1_QUALITIES = builder
                .comment("Tier 1 of Reforging (Limestone)")
                .defineList("tier1Qualities", List.of("Common"), ForgeTierifyConfig::isString);

        TIER_2_QUALITIES = builder
                .comment("Tier 2 of Reforging (Pyrite)")
                .defineList("tier2Qualities", List.of("Uncomon"), ForgeTierifyConfig::isString);

        TIER_3_QUALITIES = builder
                .comment("Tier 3 of Reforging (Galena)")
                .defineList("tier3Qualities", List.of("Rare"), ForgeTierifyConfig::isString);

        TIER_4_QUALITIES = builder
                .comment("Tier 4 of Reforging (Charoite)")
                .defineList("tier4Qualities", List.of("Epic"), ForgeTierifyConfig::isString);

        TIER_5_QUALITIES = builder
                .comment("Tier 5 of Reforging (Crown Topaz)")
                .defineList("tier5Qualities", List.of("Legendary"), ForgeTierifyConfig::isString);

        TIER_6_QUALITIES = builder
                .comment("Tier 6 of Reforging (Painite)")
                .defineList("tier6Qualities", List.of("Mythic"), ForgeTierifyConfig::isString);

        builder.pop();

        SPEC = builder.build();
    }

    private ForgeTierifyConfig() {}

    public static boolean enableArmorSetBonuses() {
        return ENABLE_ARMOR_SET_BONUSES.get();
    }

    public static float armorSetBonusMultiplier() {
        return ARMOR_SET_BONUS_MULTIPLIER.get().floatValue();
    }

    public static float armorSetPerfectBonusPercent() {
        return ARMOR_SET_PERFECT_BONUS_PERCENT.get().floatValue();
    }

    public static double perfectRollChance() {
        return PERFECT_ROLL_CHANCE.get();
    }

    public static boolean allowReforgingDamaged() {
        return ALLOW_REFORGING_DAMAGED.get();
    }

    public static boolean lootContainerModifier() {
        return LOOT_CONTAINER_MODIFIER.get();
    }

    public static float lootContainerModifierChance() {
        return LOOT_CONTAINER_MODIFIER_CHANCE.get().floatValue();
    }

    public static boolean treasureBagDropModifier() {
        return TREASURE_BAG_DROP_MODIFIER.get();
    }

    public static String treasureBagProfilesFile() {
        return TREASURE_BAG_PROFILES_FILE.get();
    }

    public static boolean entityItemModifier() {
        return ENTITY_ITEM_MODIFIER.get();
    }

    public static boolean entityLootDropModifier() {
        return ENTITY_LOOT_DROP_MODIFIER.get();
    }

    public static String entityLootDropProfilesFile() {
        return ENTITY_LOOT_DROP_PROFILES_FILE.get();
    }

    public static boolean entityEquipmentDropModifier() {
        return ENTITY_EQUIPMENT_DROP_MODIFIER.get();
    }

    public static int entityTier1Weight() {
        return ENTITY_TIER1_WEIGHT.get();
    }

    public static int entityTier2Weight() {
        return ENTITY_TIER2_WEIGHT.get();
    }

    public static int entityTier3Weight() {
        return ENTITY_TIER3_WEIGHT.get();
    }

    public static int entityTier4Weight() {
        return ENTITY_TIER4_WEIGHT.get();
    }

    public static int entityTier5Weight() {
        return ENTITY_TIER5_WEIGHT.get();
    }

    public static int entityTier6Weight() {
        return ENTITY_TIER6_WEIGHT.get();
    }

    public static boolean useDimensionTierWeights() {
        return USE_DIMENSION_TIER_WEIGHTS.get();
    }

    public static boolean dimensionTierWeightsZeroMeansNoModifier() {
        return DIMENSION_TIER_WEIGHTS_ZERO_MEANS_NO_MODIFIER.get();
    }

    public static int overworldTier1Weight() {
        return OVERWORLD_TIER1_WEIGHT.get();
    }

    public static int overworldTier2Weight() {
        return OVERWORLD_TIER2_WEIGHT.get();
    }

    public static int overworldTier3Weight() {
        return OVERWORLD_TIER3_WEIGHT.get();
    }

    public static int overworldTier4Weight() {
        return OVERWORLD_TIER4_WEIGHT.get();
    }

    public static int overworldTier5Weight() {
        return OVERWORLD_TIER5_WEIGHT.get();
    }

    public static int overworldTier6Weight() {
        return OVERWORLD_TIER6_WEIGHT.get();
    }

    public static int netherTier1Weight() {
        return NETHER_TIER1_WEIGHT.get();
    }

    public static int netherTier2Weight() {
        return NETHER_TIER2_WEIGHT.get();
    }

    public static int netherTier3Weight() {
        return NETHER_TIER3_WEIGHT.get();
    }

    public static int netherTier4Weight() {
        return NETHER_TIER4_WEIGHT.get();
    }

    public static int netherTier5Weight() {
        return NETHER_TIER5_WEIGHT.get();
    }

    public static int netherTier6Weight() {
        return NETHER_TIER6_WEIGHT.get();
    }

    public static int endTier1Weight() {
        return END_TIER1_WEIGHT.get();
    }

    public static int endTier2Weight() {
        return END_TIER2_WEIGHT.get();
    }

    public static int endTier3Weight() {
        return END_TIER3_WEIGHT.get();
    }

    public static int endTier4Weight() {
        return END_TIER4_WEIGHT.get();
    }

    public static int endTier5Weight() {
        return END_TIER5_WEIGHT.get();
    }

    public static int endTier6Weight() {
        return END_TIER6_WEIGHT.get();
    }

    public static List<String> moddedDimensionTierWeightOverrides() {
        List<? extends String> list = MODDED_DIMENSION_TIER_WEIGHT_OVERRIDES.get();
        return list == null ? List.of() : List.copyOf(list);
    }

    public static boolean craftingModifier() {
        return CRAFTING_MODIFIER.get();
    }

    public static boolean merchantModifier() {
        return MERCHANT_MODIFIER.get();
    }

    public static float reforgeModifier() {
        return REFORGE_MODIFIER.get().floatValue();
    }

    public static float levelzReforgeModifier() {
        return LEVELZ_REFORGE_MODIFIER.get().floatValue();
    }

    public static float luckReforgeModifier() {
        return LUCK_REFORGE_MODIFIER.get().floatValue();
    }

    public static boolean showReforgingTab() {
        return SHOW_REFORGING_TAB.get();
    }

    public static int xIconPosition() {
        return X_ICON_POSITION.get();
    }

    public static int yIconPosition() {
        return Y_ICON_POSITION.get();
    }

    public static boolean tieredTooltip() {
        return TIERED_TOOLTIP.get();
    }

    public static boolean showPlatesOnName() {
        return SHOW_PLATES_ON_NAME.get();
    }

    public static boolean centerName() {
        return CENTER_NAME.get();
    }

    public static List<String> getTierQualities(int tier) {
        List<? extends String> list = switch (tier) {
            case 1 -> TIER_1_QUALITIES.get();
            case 2 -> TIER_2_QUALITIES.get();
            case 3 -> TIER_3_QUALITIES.get();
            case 4 -> TIER_4_QUALITIES.get();
            case 5 -> TIER_5_QUALITIES.get();
            case 6 -> TIER_6_QUALITIES.get();
            default -> List.of();
        };
        return list == null ? List.of() : List.copyOf(list);
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }
}
