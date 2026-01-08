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
    public static final ForgeConfigSpec.BooleanValue ENTITY_ITEM_MODIFIER;
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
                .comment("Armor set bonus percent when wearing 4 matching armor pieces AND all 4 are Perfect.")
                .defineInRange("armorSetPerfectBonusPercent", 0.5d, 0.0d, 10.0d);

        PERFECT_ROLL_CHANCE = builder
                .comment("The chance for a reforge to be 'Perfect' (no negative attributes). 0.01 = 1%, 1.0 = 100%")
                .defineInRange("perfectRollChance", 0.02d, 0.0d, 1.0d);

        ALLOW_REFORGING_DAMAGED = builder
                .comment("If true, items can be reforged even if they are damaged.")
                .define("allowReforgingDamaged", true);

        LOOT_CONTAINER_MODIFIER = builder
                .comment("Items in for example mineshaft chests get modifiers")
                .define("lootContainerModifier", false);

        ENTITY_ITEM_MODIFIER = builder
                .comment("Equipped items on entities get modifiers")
                .define("entityItemModifier", true);

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
                .comment("Tier 4 of Reforging (Eclipsium Alloy)")
                .defineList("tier4Qualities", List.of("Epic"), ForgeTierifyConfig::isString);

        TIER_5_QUALITIES = builder
                .comment("Tier 5 of Reforging (Hemalith Catalyst)")
                .defineList("tier5Qualities", List.of("Legendary"), ForgeTierifyConfig::isString);

        TIER_6_QUALITIES = builder
                .comment("Tier 6 of Reforging (Cosmos Shard)")
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

    public static boolean entityItemModifier() {
        return ENTITY_ITEM_MODIFIER.get();
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
