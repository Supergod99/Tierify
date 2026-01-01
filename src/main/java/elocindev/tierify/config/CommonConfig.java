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
        return NecConfigAPI.getFile("echelon-common.json5");
    }

    @Comment("Items in for example mineshaft chests get modifiers")
    public boolean lootContainerModifier = true;
    
    @Comment("Chance for loot-container items to receive a modifier when lootContainerModifier is true.")
    @Comment("0.0 = never, 0.5 = 50%, 1.0 = always.")
    public float lootContainerModifierChance = 0.1F;

    @Comment("Armageddon treasure bags gear can get modifiers")
    public boolean treasureBagDropModifier = true;
    public String treasureBagProfilesFile = "echelon-treasure-bag-profiles.txt";
    
    @Comment("Equipped items on entities get modifiers")
    public boolean entityItemModifier = false;
    
    @Comment("If true, entity loot-table drops can receive modifiers based on a whitelist profile file.")
    @Comment("This affects loot generated from an entity's loot table (e.g., bosses), not only worn equipment.")
    public boolean entityLootDropModifier = false;
    
    @Comment("Whitelist profile file (in the config folder) for entity loot-table drop reforging.")
    @Comment("Format per line: 'modid:entity=0.50|0,0,5,10,5,2' or 'modid:entity=0.50|overworld' (presets: overworld|nether|end|global).")
    @Comment("You can also use '*=...' as a global default and 'modid:*=...' as a namespace wildcard.")
    public String entityLootDropProfilesFile = "echelon-entity-drop-profiles.txt";

    @Comment("If true, items dropped from entity equipment slots (dropEquipment path) can be reforged using the entity loot-drop profiles file.")
    @Comment("Whitelist behavior: only entities with a matching profile entry (or wildcard entry) will affect equipment drops.")
    public boolean entityEquipmentDropModifier = true;

    @Comment("Tier weights for mob-equipped items/chests when entityItemModifier=true OR lootContainerModifier=true. Higher = more common. These are relative weights, not %.")
    @Comment("Set all to 0 to disable weighting and fall back to the old fully-random behavior.")
    public int entityTier1Weight = 2000; // Common
    public int entityTier2Weight = 200; // Uncomon
    public int entityTier3Weight = 27; // Rare
    public int entityTier4Weight = 9;  // Epic
    public int entityTier5Weight = 3;  // Legendary
    public int entityTier6Weight = 1;  // Mythic
    
    @Comment("If true, use dimension-specific tier weights for mob-equipped items and loot container rolls.")
    public boolean useDimensionTierWeights = true;
    
    @Comment("When useDimensionTierWeights=true and the selected dimension profile has ALL weights set to 0, no modifier is applied (instead of falling back to random).")
    public boolean dimensionTierWeightsZeroMeansNoModifier = true;
    
    @Comment("Overworld tier weights (1=Common ... 6=Mythic). Set a tier weight to 0 to disable that tier in the Overworld.")
    public int overworldTier1Weight = 100; // Common
    public int overworldTier2Weight = 10; // Uncomon
    public int overworldTier3Weight = 1;   // Rare
    public int overworldTier4Weight = 0;   // Epic
    public int overworldTier5Weight = 0;   // Legendary
    public int overworldTier6Weight = 0;   // Mythic
    
    @Comment("Nether tier weights (1=Common ... 6=Mythic).")
    public int netherTier1Weight = 10; // Common
    public int netherTier2Weight = 100; // Uncomon
    public int netherTier3Weight = 10;  // Rare
    public int netherTier4Weight = 1;   // Epic
    public int netherTier5Weight = 0;   // Legendary
    public int netherTier6Weight = 0;   // Mythic
    
    @Comment("End tier weights (1=Common ... 6=Mythic).")
    public int endTier1Weight = 10; // Common
    public int endTier2Weight = 100; // Uncomon
    public int endTier3Weight = 1000;  // Rare
    public int endTier4Weight = 100;   // Epic
    public int endTier5Weight = 10;   // Legendary
    public int endTier6Weight = 1;   // Mythic

    @Comment("Optional overrides for modded dimensions when useDimensionTierWeights=true.")
    @Comment("Format: 'modid:dimension=500,125,20,6,3,1' (6 ints = Common..Mythic).")
    @Comment("You can also use '*=overworld|nether|end|global' as a wildcard default for any modded dimension.")
    @Comment("And you can use 'modid:*=...' to target all dimensions from a given namespace.")
    public ArrayList<String> moddedDimensionTierWeightOverrides = new ArrayList<>();
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
    public float perfectRollChance = 0.02F;
    @Comment("If true, items can be reforged even if they are damaged.")
    public boolean allowReforgingDamaged = true;
    @Comment("Enable/disable the entire armor set bonus system")
    public boolean enableArmorSetBonuses = true;
    @Comment("Armor set bonus multiplier for a full 4-piece set. 0.01 = 1%, 1.0 = 100%")
    public float armorSetBonusMultiplier = 0.2F;
    @Comment("Armor set bonus percent when wearing 4 matching armor pieces AND all 4 are Perfect. 0.01 = 1%, 1.0 = 100%")
    public float armorSetPerfectBonusPercent = 1.0F;

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
    
    @Comment("Tier 4 of Reforging (Charoite)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 4 reforge material, Charoite by default. Can be changed via the item tag tiered:reforge_tier_4")
    public ArrayList<String> tier_4_qualities = new ArrayList<>(
        List.of(
            "Epic"
        )
    );

        @Comment("Tier 5 of Reforging (Golden Topaz)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 5 reforge material, Golden Topaz by default. Can be changed via the item tag tiered:reforge_tier_5")
    public ArrayList<String> tier_5_qualities = new ArrayList<>(
        List.of(
            "Legendary"
        )
    );

        @Comment("Tier 6 of Reforging (Painite)")
    @Comment("Qualities here will be able to be reforged onto items while using the Tier 6 reforge material, Painite by default. Can be changed via the item tag tiered:reforge_tier_6")
    public ArrayList<String> tier_6_qualities = new ArrayList<>(
        List.of(
            "Mythic"
        )
    );
}
