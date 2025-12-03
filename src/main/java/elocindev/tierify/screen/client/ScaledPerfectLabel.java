package elocindev.tierify.screen.client;

import net.minecraft.text.MutableText;

public class ScaledPerfectLabel {

    private final MutableText inner;
    private final float scale;

    public ScaledPerfectLabel(MutableText inner, float scale) {
        this.inner = inner;
        this.scale = scale;
    }

    public MutableText getInner() {
        return inner;
    }

    public float getScale() {
        return scale;
    }
}
