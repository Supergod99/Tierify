package elocindev.tierify.platform;

import net.minecraftforge.fml.ModList;

public final class ForgePlatformHelper implements PlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
