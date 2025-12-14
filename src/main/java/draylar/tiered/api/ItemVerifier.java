package draylar.tiered.api;

import elocindev.tierify.Tierify;
import elocindev.tierify.util.TagFallbackMatcher;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ItemVerifier {

    private final String id;
    private final String tag;

    public ItemVerifier(String id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    public boolean isValid(Identifier itemID) {
        return isValid(itemID.toString());
    }

    public boolean isValid(String itemID) {
        if (id != null) {
            return itemID.equals(id);
        } else if (tag != null) {
            try {
                Identifier tagId = new Identifier(tag);
                TagKey<Item> itemTag = TagKey.of(RegistryKeys.ITEM, tagId);

                Item item = Registries.ITEM.get(new Identifier(itemID));
                ItemStack stack = new ItemStack(item);

                // real tag membership
                if (stack.isIn(itemTag)) {
                    return true;
                }

                // conservative fallback inference
                return TagFallbackMatcher.matches(tagId, stack);
            } catch (Exception e) {
                Tierify.LOGGER.error("Invalid verifier tag/id: tag=" + tag + " item=" + itemID, e);
            }
        }

        return false;
    }

    public String getId() {
        return id;
    }

    public TagKey<Item> getTagKey() {
        return tag == null ? null : TagKey.of(RegistryKeys.ITEM, new Identifier(tag));
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode() * 17 + (tag == null ? 0 : tag.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemVerifier other)) return false;
    
        return (id == null ? other.id == null : id.equals(other.id))
            && (tag == null ? other.tag == null : tag.equals(other.tag));
    }
}
