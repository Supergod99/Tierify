package elocindev.tierify.forge.network.s2c;

import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record AttributeSyncS2C(Map<ResourceLocation, String> jsonById) {

    public static void encode(AttributeSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.jsonById.size());
        for (Map.Entry<ResourceLocation, String> e : msg.jsonById.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            buf.writeUtf(e.getValue());
        }
    }

    public static AttributeSyncS2C decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, String> out = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            String json = buf.readUtf();
            out.put(id, json);
        }
        return new AttributeSyncS2C(out);
    }

    public static void handle(AttributeSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> ForgeTieredAttributeSubscriber.applySyncedAttributes(msg.jsonById));
        c.setPacketHandled(true);
    }
}
