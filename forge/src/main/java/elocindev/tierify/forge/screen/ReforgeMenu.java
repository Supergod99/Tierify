package elocindev.tierify.forge.screen;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.reforge.ForgeReforgeData;
import elocindev.tierify.forge.registry.ForgeMenuTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ReforgeMenu extends AbstractContainerMenu {
    private static final TagKey<Item> TAG_REFORGE_BASE_ITEM = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_base_item")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_1 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_1")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_2 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_2")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_3 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_3")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_4 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_4")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_5 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_5")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_6 = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_6")
    );
    private static final TagKey<Item> TAG_REFORGE_TIER_CLEANSE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "reforge_tier_cleanse")
    );

    private final Container inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ReforgeMenu.this.slotsChanged(this);
        }
    };

    private final ContainerLevelAccess access;

    private final DataSlot reforgeReady = DataSlot.standalone();

    public ReforgeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, ContainerLevelAccess.create(inv.player.level(), buf.readBlockPos()));
    }

    public ReforgeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ForgeMenuTypes.REFORGE.get(), id);
        this.access = access;

        addDataSlot(reforgeReady);

        addSlot(new Slot(inputs, 0, 45, 47));

        addSlot(new Slot(inputs, 1, 80, 34) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty();
            }
        });

        addSlot(new Slot(inputs, 2, 115, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidAddition(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    public boolean isReforgeReady() {
        return reforgeReady.get() == 1;
    }

    public ContainerLevelAccess getAccess() {
        return access;
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        reforgeReady.set(computeReady() ? 1 : 0);
    }

    private boolean computeReady() {
        ItemStack base = inputs.getItem(0);
        ItemStack target = inputs.getItem(1);
        ItemStack add = inputs.getItem(2);

        if (base.isEmpty() || target.isEmpty() || add.isEmpty()) return false;
        if (!ForgeTierifyConfig.allowReforgingDamaged() && target.isDamaged()) return false;
        if (!ForgeTieredAttributeSubscriber.hasAnyValidTier(target)) return false;
        if (!isValidAddition(add)) return false;
        if (!isValidBase(base, target)) return false;

        return true;
    }

    private static boolean isValidAddition(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(TAG_REFORGE_TIER_CLEANSE)) return true;

        return stack.is(TAG_REFORGE_TIER_1)
                || stack.is(TAG_REFORGE_TIER_2)
                || stack.is(TAG_REFORGE_TIER_3)
                || stack.is(TAG_REFORGE_TIER_4)
                || stack.is(TAG_REFORGE_TIER_5)
                || stack.is(TAG_REFORGE_TIER_6);
    }

    private boolean isValidBase(ItemStack baseStack, ItemStack target) {
        if (baseStack.isEmpty()) return false;

        Set<Item> required = ForgeReforgeData.getBaseItemsForTarget(target.getItem());
        if (!required.isEmpty()) {
            return required.contains(baseStack.getItem());
        }

        if (matchesRepairIngredient(target, baseStack)) return true;

        return baseStack.is(TAG_REFORGE_BASE_ITEM);
    }

    private static boolean matchesRepairIngredient(ItemStack target, ItemStack baseStack) {
        Item item = target.getItem();
        if (item instanceof TieredItem tool) {
            return tool.getTier().getRepairIngredient().test(baseStack);
        }
        if (item instanceof ArmorItem armor) {
            return armor.getMaterial().getRepairIngredient().test(baseStack);
        }
        return false;
    }

    private static boolean isCleansing(ItemStack stack) {
        return stack.is(TAG_REFORGE_TIER_CLEANSE);
    }

    public void doReforge(ServerPlayer sp) {
        if (!computeReady()) return;

        ItemStack target = inputs.getItem(1);
        ItemStack base = inputs.getItem(0);
        ItemStack add = inputs.getItem(2);

        ForgeTieredAttributeSubscriber.clearTieredData(target);

        if (isCleansing(add)) {
            add.shrink(1);
            base.shrink(1);
            slotsChanged(inputs);
            broadcastChanges();
            access.execute((level, pos) -> {
                playCleansingSound(level, pos);
                playAnvilEvent(level, pos);
            });
            return;
        }

        List<String> qualities = qualitiesFor(add);
        ResourceLocation chosen = ForgeTieredAttributeSubscriber.pickRandomTierForReforge(target, qualities, sp.getRandom(), sp);
        if (chosen == null && qualities != null) {
            chosen = ForgeTieredAttributeSubscriber.pickRandomTierForReforge(target, null, sp.getRandom(), sp);
        }
        if (chosen == null) return;

        ForgeTieredAttributeSubscriber.stashCustomNameForReforge(target);

        boolean isPerfect = sp.getRandom().nextDouble() < ForgeTierifyConfig.perfectRollChance();
        ForgeTieredAttributeSubscriber.applyTier(target, chosen, isPerfect);
        ResourceLocation chosenFinal = chosen;

        base.shrink(1);
        add.shrink(1);

        slotsChanged(inputs);
        broadcastChanges();

        access.execute((level, pos) -> {
            playReforgeSound(level, pos, chosenFinal);
            playAnvilEvent(level, pos);
        });
    }

    private static List<String> qualitiesFor(ItemStack add) {
        if (add.is(TAG_REFORGE_TIER_1)) return ForgeTierifyConfig.getTierQualities(1);
        if (add.is(TAG_REFORGE_TIER_2)) return ForgeTierifyConfig.getTierQualities(2);
        if (add.is(TAG_REFORGE_TIER_3)) return ForgeTierifyConfig.getTierQualities(3);
        if (add.is(TAG_REFORGE_TIER_4)) return ForgeTierifyConfig.getTierQualities(4);
        if (add.is(TAG_REFORGE_TIER_5)) return ForgeTierifyConfig.getTierQualities(5);
        if (add.is(TAG_REFORGE_TIER_6)) return ForgeTierifyConfig.getTierQualities(6);
        return null;
    }

    private static void playCleansingSound(Level level, net.minecraft.core.BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private static void playReforgeSound(Level level, net.minecraft.core.BlockPos pos, ResourceLocation chosenTierId) {
        SoundEvent toPlay = SoundEvents.ANVIL_USE;

        if (chosenTierId != null) {
            String path = chosenTierId.getPath().toLowerCase(Locale.ROOT);
            String key = null;
            if (path.startsWith("common_")) key = "reforge_sound_common";
            else if (path.startsWith("uncomon_") || path.startsWith("uncommon_")) key = "reforge_sound_uncommon";
            else if (path.startsWith("rare_")) key = "reforge_sound_rare";
            else if (path.startsWith("epic_")) key = "reforge_sound_epic";
            else if (path.startsWith("legendary_")) key = "reforge_sound_legendary";
            else if (path.startsWith("mythic_")) key = "reforge_sound_mythic";

            if (key != null) {
                ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, key);
                SoundEvent se = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                if (se != null) toPlay = se;
            }
        }

        level.playSound(null, pos, toPlay, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    private static void playAnvilEvent(Level level, net.minecraft.core.BlockPos pos) {
        level.levelEvent(null, 1030, pos, 0);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            this.clearContainer(player, this.inputs);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Blocks.ANVIL);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemStack = stack.copy();

            if (index == 1) {
                if (!this.moveItemStackTo(stack, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, itemStack);
            } else if (index == 0 || index == 2) {
                if (!this.moveItemStackTo(stack, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 3 && index < 39) {
                if (isValidAddition(stack) && !this.moveItemStackTo(stack, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }

                if (this.getSlot(1).hasItem()) {
                    Item item = this.getSlot(1).getItem().getItem();
                    if (item instanceof TieredItem tool && tool.getTier().getRepairIngredient().test(stack)
                            && !this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (item instanceof ArmorItem armor && armor.getMaterial().getRepairIngredient() != null
                            && armor.getMaterial().getRepairIngredient().test(stack)
                            && !this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    if (stack.is(TAG_REFORGE_BASE_ITEM) && !this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                    Set<Item> items = ForgeReforgeData.getBaseItemsForTarget(item);
                    if (items.contains(stack.getItem()) && !this.moveItemStackTo(stack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (ForgeTieredAttributeSubscriber.hasAnyValidTier(stack) && !this.moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return itemStack;
    }
}
