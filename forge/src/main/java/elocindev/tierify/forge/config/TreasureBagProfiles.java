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

public final class TreasureBagProfiles {

    public record Entry(float chance, int[] weights) {}

    private static final Map<ResourceLocation, Entry> EXACT = new HashMap<>();
    private static final Map<String, Entry> NAMESPACE_WILDCARD = new HashMap<>();
    private static Entry GLOBAL_WILDCARD = null;

    private TreasureBagProfiles() {}

    public static void reload() {
        EXACT.clear();
        NAMESPACE_WILDCARD.clear();
        GLOBAL_WILDCARD = null;

        String fileName = ForgeTierifyConfig.treasureBagProfilesFile();
        if (fileName == null || fileName.isBlank()) {
            fileName = "echelon-treasure-bag-profiles.txt";
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

            String[] parts = right.split("\\|", 2);
            if (parts.length != 2) continue;

            Float chance = parseChance(parts[0].trim());
            int[] weights = ForgeTieredAttributeSubscriber.parseWeightProfile(parts[1].trim());
            if (chance == null || weights == null) continue;

            Entry entry = new Entry(chance, weights);

            if (left.equals("*")) {
                GLOBAL_WILDCARD = entry;
                continue;
            }

            if (left.endsWith(":*")) {
                String ns = left.substring(0, left.length() - 2);
                if (!ns.isEmpty()) NAMESPACE_WILDCARD.put(ns, entry);
                continue;
            }

            try {
                EXACT.put(new ResourceLocation(left), entry);
            } catch (Exception ignored) {
            }
        }
    }

    public static Entry get(ResourceLocation bagItemId) {
        if (bagItemId == null) return null;

        Entry exact = EXACT.get(bagItemId);
        if (exact != null) return exact;

        Entry ns = NAMESPACE_WILDCARD.get(bagItemId.getNamespace());
        if (ns != null) return ns;

        return GLOBAL_WILDCARD;
    }

    private static void ensureExists(Path path) {
        if (Files.exists(path)) return;

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    "# Echelon treasure bag profiles\n" +
                    "# item_id=chance|weights\n" +
                    "# weights: 6 ints (Common..Mythic) OR preset overworld|nether|end|global\n" +
                    "# Wildcards:\n" +
                    "#   *=chance|weights\n" +
                    "#   modid:*=chance|weights\n\n" +
                    "armageddon_mod:iron_colossus_treasure_bag=1.0|5,1,0,0,0,0\n" +
                    "armageddon_mod:arion_treasurebag=1.0|5,1,0,0,0,0\n" +
                    "armageddon_mod:eldorath_treasure_bag=1.0|5,1,0,0,0,0\n" +
                    "armageddon_mod:zoranth_treasure_bag=1.0|3,5,1,0,0,0\n" +
                    "armageddon_mod:ender_dragon_treasurebag=0.5|3,5,3,1,0,0\n" +
                    "armageddon_mod:elvenite_paladin_treasure_bag=1.0|1,3,5,3,1,0\n" +
                    "armageddon_mod:vaedric_treasure_bag=1.0|1,3,5,3,1,0\n" +
                    "armageddon_mod:zoranth_newborn_of_the_zenith_treasure_bag=1.0|1,3,5,3,1,0\n" +
                    "armageddon_mod:nyxaris_the_veil_of_oblivion_treasure_bag=1.0|3,5,7,5,3,1\n",
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

    private static Float parseChance(String s) {
        try {
            float v = Float.parseFloat(s);
            if (v < 0.0f) v = 0.0f;
            if (v > 1.0f) v = 1.0f;
            return v;
        } catch (Exception e) {
            return null;
        }
    }
}
