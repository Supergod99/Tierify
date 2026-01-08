package elocindev.tierify;

import java.util.UUID;

public final class TierifyConstants {

    public static final String NBT_SUBTAG_KEY = "Tiered";
    public static final String NBT_SUBTAG_DATA_KEY = "Tier";
    public static final String NBT_SUBTAG_TEMPLATE_DATA_KEY = "Template";

    // Copied from Tierify.java (Fabric) so Forge can filter “tier modifiers” reliably.
    public static final UUID[] MODIFIERS = new UUID[] {
            UUID.fromString("baf8e074-f7f9-4549-ba1f-e21f82684b8c"),
            UUID.fromString("9b3416de-98d1-407f-bc6b-e673c2ab5252"),
            UUID.fromString("1e3ceca6-aa30-4165-9715-20bb63c11348"),
            UUID.fromString("c99bfa17-4886-4cbb-86c2-ebf9369616d5"),
            UUID.fromString("19e4dc8d-3892-4ffe-a558-f96c68491144"),
            UUID.fromString("b1641cff-84ed-4b63-85f8-2634005adc9b"),
            UUID.fromString("92f546e9-0d00-4159-8c8f-0499e49f5811"),
            UUID.fromString("e25c7fa8-13b0-4ea0-8db7-e26b78f36c90"),
            UUID.fromString("2f9dcfce-bd03-4181-86b7-91c88f71e67c")
    };

    private TierifyConstants() {}
}
