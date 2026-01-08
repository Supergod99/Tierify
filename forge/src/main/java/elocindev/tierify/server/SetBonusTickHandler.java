package elocindev.tierify.server;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SetBonusTickHandler {
    private static final Map<Object, Integer> TICKS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SetBonusTickHandler() {}

    public static <P> void endServerTick(
            Object serverKey,
            Iterable<P> players,
            BooleanSupplier enabled,
            Consumer<P> apply,
            Consumer<P> remove
    ) {
        int tick = TICKS.merge(serverKey, 1, Integer::sum);
        if (tick % 20 != 0) return;

        boolean on = enabled.getAsBoolean();
        for (P p : players) {
            if (on) apply.accept(p);
            else remove.accept(p);
        }
    }

    /** Optional: call on server stopping to reset tick counter early. */
    public static void clearForServer(Object serverKey) {
        TICKS.remove(serverKey);
    }
}
