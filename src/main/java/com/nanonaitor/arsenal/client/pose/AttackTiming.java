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
            if (swinging && (!swing.wasSwinging || player.swingProgressInt < swing.lastProgress)) {
                swing.startTick = now - Math.max(0, player.swingProgressInt);
                swing.durationTicks = cooldownTicks(player);
                swing.hand = player.swingingHand == null ? EnumHand.MAIN_HAND : player.swingingHand;
                swing.count++;
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

    /** Swing progress 0..1 timed to attack speed, or -1 when no swing is playing. */
    public static float phase(EntityPlayer player, float partialTicks) {
        Swing swing = SWINGS.get(player);
        if (swing == null || swing.startTick == Long.MIN_VALUE || player.world == null) return -1.0F;
        float age = player.world.getTotalWorldTime() - swing.startTick + partialTicks;
        float phase = age / swing.durationTicks;
        return phase >= 0.0F && phase < 1.0F ? phase : -1.0F;
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
