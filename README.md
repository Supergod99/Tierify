# Echelon

**Echelon** is a fork of **Tierify**, by ElocinDev (MIT LICENSED), made originally for Linggango. This mod adds in unique features, mechanics, custom animated gradients, and more. This is my first project, so expect bugs and other inconstistencies. I will try my best to fix them all!

### Differences
Echelon expands upon Tierify's existing system with the following changes

- **Reforges**
  All reforges now come with an upside and downside, regardles of tier. Each progressive tier increases both said upside and downside, effectively allowing players to specialize into their own ideal build.
    Ex. A common tier reforge on armor grants:
      +0.5 Armor, -3% Speed
    At mythic tier, this reforge would grant:
      +5 Armor, -8% Speed

  A total of over 100 reforges have been added if you play with all the optional dependencies!

  **Perfect Mechanic**
  Some of the reforge downsides can really scale up hard, especially at the apex tiers. This is why a new mechanic has been added, the *Perfect Roll*. Everytime an item is reforged, it has a base 2% chance (configurable) to roll as "Perfect" - and you will notice the downside of the item has been removed, as well as a perfect label appended beneath the item and a custom border. 

  **Modifier Names**
  Instead of the basic "Common" all the way up to "Mythic" names prepended to the item name, each modifier and tier combination now has its very own modifier name, with a custom animated gradient. Each of the 6 reforging tiers each has their own unique gradient, making the reforges feel more alive and exciting.

  **Cleansing Stone**
  In order to remove a tier from an item, a new craftable item called a Cleansing Stone has been added. To use this new item, put it into the reforge material slot of the reforge screen. Add in the repair material and the item you would like to have untiered, and click the reforge button. 
  
- **Reforging Tiers**
  Reforging has gotten an additional 3 new materials, and each can only roll their respective tier.
  - **Tier 1:** Limestone (Overworld)
    Allows reforging tools to Common Tier
  - **Tier 2:** Pyrite (Overworld)
    Allows reforging tools to Uncommon Tier
  - **Tier 3:** Galena (Nether)
    Allows reforging tools to Rare Tier
  - **Tier 4:** Charoite (Nether)
    Allows reforging tools to Epic Tier
  - **Tier 5:** Crown Topaz (End)
    Allows reforging tools to Legendary Tier
  - **Tier 6:** Painite (End)
    Allows reforging tools to Mythic Tier

- **Profile-driven drop reforging**
  Entity loot-table drops, entity equipment drops, and Armageddon treasure bags can be gated by profile files in the config folder. `entityLootDropProfilesFile` and `treasureBagProfilesFile` accept lines like `modid:entity=0.50|1,3,5,3,1,0` or presets `overworld|nether|end|global`, plus wildcards `*` or `modid:*` for defaults.

- **Dimension-specific tier weights**
  Tier roll weights can be tuned per dimension with `useDimensionTierWeights` (Overworld/Nether/End weights) and optional `moddedDimensionTierWeightOverrides` entries like `modid:dimension=500,125,20,6,3,1`. When `dimensionTierWeightsZeroMeansNoModifier` is true, all-zero weights disable rolls in that dimension.

- **Extended roll tuning**
  Additional config switches control loot-container rolls (`lootContainerModifier` + `lootContainerModifierChance`), crafting/merchant rolls, perfect roll chance, and whether damaged items can be reforged (`allowReforgingDamaged`). Reforge weight scaling can be adjusted with the reforge, LevelZ, and luck modifiers.

### Installation
Echelon is a Forge mod for Minecraft 1.20.1. It requires Forge 47.4.10+ and AttributesLib (Apothic Attributes). Optional compat is detected at runtime for mods like Tooltip Overhaul, Obscure API, Curios, Armageddon Mod, Brutality, and others.

### Customizations

Echelon is entirely data-driven, which means you can add, modify, and remove modifiers as you see fit. The base path for modifiers is `data/modid/item_attributes`, and tiered modifiers are stored under the modid of tiered. Here's an example modifier called "Hasteful," which grants additional dig speed when any of the valid tools are held:
```json
{
  "id": "tiered:hasteful",
  "verifiers": [
    {
      "tag": "c:pickaxes"
    },
    {
      "tag": "c:shovels"
    },
    {
      "tag": "c:axes"
    }
  ],
  "weight": 10,
  "style": {
    "color": "GREEN"
  },
  "attributes": [
    {
      "type": "generic.dig_speed",
      "modifier": {
        "name": "tiered:hasteful",
        "operation": "MULTIPLY_BASE",
        "amount": 0.10
      },
      "optional_equipment_slots": [
        "MAINHAND"
      ]
    }
  ]
}
```

