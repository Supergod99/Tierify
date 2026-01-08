package elocindev.tierify.forge.network.c2s;

import elocindev.tierify.forge.screen.ReforgeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TryReforgeC2S() {
    public static void encode(TryReforgeC2S msg, FriendlyByteBuf buf) {}
    public static TryReforgeC2S decode(FriendlyByteBuf buf) { return new TryReforgeC2S(); }

    public static void handle(TryReforgeC2S msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        if (sp == null) return;

        c.enqueueWork(() -> {
            if (sp.containerMenu instanceof ReforgeMenu menu) {
                menu.doReforge(sp);
            }
        });

        c.setPacketHandled(true);
    }
}
