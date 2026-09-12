package com.nanonaitor.arsenal.client;

/** Sound-event IDs, not the differently named underlying OGG files. */
public final class ChainSwingSoundFilter {
    private ChainSwingSoundFilter() {}

    public static boolean isExtraMeleeSound(String domain, String path) {
        if ("minecraft".equals(domain)) {
            return path.startsWith("entity.player.attack.");
        }
        if ("bettercombatmod".equals(domain)) {
            return path.startsWith("player.swing_") || path.startsWith("swing_")
                || "player.swordslash".equals(path);
        }
        if ("dsurround".equals(domain)) {
            return "sword.swing".equals(path) || "blunt.swing".equals(path)
                || "tool.swing".equals(path);
        }
        return false;
    }
}
