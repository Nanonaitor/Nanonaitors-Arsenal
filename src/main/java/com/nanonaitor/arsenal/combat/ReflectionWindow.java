package com.nanonaitor.arsenal.combat;

/** Pure rules for separating cooldown synchronization from ability cancellation. */
public final class ReflectionWindow {
    private ReflectionWindow() {}
    public static boolean externalCooldown(boolean client,boolean cooling,boolean ownEntry) {
        return !client && cooling && !ownEntry;
    }
}
