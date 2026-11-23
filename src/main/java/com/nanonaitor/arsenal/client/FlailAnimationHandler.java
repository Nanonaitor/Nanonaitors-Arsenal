package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.FlailCombat;
import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.item.ItemFlail;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class FlailAnimationHandler {
    private static final Map<EntityPlayer, AnimationState> ACTIVE = new WeakHashMap<>();

    private FlailAnimationHandler() {}

    /**
     * The flail ball's orbit angle in radians, matching the rendered orbit
     * (0 = wielder's right, PI/2 = front), or NaN when the flail is not spinning.
     */
    public static double orbitAngle(EntityPlayer player, float partialTicks) {
        AnimationState state = ACTIVE.get(player);
        if (state == null || player.world == null
            || state.endTick < player.world.getTotalWorldTime()
            || !(player.getHeldItemMainhand().getItem() instanceof ItemFlail)) return Double.NaN;
        double age = player.world.getTotalWorldTime() - state.startTick + partialTicks;
        int interval = ChainWeaponStats.swingIntervalTicks(player, player.getHeldItemMainhand());
        return age / interval * Math.PI * 2.0D;
    }

    /** Pose capture tool only: show the orbit at about this angle this tick. */
    public static void debugOrbit(EntityPlayer player, double angle) {
        long now = player.world.getTotalWorldTime();
        int interval = ChainWeaponStats.swingIntervalTicks(player, player.getHeldItemMainhand());
        ACTIVE.put(player, new AnimationState(now - Math.round(angle / (Math.PI * 2.0D) * interval), now + 2L));
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", true);
    }

    public static void startRemoteSwing(int entityId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            return;
        }
        net.minecraft.entity.Entity entity = minecraft.world.getEntityByID(entityId);
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (player == minecraft.player) {
            return;
        }
        if (!(player.getHeldItemMainhand().getItem() instanceof ItemFlail)) {
            return;
        }
        long now = minecraft.world.getTotalWorldTime();
        int animationTicks = ChainWeaponStats.swingIntervalTicks(player,
            player.getHeldItemMainhand()) + 2;
        AnimationState state = ACTIVE.get(player);
        if (state == null) {
            state = new AnimationState(now, now + animationTicks);
            ACTIVE.put(player, state);
        }
        state.endTick = now + animationTicks;
        com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) {
            ACTIVE.clear();
            return;
        }
        long now = minecraft.world.getTotalWorldTime();
        for (EntityPlayer player : minecraft.world.playerEntities) {
            ItemStack held = player.getHeldItemMainhand();
            if (!(held.getItem() instanceof ItemFlail)) {
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", false);
                ACTIVE.remove(player);
                continue;
            }
            if (FlailCombat.isBlockingConventionalShield(player)) {
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", false);
                ACTIVE.remove(player);
                continue;
            }
            AnimationState state = ACTIVE.get(player);
            boolean localHeldInput = player == minecraft.player
                && minecraft.currentScreen == null
                && minecraft.gameSettings.keyBindAttack.isKeyDown();
            if (localHeldInput) {
                if (state == null) {
                    state = new AnimationState(now, now + 2L);
                    ACTIVE.put(player, state);
                }
                state.endTick = now + 2L;
            }
            if (state != null && state.endTick >= now) {
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", true);
            } else {
                com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", false);
            }
        }
        Iterator<Map.Entry<EntityPlayer, AnimationState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityPlayer, AnimationState> entry = iterator.next();
            if (entry.getValue().endTick < now || entry.getKey().isDead) {
                EntityPlayer player = entry.getKey();
                if (!player.isDead && player.getHeldItemMainhand().getItem()
                    instanceof ItemFlail) {
                    com.nanonaitor.arsenal.data.TransientData.of(player).setBoolean("ArsenalFlailActive", false);
                }
                iterator.remove();
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
        for (Map.Entry<EntityPlayer, AnimationState> entry : ACTIVE.entrySet()) {
            EntityPlayer player = entry.getKey();
            AnimationState state = entry.getValue();
            if (state.endTick >= now && !player.isDead
                && player.getHeldItemMainhand().getItem() instanceof ItemFlail) {
                renderOrbit(player, state, event.getPartialTicks());
            }
        }
    }

    private static void renderOrbit(EntityPlayer player, AnimationState state,
                                    float partialTicks) {
        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
            - manager.viewerPosX;
        double baseY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
            - manager.viewerPosY;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
            - manager.viewerPosZ;
        float yawDegrees = player.prevRenderYawOffset
            + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        double yaw = Math.toRadians(yawDegrees);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double anchorX = px + rightX * 0.38D;
        // Match the modern renderer: the hand sits around 1.20 blocks above
        // the feet and the head orbits at a constant 1.05 blocks. Previously
        // orbitY added another height offset to an already-offset py value,
        // which made the chain climb diagonally upward from the player.
        double anchorY = baseY + (player.isSneaking() ? 1.00D : 1.20D);
        double anchorZ = pz + rightZ * 0.38D;
        // When Arsenal poses the arm, a short wooden handle sticks out of the
        // whirling hand and the chain hangs from its tip.
        double[] grip = com.nanonaitor.arsenal.client.pose.WeaponPoses.weaponArmPoint(player, partialTicks, 8.5D);
        double[] tip = com.nanonaitor.arsenal.client.pose.WeaponPoses.weaponArmPoint(player, partialTicks, 19.0D);
        if (grip != null && tip != null) {
            WeaponPartRenderer.renderStickSprite(px + grip[0], baseY + grip[1], pz + grip[2],
                px + tip[0], baseY + tip[1], pz + tip[2], player.getHeldItemMainhand().hasEffect());
            anchorX = px + tip[0];
            anchorY = baseY + tip[1];
            anchorZ = pz + tip[2];
        }

        double age = player.world.getTotalWorldTime() - state.startTick + partialTicks;
        ItemStack weapon = player.getHeldItemMainhand();
        int interval = ChainWeaponStats.swingIntervalTicks(player, weapon);
        double radius = ChainWeaponStats.flailReach(player, weapon);
        double angle = age / interval * Math.PI * 2.0D;
        double orbitY = baseY + (player.isSneaking() ? 0.85D : 1.05D);
        double ballX = px + rightX * Math.cos(angle) * radius
            + forwardX * Math.sin(angle) * radius;
        double ballY = orbitY;
        double ballZ = pz + rightZ * Math.cos(angle) * radius
            + forwardZ * Math.sin(angle) * radius;

        // Every tier uses the same tested spike-ball silhouette and close-set
        // two-frame afterimage treatment, recolored to its material palette.
        double trailGapAngle = Math.min(Math.toRadians(12.0D),
            0.70D / Math.max(0.01D, radius));
        renderTrail(weapon, px, orbitY, pz, rightX, rightZ,
            forwardX, forwardZ, radius,
            angle - trailGapAngle, 0.18D,
            WeaponPartRenderer.BALL_TRAIL_NEAR);
        renderTrail(weapon, px, orbitY, pz, rightX, rightZ,
            forwardX, forwardZ, radius,
            angle - trailGapAngle * 1.75D, 0.12D,
            WeaponPartRenderer.BALL_TRAIL_FAR);
        // First person: draw only the spinning head. The chain runs from beside the
        // camera and could sweep across the view for a moment.
        Minecraft mc = Minecraft.getMinecraft();
        if (player == mc.getRenderViewEntity() && mc.gameSettings.thirdPersonView == 0) {
            // The chain starts a short way out from the hip, toward the head, so it
            // circles the player without ever crossing the camera.
            double fx = ballX - anchorX, fy = ballY - anchorY, fz = ballZ - anchorZ;
            double reach = Math.sqrt(fx * fx + fy * fy + fz * fz);
            double skip = Math.min(0.85D, reach * 0.5D) / Math.max(1.0E-4D, reach);
            WeaponPartRenderer.renderChainAndBall(player.getHeldItemMainhand(),
                anchorX + fx * skip, anchorY - 0.15D + fy * skip, anchorZ + fz * skip,
                ballX, ballY, ballZ, 0.30D);
            return;
        }
        // Keep the chain out of the wielder's head: if the straight line would pass
        // through it, bend the chain over the head on its way out to the ball.
        double headX = px, headY = baseY + (player.isSneaking() ? 1.30D : 1.62D), headZ = pz;
        double sx = ballX - anchorX, sy = ballY - anchorY, sz = ballZ - anchorZ;
        double span = sx * sx + sy * sy + sz * sz;
        double t = span < 1.0E-6D ? 0.0D : Math.max(0.0D, Math.min(1.0D,
            ((headX - anchorX) * sx + (headY - anchorY) * sy + (headZ - anchorZ) * sz) / span));
        double cx = anchorX + sx * t, cy = anchorY + sy * t, cz = anchorZ + sz * t;
        double ox = cx - headX, oy = cy - headY, oz = cz - headZ;
        double clearance = Math.sqrt(ox * ox + oy * oy + oz * oz);
        if (t > 0.0D && t < 1.0D && clearance < 0.45D) {
            double lift = 0.45D - clearance + 0.12D;
            WeaponPartRenderer.renderChainAndBallVia(player.getHeldItemMainhand(), anchorX, anchorY, anchorZ,
                cx, cy + lift + Math.max(0.0D, -oy), cz, ballX, ballY, ballZ, 0.30D);
        } else {
            WeaponPartRenderer.renderChainAndBall(player.getHeldItemMainhand(),
                anchorX, anchorY, anchorZ, ballX, ballY, ballZ, 0.30D);
        }
    }

    private static void renderTrail(ItemStack weapon, double px, double orbitY,
                                        double pz, double rightX, double rightZ,
                                        double forwardX, double forwardZ,
                                        double radius, double angle,
                                        double ballRadius, int part) {
        double x = px + rightX * Math.cos(angle) * radius
            + forwardX * Math.sin(angle) * radius;
        double z = pz + rightZ * Math.cos(angle) * radius
            + forwardZ * Math.sin(angle) * radius;
        WeaponPartRenderer.renderBallAfterimage(weapon, x, orbitY, z,
            ballRadius, part);
    }

    private static final class AnimationState {
        private final long startTick;
        private long endTick;

        private AnimationState(long startTick, long endTick) {
            this.startTick = startTick;
            this.endTick = endTick;
        }
    }
}
