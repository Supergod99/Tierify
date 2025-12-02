package elocindev.tierify.screen.client;

public class PerfectLabelAnimator {
    
    private static final String[] FRAMES = new String[] {
        "§x§0§A§A§F§F§F✖§x§1§8§B§5§F§F✯§x§2§6§B§B§F§FP§x§3§3§C§1§F§FE§x§4§1§C§7§F§FR§x§4§F§C§D§F§FF§x§5§C§D§3§F§FE§x§6§A§D§9§F§FC§x§7§8§D§F§F§FT§x§8§6§E§5§F§F✯§x§9§3§E§B§F§F✖",
        "§x§1§8§B§5§F§F✖§x§2§6§B§B§F§F✯§x§3§3§C§1§F§FP§x§4§1§C§7§F§FE§x§4§F§C§D§F§FR§x§5§C§D§3§F§FF§x§6§A§D§9§F§FE§x§7§8§D§F§F§FC§x§8§6§E§5§F§FT§x§9§3§E§B§F§F✯§x§0§A§A§F§F§F✖",
        "§x§2§6§B§B§F§F✖§x§3§3§C§1§F§F✯§x§4§1§C§7§F§FP§x§4§F§C§D§F§FE§x§5§C§D§3§F§FR§x§6§A§D§9§F§FF§x§7§8§D§F§F§FE§x§8§6§E§5§F§FC§x§9§3§E§B§F§FT§x§0§A§A§F§F§F✯§x§1§8§B§5§F§F✖",
        "§x§3§3§C§1§F§F✖§x§4§1§C§7§F§F✯§x§4§F§C§D§F§FP§x§5§C§D§3§F§FE§x§6§A§D§9§F§FR§x§7§8§D§F§F§FF§x§8§6§E§5§F§FE§x§9§3§E§B§F§FC§x§0§A§A§F§F§FT§x§1§8§B§5§F§F✯§x§2§6§B§B§F§F✖",
        "§x§4§1§C§7§F§F✖§x§4§F§C§D§F§F✯§x§5§C§D§3§F§FP§x§6§A§D§9§F§FE§x§7§8§D§F§F§FR§x§8§6§E§5§F§FF§x§9§3§E§B§F§FE§x§0§A§A§F§F§FC§x§1§8§B§5§F§FT§x§2§6§B§B§F§F✯§x§3§3§C§1§F§F✖",
        "§x§4§F§C§D§F§F✖§x§5§C§D§3§F§F✯§x§6§A§D§9§F§FP§x§7§8§D§F§F§FE§x§8§6§E§5§F§FR§x§9§3§E§B§F§FF§x§0§A§A§F§F§FE§x§1§8§B§5§F§FC§x§2§6§B§B§F§FT§x§3§3§C§1§F§F✯§x§4§1§C§7§F§F✖",
        "§x§5§C§D§3§F§F✖§x§6§A§D§9§F§F✯§x§7§8§D§F§F§FP§x§8§6§E§5§F§FE§x§9§3§E§B§F§FR§x§0§A§A§F§F§FF§x§1§8§B§5§F§FE§x§2§6§B§B§F§FC§x§3§3§C§1§F§FT§x§4§1§C§7§F§F✯§x§4§F§C§D§F§F✖",
        "§x§6§A§D§9§F§F✖§x§7§8§D§F§F§F✯§x§8§6§E§5§F§FP§x§9§3§E§B§F§FE§x§0§A§A§F§F§FR§x§1§8§B§5§F§FF§x§2§6§B§B§F§FE§x§3§3§C§1§F§FC§x§4§1§C§7§F§FT§x§4§F§C§D§F§F✯§x§5§C§D§3§F§F✖",
        "§x§7§8§D§F§F§F✖§x§8§6§E§5§F§F✯§x§9§3§E§B§F§FP§x§0§A§A§F§F§FE§x§1§8§B§5§F§FR§x§2§6§B§B§F§FF§x§3§3§C§1§F§FE§x§4§1§C§7§F§FC§x§4§F§C§D§F§FT§x§5§C§D§3§F§F✯§x§6§A§D§9§F§F✖",
        "§x§8§6§E§5§F§F✖§x§9§3§E§B§F§F✯§x§0§A§A§F§F§FP§x§1§8§B§5§F§FE§x§2§6§B§B§F§FR§x§3§3§C§1§F§FF§x§4§1§C§7§F§FE§x§4§F§C§D§F§FC§x§5§C§D§3§F§FT§x§6§A§D§9§F§F✯§x§7§8§D§F§F§F✖",
        "§x§9§3§E§B§F§F✖§x§0§A§A§F§F§F✯§x§1§8§B§5§F§FP§x§2§6§B§B§F§FE§x§3§3§C§1§F§FR§x§4§1§C§7§F§FF§x§4§F§C§D§F§FE§x§5§C§D§3§F§FC§x§6§A§D§9§F§FT§x§7§8§D§F§F§F✯§x§8§6§E§5§F§F✖",
        "§x§0§A§A§F§F§F✖§x§1§8§B§5§F§F✯§x§2§6§B§B§F§FP§x§3§3§C§1§F§FE§x§4§1§C§7§F§FR§x§4§F§C§D§F§FF§x§5§C§D§3§F§FE§x§6§A§D§9§F§FC§x§7§8§D§F§F§FT§x§8§6§E§5§F§F✯§x§9§3§E§B§F§F✖",
        "§x§1§8§B§5§F§F✖§x§2§6§B§B§F§F✯§x§3§3§C§1§F§FP§x§4§1§C§7§F§FE§x§4§F§C§D§F§FR§x§5§C§D§3§F§FF§x§6§A§D§9§F§FE§x§7§8§D§F§F§FC§x§8§6§E§5§F§FT§x§9§3§E§B§F§F✯§x§0§A§A§F§F§F✖",
        "§x§2§6§B§B§F§F✖§x§3§3§C§1§F§F✯§x§4§1§C§7§F§FP§x§4§F§C§D§F§FE§x§5§C§D§3§F§FR§x§6§A§D§9§F§FF§x§7§8§D§F§F§FE§x§8§6§E§5§F§FC§x§9§3§E§B§F§FT§x§0§A§A§F§F§F✯§x§1§8§B§5§F§F✖",
        "§x§3§3§C§1§F§F✖§x§4§1§C§7§F§F✯§x§4§F§C§D§F§FP§x§5§C§D§3§F§FE§x§6§A§D§9§F§FR§x§7§8§D§F§F§FF§x§8§6§E§5§F§FE§x§9§3§E§B§F§FC§x§0§A§A§F§F§FT§x§1§8§B§5§F§F✯§x§2§6§B§B§F§F✖",
        "§x§4§1§C§7§F§F✖§x§4§F§C§D§F§F✯§x§5§C§D§3§F§FP§x§6§A§D§9§F§FE§x§7§8§D§F§F§FR§x§8§6§E§5§F§FF§x§9§3§E§B§F§FE§x§0§A§A§F§F§FC§x§1§8§B§5§F§FT§x§2§6§B§B§F§F✯§x§3§3§C§1§F§F✖",
        "§x§4§F§C§D§F§F✖§x§5§C§D§3§F§F✯§x§6§A§D§9§F§FP§x§7§8§D§F§F§FE§x§8§6§E§5§F§FR§x§9§3§E§B§F§FF§x§0§A§A§F§F§FE§x§1§8§B§5§F§FC§x§2§6§B§B§F§FT§x§3§3§C§1§F§F✯§x§4§1§C§7§F§F✖",
        "§x§5§C§D§3§F§F✖§x§6§A§D§9§F§F✯§x§7§8§D§F§F§FP§x§8§6§E§5§F§FE§x§9§3§E§B§F§FR§x§0§A§A§F§F§FF§x§1§8§B§5§F§FE§x§2§6§B§B§F§FC§x§3§3§C§1§F§FT§x§4§1§C§7§F§F✯§x§4§F§C§D§F§F✖",
        "§x§6§A§D§9§F§F✖§x§7§8§D§F§F§F✯§x§8§6§E§5§F§FP§x§9§3§E§B§F§FE§x§0§A§A§F§F§FR§x§1§8§B§5§F§FF§x§2§6§B§B§F§FE§x§3§3§C§1§F§FC§x§4§1§C§7§F§FT§x§4§F§C§D§F§F✯§x§5§C§D§3§F§F✖",
        "§x§7§8§D§F§F§F✖§x§8§6§E§5§F§F✯§x§9§3§E§B§F§FP§x§0§A§A§F§F§FE§x§1§8§B§5§F§FR§x§2§6§B§B§F§FF§x§3§3§C§1§F§FE§x§4§1§C§7§F§FC§x§4§F§C§D§F§FT§x§5§C§D§3§F§F✯§x§6§A§D§9§F§F✖",
        "§x§8§6§E§5§F§F✖§x§9§3§E§B§F§F✯§x§0§A§A§F§F§FP§x§1§8§B§5§F§FE§x§2§6§B§B§F§FR§x§3§3§C§1§F§FF§x§4§1§C§7§F§FE§x§4§F§C§D§F§FC§x§5§C§D§3§F§FT§x§6§A§D§9§F§F✯§x§7§8§D§F§F§F✖",
        "§x§9§3§E§B§F§F✖§x§0§A§A§F§F§F✯§x§1§8§B§5§F§FP§x§2§6§B§B§F§FE§x§3§3§C§1§F§FR§x§4§1§C§7§F§FF§x§4§F§C§D§F§FE§x§5§C§D§3§F§FC§x§6§A§D§9§F§FT§x§7§8§D§F§F§F✯§x§8§6§E§5§F§F✖",
        "§x§0§A§A§F§F§F✖§x§1§8§B§5§F§F✯§x§2§6§B§B§F§FP§x§3§3§C§1§F§FE§x§4§1§C§7§F§FR§x§4§F§C§D§F§FF§x§5§C§D§3§F§FE§x§6§A§D§9§F§FC§x§7§8§D§F§F§FT§x§8§6§E§5§F§F✯§x§9§3§E§B§F§F✖",
        "§x§1§8§B§5§F§F✖§x§2§6§B§B§F§F✯§x§3§3§C§1§F§FP§x§4§1§C§7§F§FE§x§4§F§C§D§F§FR§x§5§C§D§3§F§FF§x§6§A§D§9§F§FE§x§7§8§D§F§F§FC§x§8§6§E§5§F§FT§x§9§3§E§B§F§F✯§x§0§A§A§F§F§F✖",
        "§x§2§6§B§B§F§F✖§x§3§3§C§1§F§F✯§x§4§1§C§7§F§FP§x§4§F§C§D§F§FE§x§5§C§D§3§F§FR§x§6§A§D§9§F§FF§x§7§8§D§F§F§FE§x§8§6§E§5§F§FC§x§9§3§E§B§F§FT§x§0§A§A§F§F§F✯§x§1§8§B§5§F§F✖",
        "§x§3§3§C§1§F§F✖§x§4§1§C§7§F§F✯§x§4§F§C§D§F§FP§x§5§C§D§3§F§FE§x§6§A§D§9§F§FR§x§7§8§D§F§F§FF§x§8§6§E§5§F§FE§x§9§3§E§B§F§FC§x§0§A§A§F§F§FT§x§1§8§B§5§F§F✯§x§2§6§B§B§F§F✖",
        "§x§4§1§C§7§F§F✖§x§4§F§C§D§F§F✯§x§5§C§D§3§F§FP§x§6§A§D§9§F§FE§x§7§8§D§F§F§FR§x§8§6§E§5§F§FF§x§9§3§E§B§F§FE§x§0§A§A§F§F§FC§x§1§8§B§5§F§FT§x§2§6§B§B§F§F✯§x§3§3§C§1§F§F✖",
        "§x§4§F§C§D§F§F✖§x§5§C§D§3§F§F✯§x§6§A§D§9§F§FP§x§7§8§D§F§F§FE§x§8§6§E§5§F§FR§x§9§3§E§B§F§FF§x§0§A§A§F§F§FE§x§1§8§B§5§F§FC§x§2§6§B§B§F§FT§x§3§3§C§1§F§F✯§x§4§1§C§7§F§F✖",
        "§x§5§C§D§3§F§F✖§x§6§A§D§9§F§F✯§x§7§8§D§F§F§FP§x§8§6§E§5§F§FE§x§9§3§E§B§F§FR§x§0§A§A§F§F§FF§x§1§8§B§5§F§FE§x§2§6§B§B§F§FC§x§3§3§C§1§F§FT§x§4§1§C§7§F§F✯§x§4§F§C§D§F§F✖",
        "§x§6§A§D§9§F§F✖§x§7§8§D§F§F§F✯§x§8§6§E§5§F§FP§x§9§3§E§B§F§FE§x§0§A§A§F§F§FR§x§1§8§B§5§F§FF§x§2§6§B§B§F§FE§x§3§3§C§1§F§FC§x§4§1§C§7§F§FT§x§4§F§C§D§F§F✯§x§5§C§D§3§F§F✖",
        "§x§7§8§D§F§F§F✖§x§8§6§E§5§F§F✯§x§9§3§E§B§F§FP§x§0§A§A§F§F§FE§x§1§8§B§5§F§FR§x§2§6§B§B§F§FF§x§3§3§C§1§F§FE§x§4§1§C§7§F§FC§x§4§F§C§D§F§FT§x§5§C§D§3§F§F✯§x§6§A§D§9§F§F✖",
        "§x§8§6§E§5§F§F✖§x§9§3§E§B§F§F✯§x§0§A§A§F§F§FP§x§1§8§B§5§F§FE§x§2§6§B§B§F§FR§x§3§3§C§1§F§FF§x§4§1§C§7§F§FE§x§4§F§C§D§F§FC§x§5§C§D§3§F§FT§x§6§A§D§9§F§F✯§x§7§8§D§F§F§F✖"
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
