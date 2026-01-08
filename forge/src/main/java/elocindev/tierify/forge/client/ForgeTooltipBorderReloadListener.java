package elocindev.tierify.forge.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "tiered", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeTooltipBorderReloadListener {

    private ForgeTooltipBorderReloadListener() {}

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new TooltipBordersJsonLoader());
    }

    private static final class TooltipBordersJsonLoader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new GsonBuilder().create();

        private TooltipBordersJsonLoader() {
            // Loads all json under: assets/<ns>/tooltips/*.json
            // Your file should be: assets/tiered/tooltips/tooltip_borders.json
            super(GSON, "tooltips");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map,
                             ResourceManager resourceManager,
                             ProfilerFiller profiler) {

            List<TierifyTooltipBorderRendererForge.Template> loaded = new ArrayList<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                ResourceLocation id = entry.getKey();

                // Only consume tiered:tooltip_borders (from tooltips/tooltip_borders.json)
                if (!"tiered".equals(id.getNamespace()) || !"tooltip_borders".equals(id.getPath())) {
                    continue;
                }

                JsonElement elem = entry.getValue();
                if (!elem.isJsonObject()) continue;

                JsonObject root = elem.getAsJsonObject();
                if (!root.has("tooltips")) continue;

                JsonArray tooltips = GsonHelper.getAsJsonArray(root, "tooltips");
                for (JsonElement t : tooltips) {
                    if (!t.isJsonObject()) continue;
                    JsonObject o = t.getAsJsonObject();

                    int index = GsonHelper.getAsInt(o, "index", 0);
                    String textureStr = GsonHelper.getAsString(o, "texture", "");

                    int start = parseHexColor(GsonHelper.getAsString(o, "start_border_gradient", "FFFFFFFF"));
                    int end = parseHexColor(GsonHelper.getAsString(o, "end_border_gradient", "FFFFFFFF"));
                    int bg = parseHexColor(GsonHelper.getAsString(o, "background_gradient", "F0100010"));

                    List<String> deciders = new ArrayList<>();
                    if (o.has("decider")) {
                        JsonArray deciderArr = GsonHelper.getAsJsonArray(o, "decider");
                        for (JsonElement d : deciderArr) {
                            String s = GsonHelper.convertToString(d, "decider");
                            if (s != null && !s.isEmpty()) {
                                if ("tiered:perfect_border".equals(s)) {
                                    deciders.add("tiered:perfect");
                                } else {
                                    deciders.add(s);
                                }
                            }
                        }
                    }

                    // Avoid deprecated ResourceLocation(String,String) warnings: use tryParse.
                    ResourceLocation texture = null;
                    if (textureStr != null && !textureStr.isEmpty()) {
                        texture = ResourceLocation.tryParse("tiered:textures/gui/" + textureStr + ".png");
                    }

                    loaded.add(new TierifyTooltipBorderRendererForge.Template(
                            index, texture, start, end, bg, deciders
                    ));
                }
            }

            loaded.sort(Comparator.comparingInt(TierifyTooltipBorderRendererForge.Template::index));
            TierifyTooltipBorderRendererForge.setTemplates(loaded);
        }

        private static int parseHexColor(String s) {
            if (s == null) return 0;
            String v = s.trim();
            if (v.startsWith("#")) v = v.substring(1);
            if (v.startsWith("0x") || v.startsWith("0X")) v = v.substring(2);
            if (v.isEmpty()) return 0;

            long parsed = Long.parseLong(v, 16);

            // If user provided RRGGBB, upgrade to AARRGGBB with alpha=FF.
            if (v.length() <= 6) parsed |= 0xFF000000L;

            return (int) parsed;
        }
    }
}
