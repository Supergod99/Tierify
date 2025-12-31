package elocindev.tierify.mixin.compat;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;

public class TieredMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var loader = FabricLoader.getInstance();
        if (!loader.isModLoaded("levelz") && mixinClassName.endsWith("SkillInfoScreenMixin")) {
            return false;
        }
        if (!loader.isModLoaded("easyanvils") && mixinClassName.endsWith("ModAnvilScreenMixin")) {
            return false;
        }
        if (mixinClassName.contains("TooltipOverhaul") || mixinClassName.contains("TooltipRendererAccessor")) {
            return loader.isModLoaded("tooltipoverhaul")
                    && loader.getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
        }
        // Armageddon mod
        if (mixinClassName.endsWith("ArmageddonTreasureBagProceduresMixin")) {
            return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("armageddon_mod");
        }
        // Curios + Brutality compat
        if (mixinClassName.endsWith("CuriosBrutalityUuidSaltMixin")) {
            return loader.isModLoaded("curios") && loader.isModLoaded("brutality");
        }
        // Brutality + AttributesLib armor pipeline compat
        if (mixinClassName.endsWith("LethalityScalingFixMixin")) {
            return loader.isModLoaded("brutality")
                    || loader.isModLoaded("attributeslib")
                    || loader.isModLoaded("apothic_attributes");
        }
        
        String lower = mixinClassName.toLowerCase();
        if (lower.contains("curios")) {
            return loader.isModLoaded("curios");
        }
        if (lower.contains("brutality")) {
            return loader.isModLoaded("brutality");
        }
        if (lower.contains("attributeslib") || lower.contains("apothic")) {
            return loader.isModLoaded("attributeslib") || loader.isModLoaded("apothic_attributes");
        }
        if (lower.contains("obscure")) {
            return loader.isModLoaded("obscure_api");
        }
    
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
