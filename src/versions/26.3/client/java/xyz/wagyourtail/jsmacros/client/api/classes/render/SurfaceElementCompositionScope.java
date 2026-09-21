package xyz.wagyourtail.jsmacros.client.api.classes.render;

/**
 * Keeps one surface element's otherwise separate native feature submissions in one ordered phase.
 */
public final class SurfaceElementCompositionScope implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> LATE_DEPTH = ThreadLocal.withInitial(() -> 0);
    private final boolean active;
    private final boolean late;
    private boolean closed;

    private SurfaceElementCompositionScope(boolean active, boolean late) {
        this.active = active;
        this.late = late;
        if (active) DEPTH.set(DEPTH.get() + 1);
        if (late) LATE_DEPTH.set(LATE_DEPTH.get() + 1);
    }

    public static SurfaceElementCompositionScope enter(boolean active, boolean late) {
        return new SurfaceElementCompositionScope(active, late);
    }

    public static boolean isActive() {
        return DEPTH.get() > 0;
    }

    public static boolean isLateActive() {
        return LATE_DEPTH.get() > 0;
    }

    @Override
    public void close() {
        if (!active || closed) {
            return;
        }
        closed = true;
        int depth = DEPTH.get() - 1;
        if (depth == 0) DEPTH.remove();
        else DEPTH.set(depth);
        if (late) {
            int lateDepth = LATE_DEPTH.get() - 1;
            if (lateDepth == 0) LATE_DEPTH.remove();
            else LATE_DEPTH.set(lateDepth);
        }
    }
}
