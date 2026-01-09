package elocindev.tierify.forge.mixin;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class TieredForgeMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("elocindev.tierify.forge.mixin.compat.TooltipOverhaul")) {
            return isModLoaded("tooltipoverhaul");
        }
        if (mixinClassName.equals("elocindev.tierify.forge.mixin.compat.SkillInfoScreenMixin")) {
            return isModLoaded("levelz");
        }
        if (mixinClassName.startsWith("elocindev.tierify.forge.mixin.compat.ObscureApi")) {
            return isModLoaded("obscure_api");
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
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModLoaded(String modId) {
        LoadingModList loading = LoadingModList.get();
        if (loading != null) {
            return loading.getModFileById(modId) != null;
        }
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }
}
