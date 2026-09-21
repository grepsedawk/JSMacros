package xyz.wagyourtail.jsmacros.client.api.classes.render;

/**
 * Marks item and special-model submissions that must use the see-through feature phase.
 */
public final class SurfaceItemSeeThroughScope implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private final boolean active;
    private boolean closed;

    private SurfaceItemSeeThroughScope(boolean active) {
        this.active = active;
    }

    public static SurfaceItemSeeThroughScope enter(boolean seeThrough) {
        if (seeThrough) {
            DEPTH.set(DEPTH.get() + 1);
        }
        return new SurfaceItemSeeThroughScope(seeThrough);
    }

    public static boolean isActive() {
        return DEPTH.get() > 0;
    }

    @Override
    public void close() {
        if (!active || closed) {
            return;
        }
        closed = true;
        int depth = DEPTH.get() - 1;
        if (depth == 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }
}
