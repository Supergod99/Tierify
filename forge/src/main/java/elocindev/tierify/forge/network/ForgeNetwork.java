package elocindev.tierify.forge.network;

import elocindev.tierify.TierifyCommon;
import elocindev.tierify.forge.network.c2s.OpenAnvilFromReforgeC2S;
import elocindev.tierify.forge.network.c2s.OpenReforgeFromAnvilC2S;
import elocindev.tierify.forge.network.c2s.TryReforgeC2S;
import elocindev.tierify.forge.network.s2c.AttributeSyncS2C;
import elocindev.tierify.forge.network.s2c.ConfigSyncS2C;
import elocindev.tierify.forge.network.s2c.ReforgeItemsSyncS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(TierifyCommon.MODID, "net"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals
    );

    public static void init() {
        int id = 0;

        CHANNEL.messageBuilder(OpenReforgeFromAnvilC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenReforgeFromAnvilC2S::encode)
                .decoder(OpenReforgeFromAnvilC2S::decode)
                .consumerMainThread(OpenReforgeFromAnvilC2S::handle)
                .add();

        CHANNEL.messageBuilder(OpenAnvilFromReforgeC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OpenAnvilFromReforgeC2S::encode)
                .decoder(OpenAnvilFromReforgeC2S::decode)
                .consumerMainThread(OpenAnvilFromReforgeC2S::handle)
                .add();

        CHANNEL.messageBuilder(TryReforgeC2S.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TryReforgeC2S::encode)
                .decoder(TryReforgeC2S::decode)
                .consumerMainThread(TryReforgeC2S::handle)
                .add();

        CHANNEL.messageBuilder(AttributeSyncS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AttributeSyncS2C::encode)
                .decoder(AttributeSyncS2C::decode)
                .consumerMainThread(AttributeSyncS2C::handle)
                .add();

        CHANNEL.messageBuilder(ReforgeItemsSyncS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ReforgeItemsSyncS2C::encode)
                .decoder(ReforgeItemsSyncS2C::decode)
                .consumerMainThread(ReforgeItemsSyncS2C::handle)
                .add();

        CHANNEL.messageBuilder(ConfigSyncS2C.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ConfigSyncS2C::encode)
                .decoder(ConfigSyncS2C::decode)
                .consumerMainThread(ConfigSyncS2C::handle)
                .add();
    }

    private ForgeNetwork() {}
}
