package elocindev.tierify.forge.network.c2s;

import elocindev.tierify.forge.mixin.ItemCombinerMenuAccessor;
import elocindev.tierify.forge.screen.ReforgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class OpenReforgeFromAnvilC2S {

    public OpenReforgeFromAnvilC2S() {}

    public static void encode(OpenReforgeFromAnvilC2S msg, FriendlyByteBuf buf) {
    // no payload
    }

    public static OpenReforgeFromAnvilC2S decode(FriendlyByteBuf buf) {
        return new OpenReforgeFromAnvilC2S();
    }

    public static void handle(OpenReforgeFromAnvilC2S msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer sp = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (sp == null) return;
            if (!(sp.containerMenu instanceof AnvilMenu anvil)) return;

            ContainerLevelAccess access = ((ItemCombinerMenuAccessor) anvil).tierify$getAccess();

            // Extract pos from ContainerLevelAccess
            AtomicReference<BlockPos> posRef = new AtomicReference<>(BlockPos.ZERO);
            access.execute((level, pos) -> posRef.set(pos));

            NetworkHooks.openScreen(
                    sp,
                    new SimpleMenuProvider(
                            (id, inv, player) -> new ReforgeMenu(id, inv, access),
                            Component.translatable("container.reforge")
                    ),
                    (FriendlyByteBuf buf) -> buf.writeBlockPos(posRef.get())
            );
        });
        ctx.setPacketHandled(true);
    }
}
