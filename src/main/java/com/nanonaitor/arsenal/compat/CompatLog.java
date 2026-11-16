package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports a broken optional-mod integration once instead of crashing a tick
 * handler. Callers fall back to Arsenal's built-in behavior afterwards.
 */
public final class CompatLog {
    private static final Set<String> REPORTED =
        Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private CompatLog() {}

    public static void warnOnce(String key, String message, Throwable cause) {
        if (REPORTED.add(key)) {
            NanonaitorsArsenal.LOGGER.error("{} Arsenal will use its fallback behavior.", message, cause);
        }
    }
}
