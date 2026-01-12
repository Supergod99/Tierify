package elocindev.tierify.forge.item;

import elocindev.tierify.forge.config.ForgeTierifyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReforgeAddition extends Item {
    private final int tier;

    public ReforgeAddition(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (tier == 0) {
            tooltip.add(Component.literal("Cleansing:").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Scours an item of all reforges.").withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }

        List<String> qualities = ForgeTierifyConfig.getTierQualities(tier);
        if (qualities.isEmpty()) return;

        tooltip.add(Component.literal("Reforging Qualities:").withStyle(ChatFormatting.GRAY));
        for (String quality : qualities) {
            tooltip.add(Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(quality).withStyle(ChatFormatting.DARK_GRAY)));
        }
    }
}
