package elocindev.tierify.config;

import java.util.ArrayList;
import java.util.List;

import elocindev.necronomicon.api.config.v1.NecConfigAPI;
import elocindev.necronomicon.config.Comment;
import elocindev.necronomicon.config.NecConfig;

public class CommonConfig {
    @NecConfig
    public static CommonConfig INSTANCE;

    public static String getFile() {
        return NecConfigAPI.getFile("tierify-common.json5");
    }

    @Comment("Items in for example mineshaft chests get modifiers")
    public boolean lootContainerModifier = false;
    @Comment("Equipped items on entities get modifiers")
    public boolean entityItemModifier = true;
    @Comment("Crafted items get modifiers")
    public boolean craftingModifier = false;
    @Comment("Merchant items get modifiers")
    public boolean merchantModifier = false;
    @Comment("Decreases the biggest weights by this modifier")
    public float reforgeModifier = 0.0F;
    @Comment("Modify the biggest weights by this modifier per smithing level")
    public float levelzReforgeModifier = 0.0F;
    @Comment("Modify the biggest weights by this modifier per luck")
    public float luckReforgeModifier = 0.0F;
    @Comment("The chance for a reforge to be 'Perfect' (no negative attributes). 0.01 = 1%, 1.0 = 100%")
    public float perfectRollChance = 0.01F;
    @Comment("If true, items can be reforged even if they are damaged.")
    public boolean allowReforgingDamaged = true;
    @Comment("Armor set bonus multiplier for a full 4-piece set. 0.01 = 1%, 1.0 = 100%")
    public float armorSetBonusMultiplier = 0.15F;
    @Comment("Armor set bonus percent when wearing 4 matching armor pieces AND all 4 are Perfect. 0.01 = 1%, 1.0 = 100%")
    public float armorSetPerfectBonusPercent = 0.5F;

    @Comment("Tier 1 of Reforging (Limestone)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 1 reforge material, Limestone by default. Can be changed via the item tag tiered:reforge_tier_1")
    public ArrayList<String> tier_1_qualities = new ArrayList<>(
        List.of(
            "Common"
        )
    );

    @Comment("Tier 2 of Reforging (Pyrite)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 2 reforge material, Pyrite by default. Can be changed via the item tag tiered:reforge_tier_2")
    public ArrayList<String> tier_2_qualities = new ArrayList<>(
        List.of(
            "Uncomon"
        )
    );

    @Comment("Tier 3 of Reforging (Galena)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 3 reforge material, Galena by default. Can be changed via the item tag tiered:reforge_tier_3")
    public ArrayList<String> tier_3_qualities = new ArrayList<>(
        List.of(
            "Rare"
        )
    );
    
    @Comment("Tier 4 of Reforging (Eclipsium Alloy)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 4 reforge material, Eclipsium Alloy by default. Can be changed via the item tag tiered:reforge_tier_4")
    public ArrayList<String> tier_4_qualities = new ArrayList<>(
        List.of(
            "Epic"
        )
    );

        @Comment("Tier 5 of Reforging (Hemalith Catalyst)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 5 reforge material, Hemalith Catalyst by default. Can be changed via the item tag tiered:reforge_tier_5")
    public ArrayList<String> tier_5_qualities = new ArrayList<>(
        List.of(
            "Legendary"
        )
    );

        @Comment("Tier 6 of Reforging (Cosmos Shard)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 6 reforge material, Cosmos Shard by default. Can be changed via the item tag tiered:reforge_tier_6")
    public ArrayList<String> tier_6_qualities = new ArrayList<>(
        List.of(
            "Mythic"
        )
    );
}
