package elocindev.tierify.forge.reforge;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public final class ForgeReforgeData {
    // item -> allowed base items (if present); if absent/empty, use tag fallback (reforge_base_item)
    private static final Map<Item, Set<Item>> BASE_ITEMS_BY_TARGET = new HashMap<>();

    public static Set<Item> getBaseItemsForTarget(Item target) {
        return BASE_ITEMS_BY_TARGET.getOrDefault(target, Collections.emptySet());
    }

    public static Map<ResourceLocation, List<ResourceLocation>> getSyncPayload() {
        Map<ResourceLocation, List<ResourceLocation>> out = new HashMap<>();

        for (Map.Entry<Item, Set<Item>> entry : BASE_ITEMS_BY_TARGET.entrySet()) {
            ResourceLocation targetId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (targetId == null) continue;

            List<ResourceLocation> baseIds = new ArrayList<>();
            for (Item base : entry.getValue()) {
                ResourceLocation baseId = ForgeRegistries.ITEMS.getKey(base);
                if (baseId != null) baseIds.add(baseId);
            }

            out.put(targetId, baseIds);
        }

        return out;
    }

    public static void applySync(Map<ResourceLocation, List<ResourceLocation>> payload) {
        BASE_ITEMS_BY_TARGET.clear();
        if (payload == null) return;

        for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : payload.entrySet()) {
            Item target = ForgeRegistries.ITEMS.getValue(entry.getKey());
            if (target == null) continue;

            Set<Item> base = new HashSet<>();
            List<ResourceLocation> baseIds = entry.getValue();
            if (baseIds != null) {
                for (ResourceLocation baseId : baseIds) {
                    Item item = ForgeRegistries.ITEMS.getValue(baseId);
                    if (item != null) base.add(item);
                }
            }

            BASE_ITEMS_BY_TARGET.put(target, base);
        }
    }

    public static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        public Loader() {
            super(GSON, "reforge_items"); // reads data/*/reforge_items/*.json
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager rm, ProfilerFiller profiler) {
            BASE_ITEMS_BY_TARGET.clear();

            for (Map.Entry<ResourceLocation, JsonElement> e : object.entrySet()) {
                JsonObject root = e.getValue().getAsJsonObject();

                // Fabric format: {"items":[...], "base":[...]} :contentReference[oaicite:7]{index=7}
                Set<Item> items = parseItemList(root.getAsJsonArray("items"));
                Set<Item> base = parseItemList(root.getAsJsonArray("base"));

                for (Item it : items) {
                    BASE_ITEMS_BY_TARGET.put(it, base);
                }
            }
        }

        private static Set<Item> parseItemList(JsonArray arr) {
            Set<Item> out = new HashSet<>();
            if (arr == null) return out;

            for (JsonElement el : arr) {
                ResourceLocation id = ResourceLocation.tryParse(el.getAsString());
                if (id == null) continue;
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null) out.add(item);
            }
            return out;
        }
    }

    private ForgeReforgeData() {}
}
