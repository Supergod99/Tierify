# Echelon Forge Parity Matrix (Current Codebase)

Legend: OK / PARTIAL / MISSING / WRONG

| Area | Feature | Fabric entry points | Forge entry points | Status | Parity test |
| --- | --- | --- | --- | --- | --- |
| Gameplay | Tier attributes applied to items (required/optional slots, preferred slot guard) | `Tierify` (ModifyItemAttributeModifiersCallback), `AttributeDataLoader` | `ForgeTieredAttributeSubscriber` (ItemAttributeModifierEvent) | OK | Equip tiered item in each slot; only preferred slots gain optional modifiers. |
| Gameplay | Perfect roll flag + skip negative modifiers | `ModifierUtils#setItemStackAttribute` + `Tierify` (ModifyItemAttributeModifiersCallback) | `ReforgeMenu#doReforge` + `ForgeTieredAttributeSubscriber` | OK | Reforge until Perfect; negative attributes are absent and NBT has `Tiered:Perfect=true`. |
| Gameplay | Reforge selection by tier material qualities | `ModifierUtils#getRandomAttributeForQuality` + `ReforgeScreenHandler` | `ForgeTieredAttributeSubscriber` + `ReforgeMenu` | OK | Using tier-1 material only selects Common tiers. |
| Gameplay | Reforge base item rules (data + tag fallback) | `ReforgeDataLoader` + `ReforgeScreenHandler` | `ForgeReforgeData` + `ReforgeMenu` | OK | Specific base items from `data/tiered/reforge_items/*.json` gate reforging. |
| Gameplay | Reforge cleansing (remove tier) | `ReforgeScreenHandler#reforge` | `ReforgeMenu#doReforge` | OK | Using cleansing stone clears tier and plays grindstone sound. |
| Gameplay | Random tier on loot-table items | `LootTableMixin` | `TierifyLootModifier` | OK | Looted items from chests gain tiers when `lootContainerModifier=true`. |
| Gameplay | Random tier on generated Item Frames | `ItemFrameEntityMixin` | `forge.mixin.ItemFrameEntityMixin` | OK | Items placed by structure-generated frames get tiers; player-placed do not. |
| Gameplay | Random tier on generated Armor Stands | `ArmorStandEntityMixin` | `forge.mixin.ArmorStandEntityMixin` | OK | Generated stands apply tiers to equipped items before player interaction. |
| Gameplay | Random tier on mob equipment spawn | `MobEntityMixin` | `ForgeGameplayEventSubscriber#onFinalizeSpawn` | OK | Mob-spawned gear has tiers when `entityItemModifier=true`. |
| Gameplay | Random tier on crafted items | `ItemMixin#onCraft` | `ForgeGameplayEventSubscriber#onItemCrafted` | OK | Crafted items get tiers when `craftingModifier=true`. |
| Gameplay | Merchant output tiering | `MerchantScreenHandlerMixin` | `MerchantMenuMixin` | OK | Shift-clicking trade output applies a tier when `merchantModifier=true`. |
| Gameplay | Custom attributes registered (crit, dig speed, durable, range damage, fortune) | `CustomEntityAttributes.init`, `PlayerEntityMixin#createPlayerAttributes` | `TierifyForge`, `ForgeAttributeSubscriber` | OK | Player attribute list includes Tiered custom attributes. |
| Gameplay | Crit chance, dig speed, ranged damage attribute effects | `PlayerEntityMixin`, `BowItemMixin`, `CrossbowItemMixin`, `ArrowEntityMixin`, `TridentEntityMixin`, `SpectralArrowEntityMixin`, `AttributeHelper` | `ForgeGameplayEventSubscriber` | OK | Crit chance and ranged damage bonuses affect hits/projectiles. |
| Gameplay | Fortune attribute adds to enchant level | `EnchantmentHelperMixin` | `forge.mixin.EnchantmentHelperMixin` | OK | Fortune level increases from Tiered fortune attribute. |
| Gameplay | Armor material knockback resistance injection | `ArmorItemMixin` | `forge.mixin.ArmorItemMixin` | OK | Non-netherite armor with knockback resistance grants attribute. |
| Gameplay | Durable increases max damage | `ItemStackMixin#getMaxDamage` | `forge.mixin.ItemStackMixin` | OK | Durable tier increases max durability value. |
| Gameplay | Durable affects item bar color/width | `ItemMixin#getItemBar*` | `ItemBarMixin` | OK | Durable tier changes bar width/color. |
| Gameplay | Health sync when tiered gear changes | `LivingEntityMixin` + `TieredServerPacket#HEALTH` | `ForgeGameplayEventSubscriber#onLivingEquipmentChange` | OK | Max health updates without desync on equip/unequip. |
| UI | Tooltip tier name prefix + animated gradient | `client.ItemStackClientMixin` | `forge.mixin.client.ItemStackClientMixin` | OK | Item name shows animated tier label prefix. |
| UI | Tooltip set bonus scaling (numbers) | `TieredTooltip` + `SetBonusUtils` | `forge.mixin.client.ItemStackClientMixin` | OK | Tooltip attribute numbers scale with active set bonus. |
| UI | Perfect label + set bonus label rendering | `PerfectLabelAnimator`, `PerfectBorderRenderer`, `HandledScreenMixin` | `GuiGraphicsTooltipBorderMixin` | OK | Perfect and set bonus labels render in tooltip box. |
| UI | Tooltip borders (vanilla tooltips) | `TooltipBorderLoader`, `TieredTooltip` | `ForgeTooltipBorderReloadListener`, `TierifyTooltipBorderRendererForge`, `GuiGraphicsTooltipBorderMixin` | OK | Tooltip border matches tier JSON definitions. |
| UI | Tooltip Overhaul frame integration | `TooltipOverhaulCompat`, `TooltipOverhaul*Mixin` | `TooltipOverhaulCompatForge`, `TooltipOverhaulFrameMixin` | OK | Tooltip Overhaul frames show tier borders/labels. |
| UI | Reforge/Anvil tabs show + position config | `HandledScreenMixin`, `AnvilScreenMixin`, `ClientConfig` | `ForgeScreenTabs`, `ForgeTierifyConfig` | OK | Tabs render, open screens, and apply x/y config offsets. |
| UI | Client config options (tabs, tieredTooltip, plates, centerName) | `ClientConfig`, `ModMenuIntegration` | `ForgeTierifyConfig` | OK | Client can toggle and reposition UI elements. |
| UI | Mod compat screens (SkillInfo, ModAnvil) | `SkillInfoScreenMixin`, `ModAnvilScreenMixin` | `SkillInfoScreenMixin`, `ForgeScreenTabs` | OK | Tabs/tooltips integrate in those mod screens. |
| Data/Config | Attribute data reload | `AttributeDataLoader` | `ForgeTieredAttributeSubscriber` (reload listener) | OK | `/reload` updates tier data. |
| Data/Config | Reforge base-item data reload | `ReforgeDataLoader` | `ForgeReforgeData` (reload listener) | OK | `/reload` updates reforge base items. |
| Data/Config | Attribute data sync to clients | `Tierify#registerAttributeSyncer` | `ForgeTieredAttributeSubscriber#onDatapackSync` + `AttributeSyncS2C` | OK | Joining client receives all tier definitions. |
| Data/Config | Reforge data sync to clients | `Tierify#registerReforgeItemSyncer` | `ForgeTieredAttributeSubscriber#onDatapackSync` + `ReforgeItemsSyncS2C` | OK | Joining client receives base-item mapping. |
| Data/Config | Update item NBT after datapack reload | `Tierify#updateItemStackNbt` + `ServerLifecycleEvents.END_DATA_PACK_RELOAD` | `ForgeTieredAttributeSubscriber#onDatapackSync` | OK | Existing tiered items update/clean NBT after reload. |
| Data/Config | Update item NBT on player join | `Tierify#updateItemStackNbt` + `ServerPlayConnectionEvents.INIT` | `ForgeTieredAttributeSubscriber#onDatapackSync` | OK | Player inventory tier NBT normalized on join. |
| Networking | Reforge screen open/switch | `TieredServerPacket` + `TieredClientPacket` | `OpenReforgeFromAnvilC2S`, `OpenAnvilFromReforgeC2S` | OK | Tabs switch screens and preserve mouse position. |
| Networking | Reforge trigger | `TieredClientPacket#writeC2SReforgePacket` | `TryReforgeC2S` | OK | Clicking reforge sends server action. |
| Networking | Reforge ready button disable | `TieredServerPacket#REFORGE_READY` + client handler | `ReforgeMenu` DataSlot + `ReforgeScreen.containerTick` | OK | Button disables/enables with inputs. |
