package elocindev.tierify.screen.client;

public class PerfectLabelAnimator {
    
    private static final String[] FRAMES = new String[] {
        "§0✖§b✯§b§lPERFECT§b✯§0✖",
        "§0✖§3✯§b§lPERFECT§3✯§0✖",
        "§3✖§b✯§b§lPERFECT§b✯§3✖",
        "§b✖§b✯§b§lPERFECT§b✯§b✖",
        "§3✖§b✯§b§lPERFECT§b✯§3✖",
        "§0✖§3✯§b§lPERFECT§3✯§0✖",
        "§0✖§b✯§b§lPERFECT§b✯§0✖",
        "§0✖§0✯§b§lPERFECT§0✯§0✖"
    };

    private static int tickCounter = 0;
    private static int frame = 0;

    private static final int INTERVAL = 2;

    public static void clientTick() {
        tickCounter++;
        if (tickCounter >= INTERVAL) {
            tickCounter = 0;
            frame = (frame + 1) % FRAMES.length;
        }
    }

    public static String getPerfectLabel() {
        return FRAMES[frame];
    }
}
