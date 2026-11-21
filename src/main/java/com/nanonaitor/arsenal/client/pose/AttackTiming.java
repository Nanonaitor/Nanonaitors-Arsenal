package com.nanonaitor.arsenal.client.pose;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Tracks when each visible player starts a swing, so swing animations can last
 * as long as the weapon's real attack cooldown (20 / attack speed ticks)
 * instead of vanilla's fixed six ticks. Attack speed is a synced attribute, so
 * this works for other players as well as the local one.
 */
public final class AttackTiming {
    private static final Map<Player, Swing> SWINGS = new WeakHashMap<>();

    private static final class Swing {
        boolean wasSwinging;
        int lastProgress;
        long startTick = Long.MIN_VALUE;
        int durationTicks = 6;
        int count;
        InteractionHand hand = InteractionHand.MAIN_HAND;
        /** Each hand's own latest swing, so paired weapons can swing both at once. */
        final long[] handStart = {Long.MIN_VALUE, Long.MIN_VALUE};
        final int[] handDuration = {6, 6};
        final int[] handCount = {0, 0};
        /** A swing reported directly (see noteSwing) this or last tick: skip re-detecting it. */
        long notedTick = Long.MIN_VALUE;
        /** Last tick this player was using (guarding with) an item. */
        long lastUsingTick = Long.MIN_VALUE;
        /** The current swing started straight out of a guard (a shield bash). */
        boolean fromGuard;
    }

    /** Capture tool only: a fixed swing progress for the local player (-1 = off). */
    public static float debugPhase = -1.0F;
    public static int debugCount = -1;

    private static Swing swing(Player player) {
        return SWINGS.computeIfAbsent(player, ignored -> new Swing());
    }

    /**
     * Reports a swing as it happens. Vanilla keeps one swing per player, so a hand
     * swinging right after the other would hide the first from tick detection;
     * paired weapons (claws) report each hand so both animations play together.
     */
    public static void noteSwing(Player player, InteractionHand hand) {
        if (player.level() == null) return;
        Swing swing = swing(player);
        long now = player.level().getGameTime();
        int h = hand.ordinal();
        swing.handStart[h] = now;
        swing.handDuration[h] = cooldownTicks(player);
        swing.handCount[h]++;
        swing.startTick = now;
        swing.durationTicks = swing.handDuration[h];
        swing.hand = hand;
        swing.count++;
        swing.notedTick = now;
        swing.fromGuard = swing.lastUsingTick >= now - 2L;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { SWINGS.clear(); return; }
        long now = mc.level.getGameTime();
        for (Player player : mc.level.players()) {
            Swing swing = swing(player);
            boolean swinging = player.swinging;
            boolean noted = swing.notedTick >= now - 1L;
            // A new swing either starts from rest or restarts mid-swing (progress drops).
            if (swinging && !noted && (!swing.wasSwinging || player.swingTime < swing.lastProgress)) {
                swing.startTick = now - Math.max(0, player.swingTime);
                swing.durationTicks = cooldownTicks(player);
                swing.hand = player.swingingArm == null ? InteractionHand.MAIN_HAND : player.swingingArm;
                swing.count++;
                int h = swing.hand.ordinal();
                swing.handStart[h] = swing.startTick;
                swing.handDuration[h] = swing.durationTicks;
                swing.handCount[h]++;
                swing.fromGuard = swing.lastUsingTick >= now - 2L;
            }
            if (player.isUsingItem()) swing.lastUsingTick = now;
            swing.wasSwinging = swinging;
            swing.lastProgress = player.swingTime;
        }
    }

    /** Ticks for a full attack at this player's current attack speed, 4 to 40. */
    public static int cooldownTicks(Player player) {
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        double value = speed == null ? 4.0D : speed.getValue();
        return Math.max(4, Math.min(40, (int)Math.round(20.0D / Math.max(0.05D, value))));
    }

    private static boolean debug(Player player) {
        return debugPhase >= 0.0F && player == Minecraft.getInstance().player;
    }

    /** Swing progress 0..1 timed to attack speed, or -1 when no swing is playing. */
    public static float phase(Player player, float partialTicks) {
        if (debug(player)) return debugPhase;
        Swing swing = SWINGS.get(player);
        if (swing == null || swing.startTick == Long.MIN_VALUE) return -1.0F;
        float phase = (player.level().getGameTime() - swing.startTick + partialTicks) / swing.durationTicks;
        return phase >= 0.0F && phase < 1.0F ? phase : -1.0F;
    }

    /** One hand's own swing progress 0..1, or -1: the other hand may be swinging too. */
    public static float phase(Player player, float partialTicks, InteractionHand hand) {
        if (debug(player)) return debugPhase;
        Swing swing = SWINGS.get(player);
        int h = hand.ordinal();
        if (swing == null || swing.handStart[h] == Long.MIN_VALUE) return -1.0F;
        float phase = (player.level().getGameTime() - swing.handStart[h] + partialTicks) / swing.handDuration[h];
        return phase >= 0.0F && phase < 1.0F ? phase : -1.0F;
    }

    public static int count(Player player, InteractionHand hand) {
        if (debug(player) && debugCount >= 0) return debugCount;
        Swing swing = SWINGS.get(player);
        return swing == null ? 0 : swing.handCount[hand.ordinal()];
    }

    /** Increments every swing; lets alternating animations pick a side. */
    public static int count(Player player) {
        if (debug(player) && debugCount >= 0) return debugCount;
        Swing swing = SWINGS.get(player);
        return swing == null ? 0 : swing.count;
    }

    /** Which hand the current swing belongs to. */
    public static InteractionHand hand(Player player) {
        Swing swing = SWINGS.get(player);
        return swing == null ? InteractionHand.MAIN_HAND : swing.hand;
    }

    /** The current swing came straight out of a guard, as a shield bash does. */
    public static boolean fromGuard(Player player) {
        Swing swing = SWINGS.get(player);
        return swing != null && swing.fromGuard;
    }

    private AttackTiming() {}
}
