package elocindev.tierify.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import elocindev.tierify.TierifyCommon;
import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeCommandInit {

    private static final List<String> TIER_LIST =
            List.of("common", "uncommon", "rare", "epic", "legendary", "mythic");

    private ForgeCommandInit() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tiered")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("tier")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.literal("common")
                                        .executes(ctx -> executeCommand(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                0)))
                                .then(Commands.literal("uncommon")
                                        .executes(ctx -> executeCommand(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                1)))
                                .then(Commands.literal("rare")
                                        .executes(ctx -> executeCommand(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                2)))
                                .then(Commands.literal("epic")
                                        .executes(ctx -> executeCommand(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                3)))
                                .then(Commands.literal("legendary")
                                        .executes(ctx -> executeCommand(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                4)))
                                .then(Commands.literal("mythic")
                                        .executes(ctx -> executeCommand(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                5)))))
                .then(Commands.literal("untier")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> executeCommand(
                                        ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"),
                                        -1)))));
    }

    private static int executeCommand(CommandSourceStack source, Collection<ServerPlayer> targets, int tier) {
        for (ServerPlayer player : targets) {
            ItemStack itemStack = player.getMainHandItem();

            if (itemStack.isEmpty()) {
                source.sendSuccess(
                        () -> Component.translatable("commands.tiered.failed", player.getDisplayName()),
                        true);
                continue;
            }

            if (tier == -1) {
                if (itemStack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY) != null) {
                    ForgeTieredAttributeSubscriber.clearTieredData(itemStack);
                    source.sendSuccess(
                            () -> Component.translatable(
                                    "commands.tiered.untier",
                                    itemStack.getItem().getName(itemStack).getString(),
                                    player.getDisplayName()),
                            true);
                } else {
                    source.sendSuccess(
                            () -> Component.translatable(
                                    "commands.tiered.untier_failed",
                                    itemStack.getItem().getName(itemStack).getString(),
                                    player.getDisplayName()),
                            true);
                }
                continue;
            }

            List<ResourceLocation> potentialTier =
                    ForgeTieredAttributeSubscriber.findTierIdsForCommand(itemStack, TIER_LIST.get(tier));
            if (potentialTier.isEmpty()) {
                source.sendSuccess(
                        () -> Component.translatable(
                                "commands.tiered.tiering_failed",
                                itemStack.getItem().getName(itemStack).getString(),
                                player.getDisplayName()),
                        true);
                continue;
            }

            ForgeTieredAttributeSubscriber.clearTieredData(itemStack);

            ResourceLocation attribute = potentialTier.get(player.getRandom().nextInt(potentialTier.size()));
            if (ForgeTieredAttributeSubscriber.applyTier(itemStack, attribute, false)) {
                source.sendSuccess(
                        () -> Component.translatable(
                                "commands.tiered.tier",
                                itemStack.getItem().getName(itemStack).getString(),
                                player.getDisplayName()),
                        true);
            } else {
                source.sendSuccess(
                        () -> Component.translatable(
                                "commands.tiered.tiering_failed",
                                itemStack.getItem().getName(itemStack).getString(),
                                player.getDisplayName()),
                        true);
            }
        }

        return 1;
    }
}