#### Attributes

Echelon adds in one more custom attribute that can be utilized on top of the three tiered currently provides: Dig Speed, Crit chance, Durability, and our very own Fortune. Dig Speed increases the speed of your block breaking (think: haste), Crit Chance offers an additional random chance to crit when using a tool and Durability increases, who would have thought it, the durability of an item. Fortune is identical to vanilla fortune, but can be utilized on modifiers now.

Types: `generic.armor`, `generic.armor_toughness`, `generic.dig_speed`, `tiered:generic.durable`, `generic.max_health`, `generic.movement_speed`, `reach-entity-attributes:reach`, `generic.luck`, `generic.attack_damage`, `tiered:generic.crit_chance`, `reach-entity-attributes:attack_range`, `tiered:generic.range_attack_damage`,`tiered:generic.fortune`

#### Verifiers

A verifier (specified in the "verifiers" array of your modifier json file) defines whether or not a given tag or tool is valid for the modifier. 

A specific item ID can be specified with:
```json
"id": "minecraft:apple"
```

and a tag can be specified with:
```json
"tag": "c:helmets"
```

Tiered doesn't provide tags, so define your own with datapacks or Forge tag conventions. This repo already targets common `c:` and `forge:` tags (ex: `c:tools`, `c:pickaxes`, `forge:tools/swords`, `forge:armor/boots`) and you can extend them as needed.

#### Weight

The weight determines the commonness of the tier. Higher weights increase the chance of being applied on the item and vice versa.

#### Nbt

Custom nbt can get added via nbtValues, an example can be found below. It supports only string, boolean, integer and double values.\ 
Caution! Once added nbt keys won't get removed when once applied, just the values can get updated!

```json
"nbtValues": {
  "Damage": 100,
  "key": "value"
}
```

#### Tooltip
Since V1.2, custom tooltip borders can get set via a resource pack.
- The border texture has to be in the `assets\tiered\textures\gui` folder.
- The file has to be a json file and put inside the `assets\tiered\tooltips` folder.
- The `background_gradient` can also get set.
- The gradients has to be hex code, check transparency here: [https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4](https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4)
- Check out the default datapack under `src\main\resources\assets\tiered\tooltips`.

Example:
```json
{ 
    "tooltips": [
        {
            "index": 0,
            "start_border_gradient": "FFBABABA",
            "end_border_gradient": "FF565656",
            "texture": "tiered_borders",
            "decider": [
                "set_the_id_here",
                "tiered:common_armor"
            ]
        }
    ]
}
```

#### Reforge

Reforging items to get other tiers can be done at the anvil. There is a slot which is called "base" on the left and a slot called "addition" on the right.
The addition slot can only contain items which are stated in each tier item tag (`tiered:reforge_tier_1`, `tiered:reforge_tier_2`, `tiered:reforge_tier_3`, `tiered:reforge_tier_4`, `tiered:reforge_tier_5`, `tiered:reforge_tier_6`). The base slot can contain the reforging item material item if existent, otherwise it can only contain `tiered:reforge_base_item` tag items. The base slot item can get changed via datapack, an example can be found below and has to get put in the `tiered:reforge_items` folder.

```json
{
  "items": [
    "minecraft:bow"
  ],
  "base": [
    "minecraft:string"
  ]
}
```

### Credits
- **Draylar1** for making **Tiered**, the original mod.
- **Globox_Z** for making **TieredZ**, a fork of Tiered which Tierify is based upon.
- **ElocinDev** for making **Tierify**, which is based off of Echelon, now a fork of a fork. 

### License
Echelon's code is licensed under MIT. You are free to use the code inside this repo as you want as long as you meet the license's conditions.
Newer assets (Such as Limestone, Pyrite, Galena, Charoite, Crown Topaz, Painite, Cleansing Stone) are All Rights Reserved, and you may not use them without explicit permission.
