package elocindev.tierify.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "echelon-client")
@Config.Gui.Background("minecraft:textures/block/stone.png")
public class ClientConfig implements ConfigData {
    @Comment("Whether or not to show the reforging tab in the anvil screen.")
    @ConfigEntry.Category("client_settings")
    public boolean showReforgingTab = true;

    @ConfigEntry.Category("client_settings")
    public int xIconPosition = 0;

    @ConfigEntry.Category("client_settings")
    public int yIconPosition = 0;

    
    @ConfigEntry.Category("client_settings")
    public boolean tieredTooltip = true;

    @Comment("Swaps the text with a plate displayed on the item's name.")
    @ConfigEntry.Category("client_settings")
    public boolean showPlatesOnName = false;

    @ConfigEntry.Category("client_settings")
    public boolean centerName = true;

    @Comment("TooltipOverhaul specialEffect override for Tierify tiered tooltips. Leave blank to use Tierify defaults. " +
            "You can stack multiple effects separated by ',' or ';' (e.g. \"galaxy, white_dust\").")
    @ConfigEntry.Category("client_settings")
    public String ttoSpecialEffectOverride = "";

    @Comment("If true (and override is blank), Tierify assigns a TooltipOverhaul effect per border template index for quick testing.")
    @ConfigEntry.Category("client_settings")
    public boolean ttoSpecialEffectByTemplateIndex = false;
 
}
