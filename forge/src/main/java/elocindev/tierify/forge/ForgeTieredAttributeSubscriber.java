package elocindev.tierify.forge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;
import elocindev.tierify.TierifyCommon;
import elocindev.tierify.TierifyConstants;
import elocindev.tierify.forge.config.ForgeTierifyConfig;
import elocindev.tierify.forge.network.ForgeNetwork;
import elocindev.tierify.forge.network.s2c.AttributeSyncS2C;
import elocindev.tierify.forge.network.s2c.ReforgeItemsSyncS2C;
import elocindev.tierify.forge.reforge.ForgeReforgeData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TierifyCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeTieredAttributeSubscriber {

    private static final Logger LOGGER = LogManager.getLogger("tiered");
    private static final Gson GSON = new GsonBuilder().create();
    private static final TierAttributesReloader RELOADER = new TierAttributesReloader();

    private static final TagKey<Item> MAIN_OFFHAND_ITEM_TAG = TagKey.create(
            Registries.ITEM,
            Objects.requireNonNull(ResourceLocation.tryParse("tiered:main_offhand_item"))
    );

    private static final ResourceLocation DURABLE_ID =
            new ResourceLocation(TierifyCommon.MODID, "generic.durable");

    private ForgeTieredAttributeSubscriber() {}

    @Nullable
    public static ResourceLocation pickRandomTier(ItemStack target, List<String> qualities, RandomSource rand) {
        return RELOADER.pickRandomTier(target, qualities, rand);
    }

    @Nullable
    public static ResourceLocation pickRandomTierForReforge(ItemStack target, List<String> qualities, RandomSource rand, @Nullable Player player) {
        boolean applyModifiers = qualities == null || qualities.isEmpty();
        return RELOADER.pickRandomTierWithModifiers(target, qualities, rand, player, applyModifiers);
    }

    public static boolean hasAnyValidTier(ItemStack target) {
        return RELOADER.hasAnyValidTier(target);
    }

    public static void applyRandomTierIfAbsent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY) != null) return;

        ResourceLocation tierId = RELOADER.pickRandomTierNoBonus(stack, null, RandomSource.create());
        if (tierId != null) {
            RELOADER.applyTier(stack, tierId, false);
        }
    }

    public static void clearTieredData(ItemStack stack) {
        RELOADER.clearTieredData(stack);
    }

    public static boolean applyTier(ItemStack stack, ResourceLocation tierId, boolean perfect) {
        return RELOADER.applyTier(stack, tierId, perfect);
    }

    public static void applySyncedAttributes(Map<ResourceLocation, String> jsonById) {
        RELOADER.applySyncedAttributes(jsonById);
    }

    public static void updateItemStackNbt(ServerPlayer player) {
        if (player == null) return;
        updateItemStackNbt(player.getInventory());
    }

    public static void updateItemStackNbt(Inventory inventory) {
        if (inventory == null) return;

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            CompoundTag tierTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (tierTag == null) continue;

            ResourceLocation tierId = ResourceLocation.tryParse(tierTag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY));
            TierData data = tierId == null ? null : RELOADER.getTier(tierId);

            if (data == null || !data.isValidFor(stack)) {
                RELOADER.clearTieredData(stack);
                ResourceLocation newTier = RELOADER.pickRandomTierNoBonus(stack, null, RandomSource.create());
                if (newTier != null) {
                    RELOADER.applyTier(stack, newTier, false);
                }
                inventory.setItem(i, stack);
                continue;
            }

            data.applyExtraNbt(stack.getOrCreateTag());

            if (stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY) == null) {
                stack.getOrCreateTagElement(TierifyConstants.NBT_SUBTAG_KEY)
                        .putString(TierifyConstants.NBT_SUBTAG_DATA_KEY, tierId.toString());
            }

            inventory.setItem(i, stack);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(RELOADER);
        LOGGER.info("[Tierify/Forge] Registered item_attributes reload listener");
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        Map<ResourceLocation, String> attributes = RELOADER.getSyncPayload();
        Map<ResourceLocation, List<ResourceLocation>> reforge = ForgeReforgeData.getSyncPayload();

        if (event.getPlayer() != null) {
            updateItemStackNbt(event.getPlayer());
            sendSync(event.getPlayer(), attributes, reforge);
            return;
        }

        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            updateItemStackNbt(player);
            sendSync(player, attributes, reforge);
        }
    }

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        EquipmentSlot slot = event.getSlotType();
        if (stack == null || stack.isEmpty()) return;

        CompoundTag tierTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
        if (tierTag == null) return;

        String tierStr = tierTag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY);
        if (tierStr == null || tierStr.isEmpty()) return;

        ResourceLocation tierId = ResourceLocation.tryParse(tierStr);
        if (tierId == null) return;

        TierData tier = RELOADER.getTier(tierId);
        if (tier == null) return;

        boolean isPerfect = tierTag.getBoolean("Perfect");

        UUID tierUuid = TierifyConstants.MODIFIERS[armorStandSlotId(slot)];

        for (TierAttributeEntry entry : tier.attributes) {
            if (!entry.appliesTo(stack, slot)) continue;
            if (isPerfect && entry.amount < 0.0D) continue;

            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(entry.attributeId);
            if (attr == null) continue;

            AttributeModifier mod = new AttributeModifier(
                    tierUuid,
                    entry.name,
                    entry.amount,
                    entry.operation
            );

            event.addModifier(attr, mod);
        }
    }

    private static int armorStandSlotId(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> 0;
            case OFFHAND -> 1;
            case FEET -> 2;
            case LEGS -> 3;
            case CHEST -> 4;
            case HEAD -> 5;
        };
    }

    private static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        Item item = stack.getItem();

        if (item instanceof net.minecraft.world.item.Equipable equipable) {
            return equipable.getEquipmentSlot() == slot;
        }

        if (item instanceof ShieldItem
                || item instanceof ProjectileWeaponItem
                || stack.is(MAIN_OFFHAND_ITEM_TAG)) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }

        return slot == EquipmentSlot.MAINHAND;
    }

    private static void applyItemBorderColors(ItemStack stack, int color) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTagElement("itemborders_colors");
        String value = String.valueOf(color);
        tag.putString("top", value);
        tag.putString("bottom", value);
    }

    private static void sendSync(ServerPlayer player,
                                 Map<ResourceLocation, String> attributes,
                                 Map<ResourceLocation, List<ResourceLocation>> reforge) {
        if (player == null) return;
        ForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AttributeSyncS2C(attributes));
        ForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ReforgeItemsSyncS2C(reforge));
    }

    private static final class TierAttributesReloader extends SimpleJsonResourceReloadListener {

        private final Map<ResourceLocation, TierData> byId = new HashMap<>();
        private final Map<ResourceLocation, JsonObject> rawById = new HashMap<>();

        private TierAttributesReloader() {
            super(GSON, "item_attributes");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> object,
                             ResourceManager resourceManager,
                             ProfilerFiller profiler) {
            byId.clear();
            rawById.clear();

            int loaded = 0;
            for (Map.Entry<ResourceLocation, JsonElement> e : object.entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject root = e.getValue().getAsJsonObject();

                ResourceLocation tierId = resolveTierId(e.getKey(), root);

                TierData parsed = TierData.parse(root);
                if (parsed != null) {
                    byId.put(tierId, parsed);
                    rawById.put(tierId, root.deepCopy());
                    loaded++;
                }
            }

            LOGGER.info("[Tierify/Forge] Loaded {} item_attributes tiers", loaded);
        }

        public Map<ResourceLocation, String> getSyncPayload() {
            Map<ResourceLocation, String> out = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonObject> e : rawById.entrySet()) {
                out.put(e.getKey(), GSON.toJson(e.getValue()));
            }
            return out;
        }

        public void applySyncedAttributes(Map<ResourceLocation, String> jsonById) {
            byId.clear();
            rawById.clear();
            if (jsonById == null) return;

            for (Map.Entry<ResourceLocation, String> e : jsonById.entrySet()) {
                JsonElement el = JsonParser.parseString(e.getValue());
                if (!el.isJsonObject()) continue;

                JsonObject root = el.getAsJsonObject();
                TierData parsed = TierData.parse(root);
                if (parsed != null) {
                    byId.put(e.getKey(), parsed);
                    rawById.put(e.getKey(), root);
                }
            }
        }

        @Nullable
        public TierData getTier(ResourceLocation id) {
            return byId.get(id);
        }

        public boolean hasAnyValidTier(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;

            for (TierData data : byId.values()) {
                if (data.weight > 0 && data.isValidFor(stack)) return true;
            }

            return false;
        }

        @Nullable
        public ResourceLocation pickRandomTier(ItemStack target, List<String> qualities, RandomSource rand) {
            return pickRandomTierWithModifiers(target, qualities, rand, null, false);
        }

        @Nullable
        public ResourceLocation pickRandomTierWithModifiers(ItemStack target,
                                                            List<String> qualities,
                                                            RandomSource rand,
                                                            @Nullable Player player,
                                                            boolean applyReforgeModifiers) {
            if (target == null || target.isEmpty()) return null;

            List<Map.Entry<ResourceLocation, TierData>> candidates = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();

            for (Map.Entry<ResourceLocation, TierData> e : byId.entrySet()) {
                TierData data = e.getValue();

                if (qualities != null && !qualities.isEmpty() && !matchesQuality(e.getKey(), qualities)) continue;
                if (!data.isValidFor(target)) continue;

                int w = data.weight + 1;
                if (w <= 0) continue;

                candidates.add(e);
                weights.add(w);
            }

            if (candidates.isEmpty()) return null;

            if (applyReforgeModifiers) {
                applyReforgeWeightModifiers(weights, player);
            }

            int total = 0;
            for (Integer weight : weights) {
                if (weight != null && weight > 0) total += weight;
            }

            if (total <= 0) return null;

            int roll = rand.nextInt(total);
            int acc = 0;

            for (int i = 0; i < candidates.size(); i++) {
                int w = weights.get(i);
                if (w <= 0) continue;
                acc += w;
                if (roll < acc) return candidates.get(i).getKey();
            }

            return candidates.get(candidates.size() - 1).getKey();
        }

        @Nullable
        public ResourceLocation pickRandomTierNoBonus(ItemStack target, List<String> qualities, RandomSource rand) {
            if (target == null || target.isEmpty()) return null;

            List<Map.Entry<ResourceLocation, TierData>> candidates = new ArrayList<>();
            int total = 0;

            for (Map.Entry<ResourceLocation, TierData> e : byId.entrySet()) {
                TierData data = e.getValue();

                if (qualities != null && !qualities.isEmpty() && !matchesQuality(e.getKey(), qualities)) continue;
                if (!data.isValidFor(target)) continue;

                int w = data.weight;
                if (w <= 0) continue;

                candidates.add(e);
                total += w;
            }

            if (candidates.isEmpty() || total <= 0) return null;

            int roll = rand.nextInt(total);
            int acc = 0;

            for (Map.Entry<ResourceLocation, TierData> e : candidates) {
                int w = e.getValue().weight;
                if (w <= 0) continue;
                acc += w;
                if (roll < acc) return e.getKey();
            }

            return candidates.get(candidates.size() - 1).getKey();
        }

        public void clearTieredData(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return;

            CompoundTag tierTag = stack.getTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            if (tierTag == null) return;

            ResourceLocation tierId = ResourceLocation.tryParse(tierTag.getString(TierifyConstants.NBT_SUBTAG_DATA_KEY));
            TierData data = tierId == null ? null : byId.get(tierId);

            CompoundTag root = stack.getTag();
            if (root != null) {
                if (data != null) {
                    data.clearExtraNbt(root);
                } else {
                    root.remove("durable");
                }
            }

            stack.removeTagKey(TierifyConstants.NBT_SUBTAG_KEY);
        }

        public boolean applyTier(ItemStack stack, ResourceLocation tierId, boolean perfect) {
            if (stack == null || stack.isEmpty() || tierId == null) return false;

            TierData data = byId.get(tierId);
            if (data == null) return false;

            CompoundTag tierTag = stack.getOrCreateTagElement(TierifyConstants.NBT_SUBTAG_KEY);
            tierTag.putString(TierifyConstants.NBT_SUBTAG_DATA_KEY, tierId.toString());
            if (perfect) {
                tierTag.putBoolean("Perfect", true);
            } else {
                tierTag.remove("Perfect");
            }

            CompoundTag root = stack.getOrCreateTag();
            data.applyExtraNbt(root);
            if (data.styleColor != null) {
                applyItemBorderColors(stack, data.styleColor);
            }
            return true;
        }

        private static boolean matchesQuality(ResourceLocation id, List<String> qualities) {
            if (qualities == null || qualities.isEmpty()) return true;

            String path = id.toString().toLowerCase(Locale.ROOT);
            for (String q : qualities) {
                if (q == null || q.isBlank()) continue;
                String needle = q.toLowerCase(Locale.ROOT);
                if (path.contains(needle)) return true;
            }

            return false;
        }

        private static ResourceLocation resolveTierId(ResourceLocation fallback, JsonObject root) {
            if (root != null && root.has("id") && root.get("id").isJsonPrimitive()) {
                ResourceLocation id = ResourceLocation.tryParse(root.get("id").getAsString());
                if (id != null) return id;
            }
            return fallback;
        }

        private static void applyReforgeWeightModifiers(List<Integer> weights, @Nullable Player player) {
            if (weights == null || weights.isEmpty()) return;

            if (weights.size() > 2) {
                int maxWeight = Collections.max(weights);
                float modifier = ForgeTierifyConfig.reforgeModifier();
                for (int i = 0; i < weights.size(); i++) {
                    int w = weights.get(i);
                    if (w > maxWeight / 2) {
                        weights.set(i, (int) (w * modifier));
                    }
                }
            }

            if (player != null) {
                int maxWeight = Collections.max(weights);
                float factor = 1.0f - ForgeTierifyConfig.luckReforgeModifier() * player.getLuck();
                for (int i = 0; i < weights.size(); i++) {
                    int w = weights.get(i);
                    if (w > maxWeight / 3) {
                        weights.set(i, (int) (w * factor));
                    }
                }
            }
        }
    }

    private static final class TierData {
        final int weight;
        final List<Verifier> verifiers;
        final List<TierAttributeEntry> attributes;
        final Map<String, JsonElement> nbtValues;
        final Double durableAmount;
        final Integer styleColor;

        private TierData(int weight,
                         List<Verifier> verifiers,
                         List<TierAttributeEntry> attributes,
                         Map<String, JsonElement> nbtValues,
                         Double durableAmount,
                         Integer styleColor) {
            this.weight = weight;
            this.verifiers = verifiers;
            this.attributes = attributes;
            this.nbtValues = nbtValues;
            this.durableAmount = durableAmount;
            this.styleColor = styleColor;
        }

        boolean isValidFor(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return false;
            if (verifiers == null || verifiers.isEmpty()) return false;

            for (Verifier v : verifiers) {
                if (v != null && v.matches(stack)) return true;
            }
            return false;
        }

        void applyExtraNbt(CompoundTag root) {
            if (root == null) return;

            for (Map.Entry<String, JsonElement> entry : nbtValues.entrySet()) {
                JsonElement el = entry.getValue();
                if (!el.isJsonPrimitive()) continue;
                applyPrimitive(root, entry.getKey(), el.getAsJsonPrimitive());
            }

            if (durableAmount != null) {
                putNumber(root, "durable", durableAmount);
            }
        }

        void clearExtraNbt(CompoundTag root) {
            if (root == null) return;

            for (String key : nbtValues.keySet()) {
                if (!"Damage".equals(key)) {
                    root.remove(key);
                }
            }

            if (durableAmount != null) {
                root.remove("durable");
            }
        }

        static TierData parse(JsonObject root) {
            if (root == null || !root.has("attributes") || !root.get("attributes").isJsonArray()) return null;

            int weight = 1;
            if (root.has("weight") && root.get("weight").isJsonPrimitive()) {
                try {
                    weight = root.get("weight").getAsInt();
                } catch (Exception ignored) {
                }
            }

            List<Verifier> verifiers = new ArrayList<>();
            if (root.has("verifiers") && root.get("verifiers").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("verifiers")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject v = el.getAsJsonObject();

                    if (v.has("tag") && v.get("tag").isJsonPrimitive()) {
                        ResourceLocation tagId = ResourceLocation.tryParse(v.get("tag").getAsString());
                        if (tagId != null) {
                            verifiers.add(Verifier.forTag(TagKey.create(Registries.ITEM, tagId)));
                        }
                        continue;
                    }

                    String key = v.has("item") ? "item" : (v.has("id") ? "id" : null);
                    if (key != null && v.get(key).isJsonPrimitive()) {
                        ResourceLocation itemId = ResourceLocation.tryParse(v.get(key).getAsString());
                        if (itemId != null) {
                            Item it = ForgeRegistries.ITEMS.getValue(itemId);
                            if (it != null) verifiers.add(Verifier.forItem(it));
                        }
                    }
                }
            }

            Double durableAmount = null;
            List<TierAttributeEntry> out = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("attributes")) {
                if (!el.isJsonObject()) continue;
                JsonObject a = el.getAsJsonObject();

                String typeRaw = a.has("type") ? a.get("type").getAsString() : null;
                if (typeRaw == null || typeRaw.isEmpty()) continue;

                ResourceLocation attributeId = typeRaw.contains(":")
                        ? ResourceLocation.tryParse(typeRaw)
                        : ResourceLocation.withDefaultNamespace(typeRaw);

                if (attributeId == null) continue;

                JsonObject mod = a.has("modifier") && a.get("modifier").isJsonObject()
                        ? a.getAsJsonObject("modifier")
                        : null;
                if (mod == null) continue;

                String name = mod.has("name") ? mod.get("name").getAsString() : "Tiered Modifier";
                double amount = mod.has("amount") ? mod.get("amount").getAsDouble() : 0.0D;

                String opRaw = mod.has("operation") ? mod.get("operation").getAsString() : "ADDITION";
                AttributeModifier.Operation op;
                try {
                    op = AttributeModifier.Operation.valueOf(opRaw);
                } catch (Exception ignored) {
                    op = AttributeModifier.Operation.ADDITION;
                }

                EnumSet<EquipmentSlot> requiredSlots = parseSlots(a, "required_equipment_slots");
                EnumSet<EquipmentSlot> optionalSlots = parseSlots(a, "optional_equipment_slots");

                out.add(new TierAttributeEntry(attributeId, name, amount, op, requiredSlots, optionalSlots));

                if (DURABLE_ID.equals(attributeId)) {
                    durableAmount = amount;
                }
            }

            Map<String, JsonElement> nbtValues = parseNbtValues(root);

            Integer styleColor = parseStyleColor(root);

            return new TierData(weight, verifiers, out, nbtValues, durableAmount, styleColor);
        }

        @Nullable
        private static EnumSet<EquipmentSlot> parseSlots(JsonObject json, String key) {
            if (!json.has(key) || !json.get(key).isJsonArray()) return null;

            EnumSet<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);
            for (JsonElement s : json.getAsJsonArray(key)) {
                if (!s.isJsonPrimitive()) continue;
                try {
                    slots.add(EquipmentSlot.valueOf(s.getAsString()));
                } catch (Exception ignored) {
                }
            }

            return slots.isEmpty() ? null : slots;
        }

        @Nullable
        private static Integer parseStyleColor(JsonObject root) {
            if (root == null || !root.has("style") || !root.get("style").isJsonObject()) return null;

            JsonObject style = root.getAsJsonObject("style");
            if (!style.has("color") || !style.get("color").isJsonPrimitive()) return null;

            JsonPrimitive color = style.getAsJsonPrimitive("color");
            if (color.isNumber()) {
                return color.getAsInt();
            }

            if (color.isString()) {
                String raw = color.getAsString().trim().toLowerCase(Locale.ROOT);
                if (raw.isEmpty()) return null;

                Integer mapped = namedColor(raw);
                if (mapped != null) return mapped;

                String cleaned = raw;
                if (cleaned.startsWith("#")) cleaned = cleaned.substring(1);
                if (cleaned.startsWith("0x")) cleaned = cleaned.substring(2);
                try {
                    return Integer.parseInt(cleaned, 16);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            return null;
        }

        @Nullable
        private static Integer namedColor(String name) {
            return switch (name) {
                case "black" -> 0x000000;
                case "dark_blue" -> 0x0000AA;
                case "dark_green" -> 0x00AA00;
                case "dark_aqua" -> 0x00AAAA;
                case "dark_red" -> 0xAA0000;
                case "dark_purple" -> 0xAA00AA;
                case "gold" -> 0xFFAA00;
                case "gray" -> 0xAAAAAA;
                case "dark_gray" -> 0x555555;
                case "blue" -> 0x5555FF;
                case "green" -> 0x55FF55;
                case "aqua" -> 0x55FFFF;
                case "red" -> 0xFF5555;
                case "light_purple" -> 0xFF55FF;
                case "yellow" -> 0xFFFF55;
                case "white" -> 0xFFFFFF;
                default -> null;
            };
        }
    }

    private static Map<String, JsonElement> parseNbtValues(JsonObject root) {
        JsonObject nbtObj = null;
        if (root.has("nbtValues") && root.get("nbtValues").isJsonObject()) {
            nbtObj = root.getAsJsonObject("nbtValues");
        } else if (root.has("nbt_values") && root.get("nbt_values").isJsonObject()) {
            nbtObj = root.getAsJsonObject("nbt_values");
        }

        Map<String, JsonElement> out = new HashMap<>();
        if (nbtObj == null) return out;

        for (Map.Entry<String, JsonElement> e : nbtObj.entrySet()) {
            out.put(e.getKey(), e.getValue());
        }

        return out;
    }

    private static void applyPrimitive(CompoundTag tag, String key, JsonPrimitive value) {
        if (value.isString()) {
            tag.putString(key, value.getAsString());
        } else if (value.isBoolean()) {
            tag.putBoolean(key, value.getAsBoolean());
        } else if (value.isNumber()) {
            putNumber(tag, key, value.getAsDouble());
        }
    }

    private static void putNumber(CompoundTag tag, String key, double raw) {
        if (Math.abs(raw) % 1.0D < 0.0001D) {
            tag.putInt(key, (int) Math.round(raw));
        } else {
            tag.putDouble(key, Math.round(raw * 100.0D) / 100.0D);
        }
    }

    private static final class Verifier {
        private final TagKey<Item> tag;
        private final Item item;

        private Verifier(TagKey<Item> tag, Item item) {
            this.tag = tag;
            this.item = item;
        }

        static Verifier forTag(TagKey<Item> tag) {
            return new Verifier(tag, null);
        }

        static Verifier forItem(Item item) {
            return new Verifier(null, item);
        }

        boolean matches(ItemStack stack) {
            if (item != null) return stack.getItem() == item;
            if (tag != null) return stack.is(tag);
            return false;
        }
    }

    private static final class TierAttributeEntry {
        final ResourceLocation attributeId;
        final String name;
        final double amount;
        final AttributeModifier.Operation operation;
        final EnumSet<EquipmentSlot> requiredSlots;
        final EnumSet<EquipmentSlot> optionalSlots;

        private TierAttributeEntry(ResourceLocation attributeId,
                                   String name,
                                   double amount,
                                   AttributeModifier.Operation operation,
                                   EnumSet<EquipmentSlot> requiredSlots,
                                   EnumSet<EquipmentSlot> optionalSlots) {
            this.attributeId = attributeId;
            this.name = name;
            this.amount = amount;
            this.operation = operation;
            this.requiredSlots = requiredSlots;
            this.optionalSlots = optionalSlots;
        }

        boolean appliesTo(ItemStack stack, EquipmentSlot slot) {
            if (requiredSlots != null && requiredSlots.contains(slot)) {
                return true;
            }
            if (optionalSlots != null && optionalSlots.contains(slot)) {
                return isPreferredEquipmentSlot(stack, slot);
            }
            return false;
        }
    }
}
