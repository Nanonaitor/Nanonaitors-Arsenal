package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.BallAndChainCombat;
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
    private static final Map<EntityPlayer, ReleaseState> RELEASES = new WeakHashMap<>();

    private BallAndChainAnimationHandler() {}

    public static void startReleaseAnimation(int entityId, int charge, float distance,
                                             float yaw, float pitch) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            return;
        }
        Entity entity = minecraft.world.getEntityByID(entityId);
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        WINDUPS.remove(player);
        player.getEntityData().setBoolean("ArsenalBallAndChainActive", true);
        RELEASES.put(player, new ReleaseState(minecraft.world.getTotalWorldTime(),
            Math.max(1, Math.min(3, charge)), Math.max(0.0F, distance), yaw, pitch));
        if (player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
    }

    public static boolean isReleaseAnimationActive(EntityPlayer player) {
        return RELEASES.containsKey(player);
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
            ItemStack held = player.getHeldItemMainhand();
            if (!(held.getItem() instanceof ItemBallAndChain)) {
                player.getEntityData().setBoolean("ArsenalBallAndChainActive", false);
                WINDUPS.remove(player);
                RELEASES.remove(player);
                continue;
            }
            boolean localInput = player == minecraft.player
                && minecraft.currentScreen == null
                && player.getHeldItemOffhand().isEmpty()
                && !RELEASES.containsKey(player)
                && minecraft.gameSettings.keyBindAttack.isKeyDown();
            boolean remoteActive = player != minecraft.player && player.isHandActive()
                && player.getActiveHand() == EnumHand.MAIN_HAND
                && !RELEASES.containsKey(player);
            WindupState state = WINDUPS.get(player);
            if (localInput || remoteActive) {
                player.getEntityData().setBoolean("ArsenalBallAndChainActive", true);
                if (state == null) {
                    state = new WindupState(now);
                    WINDUPS.put(player, state);
                }
                state.endTick = now + 3L;
            }
        }
        Iterator<Map.Entry<EntityPlayer, WindupState>> windupIterator =
            WINDUPS.entrySet().iterator();
        while (windupIterator.hasNext()) {
            Map.Entry<EntityPlayer, WindupState> entry = windupIterator.next();
            if (entry.getValue().endTick < now || entry.getKey().isDead) {
                windupIterator.remove();
            }
        }
        Iterator<Map.Entry<EntityPlayer, ReleaseState>> releaseIterator =
            RELEASES.entrySet().iterator();
        while (releaseIterator.hasNext()) {
            Map.Entry<EntityPlayer, ReleaseState> entry = releaseIterator.next();
            if (now - entry.getValue().startTick > BallAndChainCombat.RELEASE_ANIMATION_TICKS
                || entry.getKey().isDead) {
                EntityPlayer player = entry.getKey();
                releaseIterator.remove();
                if (!WINDUPS.containsKey(player) && player.isHandActive()
                    && player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) {
                    player.resetActiveHand();
                }
                if (!WINDUPS.containsKey(player)) {
                    player.getEntityData().setBoolean("ArsenalBallAndChainActive", false);
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
    }

    private static void renderWindup(EntityPlayer player, WindupState state,
                                     float partialTicks) {
        RenderFrame frame = getFrame(player, partialTicks, player.rotationYaw, 0.0F);
        double age = player.world.getTotalWorldTime() - state.startTick + partialTicks;
        double angle = age / BallAndChainCombat.SWING_INTERVAL_TICKS
            * Math.PI * 2.0D;
        double distance = 0.75D + Math.cos(angle) * 0.45D;
        double horizontal = Math.sqrt(frame.forwardX * frame.forwardX
            + frame.forwardZ * frame.forwardZ);
        double windupX = horizontal > 0.0001D ? frame.forwardX / horizontal : 0.0D;
        double windupZ = horizontal > 0.0001D ? frame.forwardZ / horizontal : 1.0D;
        double ballX = frame.anchorX + windupX * distance;
        double ballY = frame.anchorY + Math.sin(angle) * 0.72D;
        double ballZ = frame.anchorZ + windupZ * distance;
        renderChainAndBall(player, frame, ballX, ballY, ballZ, 0.24D);
    }

    private static void renderRelease(EntityPlayer player, ReleaseState state,
                                      float partialTicks) {
        RenderFrame frame = getFrame(player, partialTicks, state.yaw, state.pitch);
        double age = player.world.getTotalWorldTime() - state.startTick + partialTicks;
        double progress = Math.max(0.0D, Math.min(1.0D,
            age / BallAndChainCombat.RELEASE_ANIMATION_TICKS));
        double distance = state.distance * Math.sin(progress * Math.PI);
        double ballX = frame.anchorX + frame.forwardX * distance;
        double ballY = frame.anchorY + frame.forwardY * distance;
        double ballZ = frame.anchorZ + frame.forwardZ * distance;
        renderChainAndBall(player, frame, ballX, ballY, ballZ,
            0.24D + state.charge * 0.025D);
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
            forwardX, forwardY, forwardZ);
    }

    private static void renderChainAndBall(EntityPlayer player, RenderFrame frame,
                                           double ballX, double ballY, double ballZ,
                                           double radius) {
        WeaponPartRenderer.renderChainAndBall(player.getHeldItemMainhand(),
            frame.anchorX, frame.anchorY, frame.anchorZ,
            ballX, ballY, ballZ, radius);
    }

    private static final class WindupState {
        private final long startTick;
        private long endTick;

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

        private ReleaseState(long startTick, int charge, float distance,
                             float yaw, float pitch) {
            this.startTick = startTick;
            this.charge = charge;
            this.distance = distance;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final class RenderFrame {
        private final double anchorX;
        private final double anchorY;
        private final double anchorZ;
        private final double forwardX;
        private final double forwardY;
        private final double forwardZ;

        private RenderFrame(double anchorX, double anchorY, double anchorZ,
                            double forwardX, double forwardY, double forwardZ) {
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.forwardX = forwardX;
            this.forwardY = forwardY;
            this.forwardZ = forwardZ;
        }
    }
}
