package elocindev.tierify.screen.client.component;

import elocindev.tierify.screen.client.PerfectLabelAnimator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class PerfectTierComponent implements TooltipComponent {
    private static final float SCALE = 0.65f;

    @Override
    public int getHeight() {
        // Base height 9 * scale + padding
        return (int) (9 * SCALE) + 4;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        Text text = PerfectLabelAnimator.getPerfectLabel();
        return (int) (textRenderer.getWidth(text) * SCALE);
    }

    @Override
    public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix, VertexConsumerProvider.Immediate vertexConsumers) {
        Text text = PerfectLabelAnimator.getPerfectLabel();
        
        // We need a MatrixStack to apply scaling easily, but the interface gives us a raw Matrix4f.
        // We can create a temporary stack or just apply logic directly.
        // Since Tooltip Overhaul calls this, we assume the matrix is set up for the top-left of this line.
        
        MatrixStack stack = new MatrixStack();
        stack.multiplyPositionMatrix(matrix);
        
        stack.push();
        // Scale down
        stack.scale(SCALE, SCALE, 1.0f);
        
        // Adjust Y to center vertically in the line height (optional, looks better)
        float yOffset = (getHeight() - (9 * SCALE)) / 2.0f;
        
        // Note: X is passed as 0 relative to the start of the line because we handle X in the Mixin below
        // However, due to scaling, we must draw at (x / scale, y / scale) if we scaled the whole context,
        // BUT here we are modifying the matrix at the draw call.
        // Effectively, we draw at 0,0 relative to the pushed matrix.
        
        textRenderer.draw(
            text, 
            x / SCALE, // Counter-act scale for position if needed, but usually we rely on the Translation
            (y + yOffset) / SCALE, 
            0xFFFFFF, 
            true, 
            stack.peek().getPositionMatrix(), 
            vertexConsumers, 
            TextRenderer.TextLayerType.NORMAL, 
            0, 
            0xF000F0
        );
        
        stack.pop();
    }
}
