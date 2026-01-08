package elocindev.tierify.forge.network.s2c;

import elocindev.tierify.forge.reforge.ForgeReforgeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record ReforgeItemsSyncS2C(Map<ResourceLocation, List<ResourceLocation>> baseItemsByTarget) {

    public static void encode(ReforgeItemsSyncS2C msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.baseItemsByTarget.size());
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> e : msg.baseItemsByTarget.entrySet()) {
            buf.writeResourceLocation(e.getKey());
            List<ResourceLocation> base = e.getValue();
            buf.writeVarInt(base.size());
            for (ResourceLocation id : base) {
                buf.writeResourceLocation(id);
            }
        }
    }

    public static ReforgeItemsSyncS2C decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation target = buf.readResourceLocation();
            int baseSize = buf.readVarInt();
            List<ResourceLocation> base = new ArrayList<>(baseSize);
            for (int j = 0; j < baseSize; j++) {
                base.add(buf.readResourceLocation());
            }
            out.put(target, base);
        }
        return new ReforgeItemsSyncS2C(out);
    }

    public static void handle(ReforgeItemsSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> ForgeReforgeData.applySync(msg.baseItemsByTarget));
        c.setPacketHandled(true);
    }
}
