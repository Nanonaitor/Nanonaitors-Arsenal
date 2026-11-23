package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.BallAndChainCombat;
import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class BallAndChainAnimationHandler {
    private static final Map<EntityPlayer, WindupState> WINDUPS = new WeakHashMap<>();
    /** The swung and thrown head is drawn a size up from the original. */
    private static final double BALL_SIZE = 1.3D;
    private static final Map<EntityPlayer, ReleaseState> RELEASES = new WeakHashMap<>();

    private BallAndChainAnimationHandler() {}
    public static void cancelForShield(EntityPlayer player) {
        WINDUPS.remove(player);
        RELEASES.remove(player);
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive",false);
    }

    public static void receiveWindup(int entityId) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.world==null)return;
        Entity entity=mc.world.getEntityByID(entityId);
        if(!(entity instanceof EntityPlayer) || entity==mc.player)return;
        EntityPlayer player=(EntityPlayer)entity;
        if(!(player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) || RELEASES.containsKey(player))return;
        long now=mc.world.getTotalWorldTime();
        WindupState state=WINDUPS.get(player);
        if(state==null){state=new WindupState(now);WINDUPS.put(player,state);}
        state.endTick=now+6;
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive",true);
    }

    public static void startReleaseAnimation(int entityId, int charge, float distance,
                                             float yaw, float pitch, int durationTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            return;
        }
        Entity entity = minecraft.world.getEntityByID(entityId);
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (com.nanonaitor.arsenal.combat.AbilityUseRules.activeConventionalShield(player)
            || player == minecraft.player && ShieldUsePriority.weaponAttackSuppressed(player)) {
            cancelForShield(player);
            return;
        }
        WINDUPS.remove(player);
        if (player == minecraft.player) BallAndChainInputHandler.releaseArrived();
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", true);
        RELEASES.put(player, new ReleaseState(minecraft.world.getTotalWorldTime(),
            Math.max(1, Math.min(3, charge)), Math.max(0.0F, distance), yaw, pitch,
            Math.max(3, durationTicks)));
        if (player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
    }

    /** Pose capture tool only: show a wind-up at about this angle this tick. */
    public static void debugWindup(EntityPlayer player, double angle) {
        long now = player.world.getTotalWorldTime();
        int interval = ChainWeaponStats.swingIntervalTicks(player, player.getHeldItemMainhand());
        WindupState state = new WindupState(now - Math.round(angle / (Math.PI * 2.0D) * interval));
        state.endTick = now + 3L;
        RELEASES.remove(player);
        WINDUPS.put(player, state);
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", true);
    }

    /** Pose capture tool only: show a throw at about this progress this tick. */
    public static void debugRelease(EntityPlayer player, double progress) {
        long now = player.world.getTotalWorldTime();
        int duration = 20;
        WINDUPS.remove(player);
        RELEASES.put(player, new ReleaseState(now - Math.round(progress * duration), 2, 5.0F,
            player.rotationYaw, 0.0F, duration));
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", true);
    }

    public static boolean isReleaseAnimationActive(EntityPlayer player) {
        return RELEASES.containsKey(player);
    }

    /**
     * The wind-up spin angle in radians, matching the rendered ball (0 = ball
     * level in front, PI/2 = top), or NaN when this player is not winding up.
     */
    public static double windupAngle(EntityPlayer player, float partialTicks) {
        WindupState state = WINDUPS.get(player);
        if (state == null || RELEASES.containsKey(player)
            || !(player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain)) return Double.NaN;
        return rotations(player, state, player.getHeldItemMainhand(), partialTicks) * Math.PI * 2.0D;
    }

    /** Throw progress 0..1 (out until 0.5, back after), or -1 when no throw is playing. */
    public static double releaseProgress(EntityPlayer player, float partialTicks) {
        ReleaseState state = RELEASES.get(player);
        if (state == null) return -1.0D;
        double age = player.world.getTotalWorldTime() - state.startTick + partialTicks;
        return Math.max(0.0D, Math.min(1.0D, age / state.durationTicks));
    }

    public static boolean isGuarding(EntityPlayer player) {
        return player != null
            && player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain
            && player.getHeldItemOffhand().isEmpty()
            && player.isSneaking()
            && player.isHandActive()
            && player.getActiveHand() == EnumHand.MAIN_HAND
            && !WINDUPS.containsKey(player)
            && !RELEASES.containsKey(player)
            && (player != Minecraft.getMinecraft().player
                || BallAndChainInputHandler.isGuardingInput());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            WINDUPS.clear();
            RELEASES.clear();
            return;
        }
        long now = minecraft.world.getTotalWorldTime();
        for (EntityPlayer player : minecraft.world.playerEntities) {
            if (com.nanonaitor.arsenal.combat.AbilityUseRules.activeConventionalShield(player)) {
                cancelForShield(player);
                continue;
            }
            ItemStack held = player.getHeldItemMainhand();
            if (!(held.getItem() instanceof ItemBallAndChain)) {
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", false);
                WINDUPS.remove(player);
                RELEASES.remove(player);
                continue;
            }
            boolean localInput = player == minecraft.player
                && !ShieldUsePriority.weaponAttackSuppressed(player)
                && minecraft.currentScreen == null
                && !RELEASES.containsKey(player)
                && !BallAndChainInputHandler.isGuardingInput()
                && BallAndChainInputHandler.isSwinging();
            WindupState state = WINDUPS.get(player);
            if (player==minecraft.player && BallAndChainInputHandler.isGuardingInput()
                && !RELEASES.containsKey(player)) {
                WINDUPS.remove(player);
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive",false);
                continue;
            }
            if (localInput) {
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", true);
                if (state == null) {
                    state = new WindupState(now);
                    WINDUPS.put(player, state);
                }
                state.endTick = now + 3L;
            } else if (player == minecraft.player && state != null
                && BallAndChainInputHandler.awaitingRelease() && !RELEASES.containsKey(player)) {
                // Let go: keep spinning until the throw starts, so the swing flows into it.
                state.endTick = now + 1L;
            }
        }
        Iterator<Map.Entry<EntityPlayer, WindupState>> windupIterator =
            WINDUPS.entrySet().iterator();
        while (windupIterator.hasNext()) {
            Map.Entry<EntityPlayer, WindupState> entry = windupIterator.next();
            if (entry.getValue().endTick < now || entry.getKey().isDead) {
                EntityPlayer player = entry.getKey();
                windupIterator.remove();
                if (!RELEASES.containsKey(player))
                    com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", false);
            }
        }
        Iterator<Map.Entry<EntityPlayer, ReleaseState>> releaseIterator =
            RELEASES.entrySet().iterator();
        while (releaseIterator.hasNext()) {
            Map.Entry<EntityPlayer, ReleaseState> entry = releaseIterator.next();
            if (now - entry.getValue().startTick > entry.getValue().durationTicks
                || entry.getKey().isDead) {
                EntityPlayer player = entry.getKey();
                releaseIterator.remove();
                if (!WINDUPS.containsKey(player) && player.isHandActive()
                    && player.getActiveItemStack().getItem() instanceof ItemBallAndChain
                    && player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) {
                    player.resetActiveHand();
                }
                if (!WINDUPS.containsKey(player)) {
                    com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalBallAndChainActive", false);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            return;
        }
        long now = minecraft.world.getTotalWorldTime();
        for (Map.Entry<EntityPlayer, WindupState> entry : WINDUPS.entrySet()) {
            EntityPlayer player = entry.getKey();
            if (entry.getValue().endTick >= now && !player.isDead
                && player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) {
                renderWindup(player, entry.getValue(), event.getPartialTicks());
            }
        }
        for (Map.Entry<EntityPlayer, ReleaseState> entry : RELEASES.entrySet()) {
            EntityPlayer player = entry.getKey();
            if (!player.isDead && player.getHeldItemMainhand().getItem()
                instanceof ItemBallAndChain) {
                renderRelease(player, entry.getValue(), event.getPartialTicks());
            }
        }
        // Guarding uses the tier ball itself as the shield: no chain, and the
        // same world-space model is visible from both first and third person.
        for (EntityPlayer player : minecraft.world.playerEntities) {
            if (!isGuarding(player)) continue;
            if (player == minecraft.player
                && minecraft.gameSettings.thirdPersonView == 0) continue;
            RenderFrame frame = getFrame(player, event.getPartialTicks(),
                player.rotationYaw, player.rotationPitch);
            double bx = frame.anchorX + frame.forwardX * 0.46D;
            double by = frame.anchorY - 0.12D + frame.forwardY * 0.12D;
            double bz = frame.anchorZ + frame.forwardZ * 0.46D;
            double[] right = com.nanonaitor.arsenal.client.pose.WeaponPoses.handOffset(player, event.getPartialTicks(), true);
            double[] left = com.nanonaitor.arsenal.client.pose.WeaponPoses.handOffset(player, event.getPartialTicks(), false);
            if (right != null && left != null) {
                // Held up in front like a shield, as in first person: centred between
                // both hands and pushed a little ahead of them.
                float yaw = player.prevRenderYawOffset
                    + (player.renderYawOffset - player.prevRenderYawOffset) * event.getPartialTicks();
                double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
                bx = frame.feetX + (right[0] + left[0]) * 0.5D + fx * 0.30D;
                by = frame.feetY + (right[1] + left[1]) * 0.5D + 0.05D;
                bz = frame.feetZ + (right[2] + left[2]) * 0.5D + fz * 0.30D;
            }
            // The chain ring faces back toward the hands.
            WeaponPartRenderer.renderBall(player.getHeldItemMainhand(), bx, by, bz, 0.421D * 1.15D,
                -(player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset)
                    * event.getPartialTicks()));
        }
    }

    private static void renderWindup(EntityPlayer player, WindupState state,
                                     float partialTicks) {
        RenderFrame frame = getFrame(player, partialTicks, player.rotationYaw, 0.0F);
        ItemStack weapon = player.getHeldItemMainhand();
        double reachScale = ChainWeaponStats.ballWindupReach(player, weapon)
            / BallAndChainCombat.WINDUP_REACH;
        double angle = rotations(player, state, weapon, partialTicks) * Math.PI * 2.0D;
        double distance = (0.75D + Math.cos(angle) * 0.45D) * reachScale;
        double horizontal = Math.sqrt(frame.forwardX * frame.forwardX
            + frame.forwardZ * frame.forwardZ);
        double windupX = horizontal > 0.0001D ? frame.forwardX / horizontal : 0.0D;
        double windupZ = horizontal > 0.0001D ? frame.forwardZ / horizontal : 1.0D;
        double ballX = frame.anchorX + windupX * distance;
        double ballY = frame.anchorY + Math.sin(angle) * 0.72D * reachScale;
        double ballZ = frame.anchorZ + windupZ * distance;
        renderChainAndBall(player, frame, ballX, ballY, ballZ, 0.264D * BALL_SIZE, partialTicks, 0.0F);
    }

    private static void renderRelease(EntityPlayer player, ReleaseState state,
                                      float partialTicks) {
        RenderFrame frame = getFrame(player, partialTicks, state.yaw, state.pitch);
        double age = player.world.getTotalWorldTime() - state.startTick + partialTicks;
        double progress = Math.max(0.0D, Math.min(1.0D,
            age / state.durationTicks));
        double distance = state.distance * Math.sin(progress * Math.PI);
        double ballX = frame.anchorX + frame.forwardX * distance;
        double ballY = frame.anchorY + frame.forwardY * distance;
        double ballZ = frame.anchorZ + frame.forwardZ * distance;
        double radius = (0.24D + state.charge * 0.025D) * BALL_SIZE;
        if (state.charge >= 3) {
            // A fully charged throw leaves a fading trail behind the ball.
            for (int i = 1; i <= 2; i++) {
                double lag = Math.max(0.0D, progress - i * 0.05D);
                double trail = state.distance * Math.sin(lag * Math.PI);
                WeaponPartRenderer.renderBallTrail(player.getHeldItemMainhand(),
                    frame.anchorX + frame.forwardX * trail, frame.anchorY + frame.forwardY * trail,
                    frame.anchorZ + frame.forwardZ * trail, radius, i == 1 ? 0.35F : 0.18F);
            }
        }
        // The ball tumbles around the chain line in flight.
        renderChainAndBall(player, frame, ballX, ballY, ballZ, radius, partialTicks, (float) (age * 38.0D));
    }

    private static RenderFrame getFrame(EntityPlayer player, float partialTicks,
                                        float yawValue, float pitchValue) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
            - manager.viewerPosX;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
            - manager.viewerPosY;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
            - manager.viewerPosZ;
        double yaw = Math.toRadians(yawValue);
        double pitch = Math.toRadians(pitchValue);
        double pitchCosine = Math.cos(pitch);
        double forwardX = -Math.sin(yaw) * pitchCosine;
        double forwardY = -Math.sin(pitch);
        double forwardZ = Math.cos(yaw) * pitchCosine;
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double anchorX = px + forwardX * 0.18D + rightX * 0.28D;
        double anchorY = py + (player.isSneaking() ? 1.05D : 1.25D);
        double anchorZ = pz + forwardZ * 0.18D + rightZ * 0.28D;
        return new RenderFrame(anchorX, anchorY, anchorZ,
            forwardX, forwardY, forwardZ, px, py, pz);
    }

    /**
     * The ball keeps its own path (the one the server hits along); the chain
     * starts in the posed hand when Arsenal draws the arms, else at the anchor.
     */
    private static void renderChainAndBall(EntityPlayer player, RenderFrame frame,
                                           double ballX, double ballY, double ballZ,
                                           double radius, float partialTicks, float spin) {
        double fromX = frame.anchorX, fromY = frame.anchorY, fromZ = frame.anchorZ;
        double[] hand = com.nanonaitor.arsenal.client.pose.WeaponPoses.weaponHandOffset(player, partialTicks);
        if (hand != null) {
            fromX = frame.feetX + hand[0];
            fromY = frame.feetY + hand[1];
            fromZ = frame.feetZ + hand[2];
        }
        WeaponPartRenderer.renderSpinningChainAndBall(player.getHeldItemMainhand(),
            fromX, fromY, fromZ, ballX, ballY, ballZ, radius, spin);
        // Two-handed (empty off-hand): the other hand holds the slack of the chain.
        double[] other = player.getHeldItemOffhand().isEmpty() && hand != null
            ? com.nanonaitor.arsenal.client.pose.WeaponPoses.handOffset(player, partialTicks, false) : null;
        if (other != null) {
            WeaponPartRenderer.renderSlackChain(player.getHeldItemMainhand(),
                frame.feetX + other[0], frame.feetY + other[1], frame.feetZ + other[2],
                fromX, fromY, fromZ);
        }
    }

    /**
     * Turns spun so far. Holding use speeds the spin up (and letting go slows it):
     * the ball carries on from where it is at the new speed instead of jumping.
     */
    private static double rotations(EntityPlayer player, WindupState state, ItemStack weapon, float partialTicks) {
        int interval = ChainWeaponStats.swingIntervalTicks(player, weapon);
        long now = player.world.getTotalWorldTime();
        if (state.interval == 0) {
            state.interval = interval;
            state.baseTick = state.startTick;
        } else if (interval != state.interval) {
            state.baseRotations += (now - state.baseTick) / (double) state.interval;
            state.baseTick = now;
            state.interval = interval;
        }
        return state.baseRotations + (now - state.baseTick + partialTicks) / state.interval;
    }

    private static final class WindupState {
        private final long startTick;
        private long endTick;
        private int interval;
        private long baseTick;
        private double baseRotations;

        private WindupState(long startTick) {
            this.startTick = startTick;
            this.endTick = startTick + 3L;
        }
    }

    private static final class ReleaseState {
        private final long startTick;
        private final int charge;
        private final float distance;
        private final float yaw;
        private final float pitch;
        private final int durationTicks;

        private ReleaseState(long startTick, int charge, float distance,
                             float yaw, float pitch, int durationTicks) {
            this.startTick = startTick;
            this.charge = charge;
            this.distance = distance;
            this.yaw = yaw;
            this.pitch = pitch;
            this.durationTicks = durationTicks;
        }
    }

    private static final class RenderFrame {
        private final double anchorX;
        private final double anchorY;
        private final double anchorZ;
        private final double forwardX;
        private final double forwardY;
        private final double forwardZ;
        private final double feetX;
        private final double feetY;
        private final double feetZ;

        private RenderFrame(double anchorX, double anchorY, double anchorZ,
                            double forwardX, double forwardY, double forwardZ,
                            double feetX, double feetY, double feetZ) {
            this.feetX = feetX;
            this.feetY = feetY;
            this.feetZ = feetZ;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.forwardX = forwardX;
            this.forwardY = forwardY;
            this.forwardZ = forwardZ;
        }
    }
}
