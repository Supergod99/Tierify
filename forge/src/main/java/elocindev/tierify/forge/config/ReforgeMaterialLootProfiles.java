package elocindev.tierify.forge.config;

import elocindev.tierify.forge.ForgeTieredAttributeSubscriber;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReforgeMaterialLootProfiles {

    public record Entry(int[] weights) {}

    private static final Map<ResourceLocation, Entry> EXACT = new HashMap<>();
    private static final Map<String, Entry> NAMESPACE_WILDCARD = new HashMap<>();
    private static Entry GLOBAL_WILDCARD = null;

    private ReforgeMaterialLootProfiles() {}

    public static void reload() {
        EXACT.clear();
        NAMESPACE_WILDCARD.clear();
        GLOBAL_WILDCARD = null;

        String fileName = ForgeTierifyConfig.reforgeMaterialLootProfilesFile();
        if (fileName == null || fileName.isBlank()) {
            fileName = "echelon-reforge-material-profiles.txt";
        }

        Path path = FMLPaths.CONFIGDIR.get().resolve(fileName);
        ensureExists(path);

        final List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }

        for (String raw : lines) {
            String line = stripComments(raw).trim();
            if (line.isEmpty()) continue;

            int eq = line.indexOf('=');
            if (eq <= 0 || eq >= line.length() - 1) continue;

            String left = line.substring(0, eq).trim();
            String right = line.substring(eq + 1).trim();

            int[] weights = ForgeTieredAttributeSubscriber.parseWeightProfile(right);
            if (weights == null || weights.length != 6) continue;

            Entry entry = new Entry(weights);

            if (left.equals("*")) {
                GLOBAL_WILDCARD = entry;
                continue;
            }

            if (left.endsWith(":*")) {
                String ns = left.substring(0, left.length() - 2);
                if (!ns.isEmpty()) NAMESPACE_WILDCARD.put(ns, entry);
                continue;
            }

            ResourceLocation id = ResourceLocation.tryParse(left);
            if (id != null) {
                EXACT.put(id, entry);
            }
        }
    }

    public static Entry get(ResourceLocation dimensionId) {
        if (dimensionId == null) return null;

        Entry exact = EXACT.get(dimensionId);
        if (exact != null) return exact;

        Entry ns = NAMESPACE_WILDCARD.get(dimensionId.getNamespace());
        if (ns != null) return ns;

        return GLOBAL_WILDCARD;
    }

    private static void ensureExists(Path path) {
        if (Files.exists(path)) return;

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    "# Echelon reforge material loot profiles\n" +
                    "# dimension_id=weights\n" +
                    "# weights: 6 ints (Common..Mythic) OR preset overworld|nether|end|global\n" +
                    "# Wildcards:\n" +
                    "#   *=weights\n" +
                    "#   modid:*=weights\n\n" +
                    "minecraft:overworld=100,10,1,0,0,0\n" +
                    "minecraft:the_nether=10,100,10,3,1,0\n" +
                    "minecraft:the_end=0,10,100,10,3,1\n",
                    StandardCharsets.UTF_8
            );
        } catch (IOException ignored) {
        }
    }

    private static String stripComments(String s) {
        int hash = s.indexOf('#');
        int slashes = s.indexOf("//");

        int cut = -1;
        if (hash >= 0) cut = hash;
        if (slashes >= 0) cut = (cut < 0) ? slashes : Math.min(cut, slashes);

        return (cut >= 0) ? s.substring(0, cut) : s;
    }
}
