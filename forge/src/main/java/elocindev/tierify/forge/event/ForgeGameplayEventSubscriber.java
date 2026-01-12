package elocindev.tierify.forge.event;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.util.ForgeAttributeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeGameplayEventSubscriber {

    private ForgeGameplayEventSubscriber() {}

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!ForgeTierifyConfig.craftingModifier()) return;
        if (event.getEntity() == null || event.getEntity().level().isClientSide()) return;

        ItemStack crafted = event.getCrafting();
        if (crafted == null || crafted.isEmpty()) return;

        ForgeTieredAttributeSubscriber.applyRandomTierIfAbsent(crafted);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!ForgeTierifyConfig.entityItemModifier()) return;

        Mob mob = event.getEntity();
        if (mob == null || mob.level().isClientSide()) return;

        var dimensionId = mob.level().dimension().location();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mob.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            ForgeTieredAttributeSubscriber.applyTierFromEntityWeights(stack, dimensionId, mob.getRandom());
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        if (player == null) return;
        event.setNewSpeed(ForgeAttributeHelper.getExtraDigSpeed(player, event.getNewSpeed()));
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        if (event.isVanillaCritical()) return;
        Player player = event.getEntity();
        if (player == null) return;

        if (ForgeAttributeHelper.shouldMeeleCrit(player)) {
            event.setResult(Event.Result.ALLOW);
            event.setDamageModifier(1.5F);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;

        if (arrow.getOwner() instanceof Player player) {
            double base = arrow.getBaseDamage();
            float critAdjusted = ForgeAttributeHelper.getExtraCritDamage(player, (float) base);
            float rangeAdjusted = ForgeAttributeHelper.getExtraRangeDamage(player, critAdjusted);
            arrow.setBaseDamage(rangeAdjusted);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof ThrownTrident trident) {
            if (trident.getOwner() instanceof Player player) {
                event.setAmount(ForgeAttributeHelper.getExtraRangeDamage(player, event.getAmount()));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack from = event.getFrom();
        if (from.isEmpty()) return;

        if (!hasTieredModifier(from, event.getSlot())) return;

        ItemStack to = event.getTo();
        boolean syncHealth = to.isEmpty();

        if (!syncHealth) {
            if (from.getItem() != to.getItem()) {
                syncHealth = true;
            } else {
                CompoundTag oldTag = from.getTag() == null ? new CompoundTag() : from.getTag().copy();
                CompoundTag newTag = to.getTag() == null ? new CompoundTag() : to.getTag().copy();

                oldTag.remove("Damage");
                oldTag.remove("iced");
                newTag.remove("Damage");
                newTag.remove("iced");

                if (!oldTag.equals(newTag)) {
                    syncHealth = true;
                }
            }
        }

        if (syncHealth) {
            float max = player.getMaxHealth();
            if (player.getHealth() > max) {
                player.setHealth(max);
            }
            player.connection.send(new ClientboundSetHealthPacket(
                    player.getHealth(),
                    player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel()
            ));
        }
    }

    private static boolean hasTieredModifier(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return false;
        var modifiers = stack.getAttributeModifiers(slot);
        if (modifiers == null || modifiers.isEmpty()) return false;

        for (AttributeModifier mod : modifiers.values()) {
            if (mod.getName() != null && mod.getName().contains("tiered:")) {
                return true;
            }
        }

        return false;
    }
}
