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
        player.getEntityData().setBoolean("ArsenalFlailActive", true);
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
                player.getEntityData().setBoolean("ArsenalFlailActive", false);
                ACTIVE.remove(player);
                continue;
            }
            if (FlailCombat.isBlockingConventionalShield(player)) {
                player.getEntityData().setBoolean("ArsenalFlailActive", false);
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
                player.getEntityData().setBoolean("ArsenalFlailActive", true);
            } else {
                player.getEntityData().setBoolean("ArsenalFlailActive", false);
            }
        }
        Iterator<Map.Entry<EntityPlayer, AnimationState>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityPlayer, AnimationState> entry = iterator.next();
            if (entry.getValue().endTick < now || entry.getKey().isDead) {
                EntityPlayer player = entry.getKey();
                if (!player.isDead && player.getHeldItemMainhand().getItem()
                    instanceof ItemFlail) {
                    player.getEntityData().setBoolean("ArsenalFlailActive", false);
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
        WeaponPartRenderer.renderChainAndBall(player.getHeldItemMainhand(),
            anchorX, anchorY, anchorZ, ballX, ballY, ballZ, 0.30D);
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
