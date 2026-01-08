package elocindev.tierify.platform;

public final class Platform {
    private static PlatformHelper impl;

    private Platform() {}

    public static void init(PlatformHelper implementation) {
        impl = implementation;
    }

    public static boolean isModLoaded(String modId) {
        if (impl == null) {
            throw new IllegalStateException("Platform not initialized");
        }
        return impl.isModLoaded(modId);
    }
}

