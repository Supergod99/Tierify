package elocindev.tierify.screen.client;

import java.util.Locale;

public class TierGradientAnimator {

    private static final int INTERVAL = 2;
    private static int tick = 0;

    private static int commonF = 0, uncommonF = 0, rareF = 0, epicF = 0, legendaryF = 0, mythicF = 0;

    private static final String[] COMMON = {
        "505050","5B5B5B","656565","707070","7B7B7B","858585","909090","9B9B9B",
        "A4A4A4","ADADAD","B5B5B5","BEBEBE","C6C6C6","CFCFCF","D7D7D7","E0E0E0"
    };

    private static final String[] UNCOMMON = {
        "0F5F0F","146D14","187B18","1D891D","219821","26A626","2AB42A","2FC22F",
        "33D133","38D938","3DE13D","43E843","4FDA4F","5BE15B","68E868","74EF74"
    };

    private static final String[] RARE = {
        "0A3A8F","0D439E","0F4CAD","1256BC","155FCB","1768DA","1A72E9","1D7BF8",
        "2188FF","258FFF","2896FF","2C97FF","31A1FF","37ABFF","3CB5FF","42BEFF"
    };

    private static final String[] EPIC = {
        "5A0A8F","65119E","7117AD","7C1EBC","8825CB","932BDA","9F32E9","AA38F8",
        "B644FF","BE4CFF","C451FF","C551FF","CE5AFF","D662FF","DF6BFF","E773FF"
    };

    private static final String[] LEGENDARY = {
        "B87400","BE7D00","C58500","CB8E00","D29600","D89F00","DFA700","E5B000",
        "ECB800","F0B500","F3B000","F6A900","F9A200","FC9B00","FF94000","FF7C00"
    };

    private static final String[] MYTHIC = {
        "700000","830000","960000","A90000","BC0000","CF0000","E20000","F50000",
        "F10000","D60000","BA0000","9E0000","830000","670000","4C0000","300000"
    };

    public static void clientTick() {
        tick++;
        if (tick >= INTERVAL) {
            tick = 0;
            commonF = (commonF+1)%16;
            uncommonF = (uncommonF+1)%16;
            rareF = (rareF+1)%16;
            epicF = (epicF+1)%16;
            legendaryF = (legendaryF+1)%16;
            mythicF = (mythicF+1)%16;
        }
    }

    private static String rgbToMC(String hex) {
        hex = hex.toUpperCase(Locale.ROOT);
        return "§x§" + hex.charAt(0) + "§" + hex.charAt(1) + "§" +
                      hex.charAt(2) + "§" + hex.charAt(3) + "§" +
                      hex.charAt(4) + "§" + hex.charAt(5);
    }

    public static String getTierFromId(String id) {
        if (id == null) return "";
        int c = id.indexOf(':');
        String path = c >= 0 ? id.substring(c+1) : id;
        String[] p = path.split("_");
        return p.length > 0 ? p[0].toLowerCase(Locale.ROOT) : "";
    }

    public static String animate(String name, String tier) {
        if (name == null || name.isEmpty()) return name;

        String[] pal;
        int f;

        switch (tier) {
            case "common": pal = COMMON; f = commonF; break;
            case "uncommon": pal = UNCOMMON; f = uncommonF; break;
            case "rare": pal = RARE; f = rareF; break;
            case "epic": pal = EPIC; f = epicF; break;
            case "legendary": pal = LEGENDARY; f = legendaryF; break;
            case "mythic": pal = MYTHIC; f = mythicF; break;
            default: return name;
        }

        StringBuilder out = new StringBuilder(name.length()*12);
        for (int i = 0; i < name.length(); i++) {
            String hex = pal[(i+f)%16];
            out.append(rgbToMC(hex)).append(name.charAt(i));
        }
        return out.toString();
    }
}
