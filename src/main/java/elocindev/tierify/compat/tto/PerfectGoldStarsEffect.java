package elocindev.tierify.compat.tto;

import dev.xylonity.tooltipoverhaul.client.layer.LayerDepth;
import dev.xylonity.tooltipoverhaul.client.layer.impl.EffectLayer;
import dev.xylonity.tooltipoverhaul.client.render.TooltipContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Tierify custom "Perfect" stars:
 * - Smaller
 * - More frequent
 * - 2–3 concurrently
 * - Gold palette
 *
 * This is intentionally Tierify-owned and only injected when Tierify is using its DEFAULT effects
 * (i.e., no user override and no template-index test mode), so TooltipOverhaul's built-in stars remain
 * untouched when overrides are enabled.
 */
public final class PerfectGoldStarsEffect implements EffectLayer {

    // Desired behavior knobs
    private static final int STAR_CAP = 3;
    private static final long SPAWN_INTERVAL_MS = 900L;

    private static final int MIN_LIFE_MS = 500;
    private static final int MAX_LIFE_MS = 900;

    private static final float MIN_SIZE = 8.0f;
    private static final float MAX_SIZE = 18.0f;

    // Visual tuning
    private static final float BORDER_PADDING = 6.0f; // spawn around border band

    // Gold palette (ARGB)
    private static final int GOLD_CORE = 0xFFFFD36A;
    private static final int GOLD_HOT  = 0xFFFFF1B5;
    private static final int GOLD_DEEP = 0xFFFFB000;

    private final Random random = new Random();
    private final Deque<Star> stars = new ArrayDeque<>();
    private long lastSpawn = 0L;

    @Override
    public LayerDepth getLayerDepth() {
        return LayerDepth.EFFECT;
    }

    @Override
    public void render(TooltipContext context, Vec2f position) {
        final long now = System.currentTimeMillis();

        final int x0 = (int) position.x;
        final int y0 = (int) position.y;
        final int w  = (int) context.getTooltipSize().x;
        final int h  = (int) context.getTooltipSize().y;

        // Cull expired
        while (!stars.isEmpty() && stars.peekFirst().isExpired(now)) {
            stars.removeFirst();
        }

        // Spawn
        if (stars.size() < STAR_CAP && now - lastSpawn >= SPAWN_INTERVAL_MS) {
            stars.addLast(spawnStar(now, w, h));
            lastSpawn = now;
        }

        if (stars.isEmpty()) return;

        DrawContext dc = context.getGraphics();

        for (Star s : stars) {
            float t = s.lifeProgress(now); // 0..1
            if (t >= 1.0f) continue;

            // Fade in/out with a soft peak
            float alpha = easePulse(t);

            // Subtle twinkle
            float twinkle = 0.85f + 0.15f * (float) Math.sin((now - s.birthMs) * 0.020);

            float size = s.size * twinkle;
            int cx = x0 + Math.round(s.offsetX);
            int cy = y0 + Math.round(s.offsetY);

            // Draw glow + core
            int glowA = clamp255((int) (alpha * 90));
            int coreA = clamp255((int) (alpha * 220));

            int glowColor = withAlpha(GOLD_CORE, glowA);
            int coreColor = withAlpha(pickGold(coreA), coreA);

            drawSparkle(dc, cx, cy, size + 4.0f, glowColor, 2);
            drawSparkle(dc, cx, cy, size, coreColor, 1);
        }
    }

    private Star spawnStar(long now, int tooltipW, int tooltipH) {
        // Spawn around the perimeter band so stars feel like they're orbiting the tooltip frame.
        int side = random.nextInt(4); // 0=top,1=right,2=bottom,3=left

        float x;
        float y;

        switch (side) {
            case 0 -> { // top
                x = randRange(-BORDER_PADDING, tooltipW + BORDER_PADDING);
                y = randRange(-BORDER_PADDING, 0.0f);
            }
            case 1 -> { // right
                x = randRange(tooltipW, tooltipW + BORDER_PADDING);
                y = randRange(-BORDER_PADDING, tooltipH + BORDER_PADDING);
            }
            case 2 -> { // bottom
                x = randRange(-BORDER_PADDING, tooltipW + BORDER_PADDING);
                y = randRange(tooltipH, tooltipH + BORDER_PADDING);
            }
            default -> { // left
                x = randRange(-BORDER_PADDING, 0.0f);
                y = randRange(-BORDER_PADDING, tooltipH + BORDER_PADDING);
            }
        }

        float size = randRange(MIN_SIZE, MAX_SIZE);
        int life = (int) randRange(MIN_LIFE_MS, MAX_LIFE_MS);

        return new Star(x, y, size, now, life);
    }

    /**
     * Draw a sparkle using simple rects (no textures), so it's self-contained and stable.
     * thickness = 1 or 2 typically.
     */
    private static void drawSparkle(DrawContext dc, int cx, int cy, float size, int color, int thickness) {
        int half = Math.max(1, Math.round(size * 0.5f));
        int t = Math.max(1, thickness);

        // plus "+"
        dc.fill(cx - t, cy - half, cx + t, cy + half + 1, color);
        dc.fill(cx - half, cy - t, cx + half + 1, cy + t, color);

        // "x" diagonals as short stepped rects
        // (very small sparkle; stepping keeps it crisp in pixel UI)
        for (int i = -half; i <= half; i += 2) {
            dc.fill(cx + i, cy + i, cx + i + t, cy + i + t, color);
            dc.fill(cx + i, cy - i, cx + i + t, cy - i + t, color);
        }
    }

    private float randRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static int clamp255(int a) {
        return Math.max(0, Math.min(255, a));
    }

    private static int withAlpha(int argb, int a) {
        return (clamp255(a) << 24) | (argb & 0x00FFFFFF);
    }

    private int pickGold(int a) {
        // Rotate between a few gold tones
        int choice = random.nextInt(3);
        int base = (choice == 0) ? GOLD_HOT : (choice == 1) ? GOLD_CORE : GOLD_DEEP;
        return withAlpha(base, a);
    }

    /**
     * Soft pulse: fades in quickly, peaks, fades out.
     */
    private static float easePulse(float t) {
        // 0..1 -> bell-ish curve
        // peak at ~0.5
        float x = (t <= 0.5f) ? (t / 0.5f) : ((1.0f - t) / 0.5f);
        // smoothstep-ish
        return x * x * (3.0f - 2.0f * x);
    }

    private record Star(float offsetX, float offsetY, float size, long birthMs, int lifeMs) {
        boolean isExpired(long now) {
            return (now - birthMs) >= lifeMs;
        }
        float lifeProgress(long now) {
            return Math.min(1.0f, (now - birthMs) / (float) lifeMs);
        }
    }
}
