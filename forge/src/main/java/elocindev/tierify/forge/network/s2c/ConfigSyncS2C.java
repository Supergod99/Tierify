package elocindev.tierify.forge.network.s2c;

import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ConfigSyncS2C(ForgeTierifyConfig.SyncedConfig config) {

    public static void encode(ConfigSyncS2C msg, FriendlyByteBuf buf) {
        ForgeTierifyConfig.SyncedConfig c = msg.config;
        buf.writeBoolean(c.enableArmorSetBonuses());
        buf.writeFloat(c.armorSetBonusMultiplier());
        buf.writeFloat(c.armorSetPerfectBonusPercent());
        buf.writeDouble(c.perfectRollChance());
        buf.writeBoolean(c.allowReforgingDamaged());
        buf.writeBoolean(c.lootContainerModifier());
        buf.writeFloat(c.lootContainerModifierChance());
        buf.writeBoolean(c.treasureBagDropModifier());
        buf.writeUtf(c.treasureBagProfilesFile());
        buf.writeBoolean(c.entityItemModifier());
        buf.writeBoolean(c.entityLootDropModifier());
        buf.writeUtf(c.entityLootDropProfilesFile());
        buf.writeBoolean(c.entityEquipmentDropModifier());
        buf.writeVarInt(c.entityTier1Weight());
        buf.writeVarInt(c.entityTier2Weight());
        buf.writeVarInt(c.entityTier3Weight());
        buf.writeVarInt(c.entityTier4Weight());
        buf.writeVarInt(c.entityTier5Weight());
        buf.writeVarInt(c.entityTier6Weight());
        buf.writeBoolean(c.useDimensionTierWeights());
        buf.writeBoolean(c.dimensionTierWeightsZeroMeansNoModifier());
        buf.writeVarInt(c.overworldTier1Weight());
        buf.writeVarInt(c.overworldTier2Weight());
        buf.writeVarInt(c.overworldTier3Weight());
        buf.writeVarInt(c.overworldTier4Weight());
        buf.writeVarInt(c.overworldTier5Weight());
        buf.writeVarInt(c.overworldTier6Weight());
        buf.writeVarInt(c.netherTier1Weight());
        buf.writeVarInt(c.netherTier2Weight());
        buf.writeVarInt(c.netherTier3Weight());
        buf.writeVarInt(c.netherTier4Weight());
        buf.writeVarInt(c.netherTier5Weight());
        buf.writeVarInt(c.netherTier6Weight());
        buf.writeVarInt(c.endTier1Weight());
        buf.writeVarInt(c.endTier2Weight());
        buf.writeVarInt(c.endTier3Weight());
        buf.writeVarInt(c.endTier4Weight());
        buf.writeVarInt(c.endTier5Weight());
        buf.writeVarInt(c.endTier6Weight());
        writeStringList(buf, c.moddedDimensionTierWeightOverrides());
        buf.writeBoolean(c.craftingModifier());
        buf.writeBoolean(c.merchantModifier());
        buf.writeFloat(c.reforgeModifier());
        buf.writeFloat(c.levelzReforgeModifier());
        buf.writeFloat(c.luckReforgeModifier());
        buf.writeBoolean(c.showReforgingTab());
        buf.writeVarInt(c.xIconPosition());
        buf.writeVarInt(c.yIconPosition());
        buf.writeBoolean(c.tieredTooltip());
        buf.writeBoolean(c.showPlatesOnName());
        buf.writeBoolean(c.centerName());
        writeStringList(buf, c.tier1Qualities());
        writeStringList(buf, c.tier2Qualities());
        writeStringList(buf, c.tier3Qualities());
        writeStringList(buf, c.tier4Qualities());
        writeStringList(buf, c.tier5Qualities());
        writeStringList(buf, c.tier6Qualities());
    }

    public static ConfigSyncS2C decode(FriendlyByteBuf buf) {
        boolean enableArmorSetBonuses = buf.readBoolean();
        float armorSetBonusMultiplier = buf.readFloat();
        float armorSetPerfectBonusPercent = buf.readFloat();
        double perfectRollChance = buf.readDouble();
        boolean allowReforgingDamaged = buf.readBoolean();
        boolean lootContainerModifier = buf.readBoolean();
        float lootContainerModifierChance = buf.readFloat();
        boolean treasureBagDropModifier = buf.readBoolean();
        String treasureBagProfilesFile = buf.readUtf();
        boolean entityItemModifier = buf.readBoolean();
        boolean entityLootDropModifier = buf.readBoolean();
        String entityLootDropProfilesFile = buf.readUtf();
        boolean entityEquipmentDropModifier = buf.readBoolean();
        int entityTier1Weight = buf.readVarInt();
        int entityTier2Weight = buf.readVarInt();
        int entityTier3Weight = buf.readVarInt();
        int entityTier4Weight = buf.readVarInt();
        int entityTier5Weight = buf.readVarInt();
        int entityTier6Weight = buf.readVarInt();
        boolean useDimensionTierWeights = buf.readBoolean();
        boolean dimensionTierWeightsZeroMeansNoModifier = buf.readBoolean();
        int overworldTier1Weight = buf.readVarInt();
        int overworldTier2Weight = buf.readVarInt();
        int overworldTier3Weight = buf.readVarInt();
        int overworldTier4Weight = buf.readVarInt();
        int overworldTier5Weight = buf.readVarInt();
        int overworldTier6Weight = buf.readVarInt();
        int netherTier1Weight = buf.readVarInt();
        int netherTier2Weight = buf.readVarInt();
        int netherTier3Weight = buf.readVarInt();
        int netherTier4Weight = buf.readVarInt();
        int netherTier5Weight = buf.readVarInt();
        int netherTier6Weight = buf.readVarInt();
        int endTier1Weight = buf.readVarInt();
        int endTier2Weight = buf.readVarInt();
        int endTier3Weight = buf.readVarInt();
        int endTier4Weight = buf.readVarInt();
        int endTier5Weight = buf.readVarInt();
        int endTier6Weight = buf.readVarInt();
        List<String> moddedDimensionTierWeightOverrides = readStringList(buf);
        boolean craftingModifier = buf.readBoolean();
        boolean merchantModifier = buf.readBoolean();
        float reforgeModifier = buf.readFloat();
        float levelzReforgeModifier = buf.readFloat();
        float luckReforgeModifier = buf.readFloat();
        boolean showReforgingTab = buf.readBoolean();
        int xIconPosition = buf.readVarInt();
        int yIconPosition = buf.readVarInt();
        boolean tieredTooltip = buf.readBoolean();
        boolean showPlatesOnName = buf.readBoolean();
        boolean centerName = buf.readBoolean();
        List<String> tier1Qualities = readStringList(buf);
        List<String> tier2Qualities = readStringList(buf);
        List<String> tier3Qualities = readStringList(buf);
        List<String> tier4Qualities = readStringList(buf);
        List<String> tier5Qualities = readStringList(buf);
        List<String> tier6Qualities = readStringList(buf);

        ForgeTierifyConfig.SyncedConfig config = new ForgeTierifyConfig.SyncedConfig(
                enableArmorSetBonuses,
                armorSetBonusMultiplier,
                armorSetPerfectBonusPercent,
                perfectRollChance,
                allowReforgingDamaged,
                lootContainerModifier,
                lootContainerModifierChance,
                treasureBagDropModifier,
                treasureBagProfilesFile,
                entityItemModifier,
                entityLootDropModifier,
                entityLootDropProfilesFile,
                entityEquipmentDropModifier,
                entityTier1Weight,
                entityTier2Weight,
                entityTier3Weight,
                entityTier4Weight,
                entityTier5Weight,
                entityTier6Weight,
                useDimensionTierWeights,
                dimensionTierWeightsZeroMeansNoModifier,
                overworldTier1Weight,
                overworldTier2Weight,
                overworldTier3Weight,
                overworldTier4Weight,
                overworldTier5Weight,
                overworldTier6Weight,
                netherTier1Weight,
                netherTier2Weight,
                netherTier3Weight,
                netherTier4Weight,
                netherTier5Weight,
                netherTier6Weight,
                endTier1Weight,
                endTier2Weight,
                endTier3Weight,
                endTier4Weight,
                endTier5Weight,
                endTier6Weight,
                moddedDimensionTierWeightOverrides,
                craftingModifier,
                merchantModifier,
                reforgeModifier,
                levelzReforgeModifier,
                luckReforgeModifier,
                showReforgingTab,
                xIconPosition,
                yIconPosition,
                tieredTooltip,
                showPlatesOnName,
                centerName,
                tier1Qualities,
                tier2Qualities,
                tier3Qualities,
                tier4Qualities,
                tier5Qualities,
                tier6Qualities
        );
        return new ConfigSyncS2C(config);
    }

    public static void handle(ConfigSyncS2C msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> ForgeTierifyConfig.applySyncedConfig(msg.config));
        c.setPacketHandled(true);
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> list) {
        List<String> out = list == null ? List.of() : list;
        buf.writeVarInt(out.size());
        for (String s : out) {
            buf.writeUtf(s);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(buf.readUtf());
        }
        return List.copyOf(out);
    }
}
