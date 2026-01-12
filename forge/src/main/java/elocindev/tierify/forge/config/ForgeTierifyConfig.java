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

    public record SyncedConfig(
            boolean enableArmorSetBonuses,
            float armorSetBonusMultiplier,
            float armorSetPerfectBonusPercent,
            double perfectRollChance,
            boolean allowReforgingDamaged,
            boolean lootContainerModifier,
            float lootContainerModifierChance,
            boolean treasureBagDropModifier,
            String treasureBagProfilesFile,
            boolean entityItemModifier,
            boolean entityLootDropModifier,
            String entityLootDropProfilesFile,
            boolean entityEquipmentDropModifier,
            int entityTier1Weight,
            int entityTier2Weight,
            int entityTier3Weight,
            int entityTier4Weight,
            int entityTier5Weight,
            int entityTier6Weight,
            boolean useDimensionTierWeights,
            boolean dimensionTierWeightsZeroMeansNoModifier,
            int overworldTier1Weight,
            int overworldTier2Weight,
            int overworldTier3Weight,
            int overworldTier4Weight,
            int overworldTier5Weight,
            int overworldTier6Weight,
            int netherTier1Weight,
            int netherTier2Weight,
            int netherTier3Weight,
            int netherTier4Weight,
            int netherTier5Weight,
            int netherTier6Weight,
            int endTier1Weight,
            int endTier2Weight,
            int endTier3Weight,
            int endTier4Weight,
            int endTier5Weight,
            int endTier6Weight,
            List<String> moddedDimensionTierWeightOverrides,
            boolean craftingModifier,
            boolean merchantModifier,
            float reforgeModifier,
            float levelzReforgeModifier,
            float luckReforgeModifier,
            boolean showReforgingTab,
            int xIconPosition,
            int yIconPosition,
            boolean tieredTooltip,
            boolean showPlatesOnName,
            boolean centerName,
            List<String> tier1Qualities,
            List<String> tier2Qualities,
            List<String> tier3Qualities,
            List<String> tier4Qualities,
            List<String> tier5Qualities,
            List<String> tier6Qualities
    ) {}

    private static volatile SyncedConfig syncedConfig;

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

    public static void applySyncedConfig(SyncedConfig config) {
        syncedConfig = config;
    }

    public static SyncedConfig snapshot() {
        List<? extends String> moddedOverrides = MODDED_DIMENSION_TIER_WEIGHT_OVERRIDES.get();
        List<? extends String> tier1 = TIER_1_QUALITIES.get();
        List<? extends String> tier2 = TIER_2_QUALITIES.get();
        List<? extends String> tier3 = TIER_3_QUALITIES.get();
        List<? extends String> tier4 = TIER_4_QUALITIES.get();
        List<? extends String> tier5 = TIER_5_QUALITIES.get();
        List<? extends String> tier6 = TIER_6_QUALITIES.get();

        return new SyncedConfig(
                ENABLE_ARMOR_SET_BONUSES.get(),
                ARMOR_SET_BONUS_MULTIPLIER.get().floatValue(),
                ARMOR_SET_PERFECT_BONUS_PERCENT.get().floatValue(),
                PERFECT_ROLL_CHANCE.get(),
                ALLOW_REFORGING_DAMAGED.get(),
                LOOT_CONTAINER_MODIFIER.get(),
                LOOT_CONTAINER_MODIFIER_CHANCE.get().floatValue(),
                TREASURE_BAG_DROP_MODIFIER.get(),
                TREASURE_BAG_PROFILES_FILE.get(),
                ENTITY_ITEM_MODIFIER.get(),
                ENTITY_LOOT_DROP_MODIFIER.get(),
                ENTITY_LOOT_DROP_PROFILES_FILE.get(),
                ENTITY_EQUIPMENT_DROP_MODIFIER.get(),
                ENTITY_TIER1_WEIGHT.get(),
                ENTITY_TIER2_WEIGHT.get(),
                ENTITY_TIER3_WEIGHT.get(),
                ENTITY_TIER4_WEIGHT.get(),
                ENTITY_TIER5_WEIGHT.get(),
                ENTITY_TIER6_WEIGHT.get(),
                USE_DIMENSION_TIER_WEIGHTS.get(),
                DIMENSION_TIER_WEIGHTS_ZERO_MEANS_NO_MODIFIER.get(),
                OVERWORLD_TIER1_WEIGHT.get(),
                OVERWORLD_TIER2_WEIGHT.get(),
                OVERWORLD_TIER3_WEIGHT.get(),
                OVERWORLD_TIER4_WEIGHT.get(),
                OVERWORLD_TIER5_WEIGHT.get(),
                OVERWORLD_TIER6_WEIGHT.get(),
                NETHER_TIER1_WEIGHT.get(),
                NETHER_TIER2_WEIGHT.get(),
                NETHER_TIER3_WEIGHT.get(),
                NETHER_TIER4_WEIGHT.get(),
                NETHER_TIER5_WEIGHT.get(),
                NETHER_TIER6_WEIGHT.get(),
                END_TIER1_WEIGHT.get(),
                END_TIER2_WEIGHT.get(),
                END_TIER3_WEIGHT.get(),
                END_TIER4_WEIGHT.get(),
                END_TIER5_WEIGHT.get(),
                END_TIER6_WEIGHT.get(),
                moddedOverrides == null ? List.of() : List.copyOf(moddedOverrides),
                CRAFTING_MODIFIER.get(),
                MERCHANT_MODIFIER.get(),
                REFORGE_MODIFIER.get().floatValue(),
                LEVELZ_REFORGE_MODIFIER.get().floatValue(),
                LUCK_REFORGE_MODIFIER.get().floatValue(),
                SHOW_REFORGING_TAB.get(),
                X_ICON_POSITION.get(),
                Y_ICON_POSITION.get(),
                TIERED_TOOLTIP.get(),
                SHOW_PLATES_ON_NAME.get(),
                CENTER_NAME.get(),
                tier1 == null ? List.of() : List.copyOf(tier1),
                tier2 == null ? List.of() : List.copyOf(tier2),
                tier3 == null ? List.of() : List.copyOf(tier3),
                tier4 == null ? List.of() : List.copyOf(tier4),
                tier5 == null ? List.of() : List.copyOf(tier5),
                tier6 == null ? List.of() : List.copyOf(tier6)
        );
    }

    public static boolean enableArmorSetBonuses() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.enableArmorSetBonuses() : ENABLE_ARMOR_SET_BONUSES.get();
    }

    public static float armorSetBonusMultiplier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.armorSetBonusMultiplier() : ARMOR_SET_BONUS_MULTIPLIER.get().floatValue();
    }

    public static float armorSetPerfectBonusPercent() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.armorSetPerfectBonusPercent() : ARMOR_SET_PERFECT_BONUS_PERCENT.get().floatValue();
    }

    public static double perfectRollChance() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.perfectRollChance() : PERFECT_ROLL_CHANCE.get();
    }

    public static boolean allowReforgingDamaged() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.allowReforgingDamaged() : ALLOW_REFORGING_DAMAGED.get();
    }

    public static boolean lootContainerModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.lootContainerModifier() : LOOT_CONTAINER_MODIFIER.get();
    }

    public static float lootContainerModifierChance() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.lootContainerModifierChance() : LOOT_CONTAINER_MODIFIER_CHANCE.get().floatValue();
    }

    public static boolean treasureBagDropModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.treasureBagDropModifier() : TREASURE_BAG_DROP_MODIFIER.get();
    }

    public static String treasureBagProfilesFile() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.treasureBagProfilesFile() : TREASURE_BAG_PROFILES_FILE.get();
    }

    public static boolean entityItemModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityItemModifier() : ENTITY_ITEM_MODIFIER.get();
    }

    public static boolean entityLootDropModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityLootDropModifier() : ENTITY_LOOT_DROP_MODIFIER.get();
    }

    public static String entityLootDropProfilesFile() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityLootDropProfilesFile() : ENTITY_LOOT_DROP_PROFILES_FILE.get();
    }

    public static boolean entityEquipmentDropModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityEquipmentDropModifier() : ENTITY_EQUIPMENT_DROP_MODIFIER.get();
    }

    public static int entityTier1Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityTier1Weight() : ENTITY_TIER1_WEIGHT.get();
    }

    public static int entityTier2Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityTier2Weight() : ENTITY_TIER2_WEIGHT.get();
    }

    public static int entityTier3Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityTier3Weight() : ENTITY_TIER3_WEIGHT.get();
    }

    public static int entityTier4Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityTier4Weight() : ENTITY_TIER4_WEIGHT.get();
    }

    public static int entityTier5Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityTier5Weight() : ENTITY_TIER5_WEIGHT.get();
    }

    public static int entityTier6Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.entityTier6Weight() : ENTITY_TIER6_WEIGHT.get();
    }

    public static boolean useDimensionTierWeights() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.useDimensionTierWeights() : USE_DIMENSION_TIER_WEIGHTS.get();
    }

    public static boolean dimensionTierWeightsZeroMeansNoModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.dimensionTierWeightsZeroMeansNoModifier() : DIMENSION_TIER_WEIGHTS_ZERO_MEANS_NO_MODIFIER.get();
    }

    public static int overworldTier1Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.overworldTier1Weight() : OVERWORLD_TIER1_WEIGHT.get();
    }

    public static int overworldTier2Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.overworldTier2Weight() : OVERWORLD_TIER2_WEIGHT.get();
    }

    public static int overworldTier3Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.overworldTier3Weight() : OVERWORLD_TIER3_WEIGHT.get();
    }

    public static int overworldTier4Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.overworldTier4Weight() : OVERWORLD_TIER4_WEIGHT.get();
    }

    public static int overworldTier5Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.overworldTier5Weight() : OVERWORLD_TIER5_WEIGHT.get();
    }

    public static int overworldTier6Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.overworldTier6Weight() : OVERWORLD_TIER6_WEIGHT.get();
    }

    public static int netherTier1Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.netherTier1Weight() : NETHER_TIER1_WEIGHT.get();
    }

    public static int netherTier2Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.netherTier2Weight() : NETHER_TIER2_WEIGHT.get();
    }

    public static int netherTier3Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.netherTier3Weight() : NETHER_TIER3_WEIGHT.get();
    }

    public static int netherTier4Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.netherTier4Weight() : NETHER_TIER4_WEIGHT.get();
    }

    public static int netherTier5Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.netherTier5Weight() : NETHER_TIER5_WEIGHT.get();
    }

    public static int netherTier6Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.netherTier6Weight() : NETHER_TIER6_WEIGHT.get();
    }

    public static int endTier1Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.endTier1Weight() : END_TIER1_WEIGHT.get();
    }

    public static int endTier2Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.endTier2Weight() : END_TIER2_WEIGHT.get();
    }

    public static int endTier3Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.endTier3Weight() : END_TIER3_WEIGHT.get();
    }

    public static int endTier4Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.endTier4Weight() : END_TIER4_WEIGHT.get();
    }

    public static int endTier5Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.endTier5Weight() : END_TIER5_WEIGHT.get();
    }

    public static int endTier6Weight() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.endTier6Weight() : END_TIER6_WEIGHT.get();
    }

    public static List<String> moddedDimensionTierWeightOverrides() {
        SyncedConfig sc = syncedConfig;
        if (sc != null) {
            return sc.moddedDimensionTierWeightOverrides();
        }
        List<? extends String> list = MODDED_DIMENSION_TIER_WEIGHT_OVERRIDES.get();
        return list == null ? List.of() : List.copyOf(list);
    }

    public static boolean craftingModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.craftingModifier() : CRAFTING_MODIFIER.get();
    }

    public static boolean merchantModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.merchantModifier() : MERCHANT_MODIFIER.get();
    }

    public static float reforgeModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.reforgeModifier() : REFORGE_MODIFIER.get().floatValue();
    }

    public static float levelzReforgeModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.levelzReforgeModifier() : LEVELZ_REFORGE_MODIFIER.get().floatValue();
    }

    public static float luckReforgeModifier() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.luckReforgeModifier() : LUCK_REFORGE_MODIFIER.get().floatValue();
    }

    public static boolean showReforgingTab() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.showReforgingTab() : SHOW_REFORGING_TAB.get();
    }

    public static int xIconPosition() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.xIconPosition() : X_ICON_POSITION.get();
    }

    public static int yIconPosition() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.yIconPosition() : Y_ICON_POSITION.get();
    }

    public static boolean tieredTooltip() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.tieredTooltip() : TIERED_TOOLTIP.get();
    }

    public static boolean showPlatesOnName() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.showPlatesOnName() : SHOW_PLATES_ON_NAME.get();
    }

    public static boolean centerName() {
        SyncedConfig sc = syncedConfig;
        return sc != null ? sc.centerName() : CENTER_NAME.get();
    }

    public static List<String> getTierQualities(int tier) {
        SyncedConfig sc = syncedConfig;
        if (sc != null) {
            return switch (tier) {
                case 1 -> sc.tier1Qualities();
                case 2 -> sc.tier2Qualities();
                case 3 -> sc.tier3Qualities();
                case 4 -> sc.tier4Qualities();
                case 5 -> sc.tier5Qualities();
                case 6 -> sc.tier6Qualities();
                default -> List.of();
            };
        }
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
