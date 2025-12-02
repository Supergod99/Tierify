package elocindev.tierify.screen.client;

import net.minecraft.text.Text;
import java.util.HashMap;
import java.util.Map;

public class TierGradientAnimator {

    private static int tick = 0;
    private static final int INTERVAL = 2;

    // ---- 16-frame gradients per tier ----

    private static final String[] COMMON = {
        "A0A0A0","9A9A9A","959595","8F8F8F","8A8A8A","858585","7F7F7F","7A7A7A",
        "757575","6F6F6F","6A6A6A","656565","5F5F5F","5A5A5A","555555","505050"
    };

    private static final String[] UNCOMMON = {
        "55FF55","50F250","4CE64C","47D947","42CC42","3DBF3D","39B339","34A634",
        "309A30","2B8D2B","268126","227422","1D671D","185B18","144E14","0F420F"
    };

    private static final String[] RARE = {
        "5555FF","5050F2","4C4CE6","4747D9","4242CC","3D3DBF","3939B3","3434A6",
        "30309A","2B2B8D","262681","222274","1D1D67","18185B","14144E","0F0F42"
    };

    private static final String[] EPIC = {
        "AA00FF","A000F2","9600E6","8C00D9","8200CC","7800BF","6E00B3","6400A6",
        "5A009A","50008D","460081","3C0074","320067","28005B","1E004E","140042"
    };

    private static final String[] LEGENDARY = {
        "FFB000","F7A600","EF9C00","E69200","DE8800","D67E00","CE7400","C46A00",
        "BC6000","B25600","AA4C00","A04200","983800","8E2E00","862400","7C1A00"
    };

    private static final String[] MYTHIC = {
        "FF3A3A","F23232","E62929","D92020","CC1818","BF1010","B30808","A60000",
        "9A0000","8D0000","810000","740000","670000","5B0000","4E0000","420000"
    };

    private static final Map<String, String[]> TABLE = new HashMap<>();

    static {
        TABLE.put("common", COMMON);
        TABLE.put("uncommon", UNCOMMON);
        TABLE.put("rare", RARE);
        TABLE.put("epic", EPIC);
        TABLE.put("legendary", LEGENDARY);
        TABLE.put("mythic", MYTHIC);
    }

    public static void clientTick() {
        if (++tick >= INTERVAL) tick = 0;
    }

    public static String getTierFromId(String id) {
        if (id.contains("common")) return "common";
        if (id.contains("uncommon")) return "uncommon";
        if (id.contains("rare")) return "rare";
        if (id.contains("epic")) return "epic";
        if (id.contains("legendary")) return "legendary";
        if (id.contains("mythic")) return "mythic";
        return "common";
    }

    public static String animate(String base, String tier) {
        String[] frames = TABLE.getOrDefault(tier, COMMON);
        int idx = (int)((System.currentTimeMillis() / 75) % frames.length);
        String hex = frames[idx];

        // Strip ALL external formatting first
        String raw = base.replaceAll("§[0-9A-FK-ORa-fk-or]", "");

        String bold = (tier.equals("legendary") || tier.equals("mythic")) ? "§l" : "";

        return "§x§" + hex.charAt(0) + "§" + hex.charAt(1) +
                       "§" + hex.charAt(2) + "§" + hex.charAt(3) +
                       "§" + hex.charAt(4) + "§" + hex.charAt(5) +
                       bold + raw;
    }
}
