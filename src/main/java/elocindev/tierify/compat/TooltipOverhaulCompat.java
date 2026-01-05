package elocindev.tierify.compat;

public class TooltipOverhaulCompat {
    public static void init() {
        // TooltipOverhaul 1.4+ integration is handled via mixins:
        // - StyleFactory#create(...) appends TierifyBorderLayer
        // - CustomFrameManager#of(...) is overridden for Tierify items (CustomFrameData)
        //
        // No runtime registration is required (and old TooltipRendererAccessor is not valid in 1.4).
        System.out.println("[Tierify] TooltipOverhaulCompat.init(): no-op (1.4+ mixin integration).");
    }
}
