package com.nanonaitor.arsenal.client.pose;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Tracks when each visible player starts a swing, so swing animations can last
 * as long as the weapon's real attack cooldown (20 / attack speed ticks)
 * instead of vanilla's fixed six ticks. Attack speed is a synced attribute, so
 * this works for other players as well as the local one.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class AttackTiming {
    private static final Map<EntityPlayer, Swing> SWINGS = new WeakHashMap<>();

    private AttackTiming() {}

    private static final class Swing {
        boolean wasSwinging;
        int lastProgress;
        long startTick = Long.MIN_VALUE;
        int durationTicks = 6;
        int count;
        EnumHand hand = EnumHand.MAIN_HAND;
        /** Each hand's own latest swing, so paired weapons can swing both at once. */
        final long[] handStart = {Long.MIN_VALUE, Long.MIN_VALUE};
        final int[] handDuration = {6, 6};
        final int[] handCount = {0, 0};
        /** A swing reported directly (see noteSwing) this or last tick: skip re-detecting it. */
        long notedTick = Long.MIN_VALUE;
    }

    /**
     * Report a swing as it happens. Vanilla keeps one swing per player, so a hand
     * swinging right after the other would hide the first from tick detection;
     * paired weapons (claws) report each hand so both animations play together.
     */
    public static void noteSwing(EntityPlayer player, EnumHand hand) {
        if (player.world == null) return;
        Swing swing = SWINGS.get(player);
        if (swing == null) {
            swing = new Swing();
            SWINGS.put(player, swing);
        }
        long now = player.world.getTotalWorldTime();
        int h = hand.ordinal();
        swing.handStart[h] = now;
        swing.handDuration[h] = cooldownTicks(player);
        swing.handCount[h]++;
        swing.startTick = now;
        swing.durationTicks = swing.handDuration[h];
        swing.hand = hand;
        swing.count++;
        swing.notedTick = now;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            SWINGS.clear();
            return;
        }
        long now = mc.world.getTotalWorldTime();
        for (EntityPlayer player : mc.world.playerEntities) {
            Swing swing = SWINGS.get(player);
            if (swing == null) {
                swing = new Swing();
                SWINGS.put(player, swing);
            }
            boolean swinging = player.isSwingInProgress;
            // A new swing either starts from rest or restarts mid-swing (progress drops).
            boolean noted = swing.notedTick >= now - 1L;
            if (swinging && !noted && (!swing.wasSwinging || player.swingProgressInt < swing.lastProgress)) {
                swing.startTick = now - Math.max(0, player.swingProgressInt);
                swing.durationTicks = cooldownTicks(player);
                swing.hand = player.swingingHand == null ? EnumHand.MAIN_HAND : player.swingingHand;
                swing.count++;
                int h = swing.hand.ordinal();
                swing.handStart[h] = swing.startTick;
                swing.handDuration[h] = swing.durationTicks;
                swing.handCount[h]++;
            }
            swing.wasSwinging = swinging;
            swing.lastProgress = player.swingProgressInt;
        }
    }

    /** Ticks for a full attack at this player's current attack speed, 4 to 40. */
    public static int cooldownTicks(EntityPlayer player) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        double value = speed == null ? 4.0D : speed.getAttributeValue();
        return Math.max(4, Math.min(40, (int) Math.round(20.0D / Math.max(0.05D, value))));
    }

    /** Pose capture tool only: a fixed swing progress for the local player (-1 = off). */
    public static float debugPhase = -1.0F;

    /** Swing progress 0..1 timed to attack speed, or -1 when no swing is playing. */
    public static float phase(EntityPlayer player, float partialTicks) {
        if (debugPhase >= 0.0F && player == Minecraft.getMinecraft().player) return debugPhase;
        Swing swing = SWINGS.get(player);
        if (swing == null || swing.startTick == Long.MIN_VALUE || player.world == null) return -1.0F;
        float age = player.world.getTotalWorldTime() - swing.startTick + partialTicks;
        float phase = age / swing.durationTicks;
        return phase >= 0.0F && phase < 1.0F ? phase : -1.0F;
    }

    /** One hand's own swing progress 0..1, or -1: the other hand may be swinging too. */
    public static float phase(EntityPlayer player, float partialTicks, EnumHand hand) {
        if (debugPhase >= 0.0F && player == Minecraft.getMinecraft().player) return debugPhase;
        Swing swing = SWINGS.get(player);
        int h = hand.ordinal();
        if (swing == null || swing.handStart[h] == Long.MIN_VALUE || player.world == null) return -1.0F;
        float age = player.world.getTotalWorldTime() - swing.handStart[h] + partialTicks;
        float phase = age / swing.handDuration[h];
        return phase >= 0.0F && phase < 1.0F ? phase : -1.0F;
    }

    /** One hand's swing count. */
    public static int count(EntityPlayer player, EnumHand hand) {
        Swing swing = SWINGS.get(player);
        return swing == null ? 0 : swing.handCount[hand.ordinal()];
    }

    /** Tick the latest swing started, or Long.MIN_VALUE. */
    public static long startTick(EntityPlayer player) {
        Swing swing = SWINGS.get(player);
        return swing == null ? Long.MIN_VALUE : swing.startTick;
    }

    /** Which hand the current swing belongs to. */
    public static EnumHand hand(EntityPlayer player) {
        Swing swing = SWINGS.get(player);
        return swing == null ? EnumHand.MAIN_HAND : swing.hand;
    }

    /** Increments every swing; lets alternating animations pick a side. */
    public static int count(EntityPlayer player) {
        Swing swing = SWINGS.get(player);
        return swing == null ? 0 : swing.count;
    }
}
