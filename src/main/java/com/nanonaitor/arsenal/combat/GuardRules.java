package com.nanonaitor.arsenal.combat;

/** Pure rules shared by gameplay, UI, and regression tests. */
public final class GuardRules {
    public static final int MAX_STRAIN=25;
    public static final int RECOVERY_TICKS=60;
    private GuardRules() {}
    public static double pairedInterval(double mainCooldown, double offCooldown) {
        return Math.max(1.0D, 1.0D / (1.0D / Math.max(1.0D, mainCooldown)
            + 1.0D / Math.max(1.0D, offCooldown)));
    }
    public static int scimitarWear(float blockedDamage) {
        return Math.max(1,(int)Math.ceil(blockedDamage / 2.0D));
    }
    public static int recoveredStrain(int strain,long elapsedTicks) {
        return Math.max(0,strain-(int)Math.min(MAX_STRAIN,Math.max(0L,elapsedTicks)/20L));
    }
}
