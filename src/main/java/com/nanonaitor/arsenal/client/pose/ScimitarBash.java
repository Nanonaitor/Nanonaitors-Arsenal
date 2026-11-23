package com.nanonaitor.arsenal.client.pose;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ScimitarShieldCompat;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Times the paired-scimitar shield bash on its own clock.
 *
 * The bash puts both blades on cooldown, which drops the guard at once. Without
 * this the outward double slash stopped after a tick or two and the bash's
 * swing then played as an ordinary main-hand attack. A bash is the swing that
 * starts while (or right after) the pair was guarding; it owns the arms until
 * it finishes, for the local player and everyone else.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ScimitarBash {
    /** Length of the bash animation in ticks. */
    public static final int DURATION = 10;
    private static final Map<EntityPlayer, long[]> STATE = new WeakHashMap<>();

    private ScimitarBash() {}

    /** [0] bash start tick, [1] last tick the pair was guarding. */
    private static long[] state(EntityPlayer player) {
        long[] s = STATE.get(player);
        if (s == null) {
            s = new long[] {Long.MIN_VALUE, Long.MIN_VALUE};
            STATE.put(player, s);
        }
        return s;
    }

    /** The local player just sent a bash. */
    public static void note(EntityPlayer player) {
        if (player.world == null) return;
        state(player)[0] = player.world.getTotalWorldTime();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            STATE.clear();
            return;
        }
        long now = mc.world.getTotalWorldTime();
        for (EntityPlayer player : mc.world.playerEntities) {
            long[] s = state(player);
            if (ScimitarShieldCompat.isGuarding(player)) s[1] = now;
            // Another player's bash arrives as a swing while they guard.
            if (player != mc.player && ScimitarShieldCompat.isPair(player) && player.isSwingInProgress
                && player.swingProgressInt <= 1 && s[1] >= now - 2L && now - s[0] > DURATION) {
                s[0] = now - Math.max(0, player.swingProgressInt);
            }
        }
    }

    /** Pose capture tool only: a fixed bash progress for the local player (-1 = off). */
    public static float debugPhase = -1.0F;

    /** Bash progress 0..1, or -1 when no bash is playing. */
    public static float phase(EntityPlayer player, float partialTicks) {
        if (debugPhase >= 0.0F && player == Minecraft.getMinecraft().player
            && ScimitarShieldCompat.isPair(player)) return debugPhase;
        long[] s = STATE.get(player);
        if (s == null || s[0] == Long.MIN_VALUE || player.world == null
            || !ScimitarShieldCompat.isPair(player)) return -1.0F;
        float p = (player.world.getTotalWorldTime() - s[0] + partialTicks) / DURATION;
        return p >= 0.0F && p < 1.0F ? p : -1.0F;
    }

    /** True while the bash's own swing would otherwise replay as a normal attack. */
    public static boolean ownsSwing(EntityPlayer player) {
        long[] s = STATE.get(player);
        return s != null && s[0] != Long.MIN_VALUE && AttackTiming.startTick(player) <= s[0] + 1L;
    }
}
