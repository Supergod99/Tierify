package elocindev.tierify.screen.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;

public class ScaledText extends MutableText {

    private final MutableText inner;
    private final float scale;

    public ScaledText(MutableText inner, float scale) {
        super(inner.getContent(), inner.getSiblings(), inner.getStyle());
        this.inner = inner;
        this.scale = scale;
    }

    public void render(DrawContext context, TextRenderer renderer, int x, int y, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        renderer.draw(
            inner,
            0,
            0,
            color,
            false,
            context.getMatrices().peek().getPositionMatrix(),
            context.getVertexConsumers(),
            TextRenderer.TextLayerType.NORMAL,
            0,
            0xF000F0
        );

        context.getMatrices().pop();
    }

    public MutableText getInner() {
        return inner;
    }

    public float getScale() {
        return scale;
    }
}
