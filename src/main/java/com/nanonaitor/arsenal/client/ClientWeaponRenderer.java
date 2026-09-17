package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;

public final class ClientWeaponRenderer {
    public static void render(RenderLivingEvent.Post<?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        boolean local = player == minecraft.player;
        int swingTicks = ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem(),
            weapon.kind() == WeaponKind.BALL_AND_CHAIN && local && ClientControls.ballWindBoost());
        if (weapon.kind() == WeaponKind.BLADE_STAFF && AbilityVisualState.staffReflecting(player)) {
            double partial = minecraft.getFrameTime();
            double bodyYaw = Math.toRadians(Mth.rotLerp((float)partial, player.yBodyRotO, player.yBodyRot));
            double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
            // Keep the weapon centered in front of the torso. Only the staff
            // rotates, so it spins on its own midpoint rather than orbiting the arm.
            renderSpinningItem(event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), 0,
                player.getMainHandItem().copy(), -sinBody * 0.72D, 1.15D,
                cosBody * 0.72D, 1.25F, (float)-Math.toDegrees(bodyYaw),
                (float)((now + partial) * 48.0D));
        } else if (weapon.kind() == WeaponKind.FLAIL && (local ? ClientControls.flailActive() : player.isUsingItem())) {
            double angle = -(now + minecraft.getFrameTime()) / swingTicks * Math.PI * 2.0D;
            double reach = ChainWeaponStats.flailReach(player, player.getMainHandItem());
            renderFlailTrails(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                0, weapon.tier(), angle, reach, 0.0D);
            renderChain(event, player.getMainHandItem(), weapon.tier(),
                0.34D, 1.2D, 0.0D, Math.cos(angle) * reach, 1.05D,
                Math.sin(angle) * reach, 0.87F, false);
        }
    }

    /** One world-space attack, including the local player in first person (as in 1.12.2). */
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        var buffers = minecraft.renderBuffers().bufferSource();
        for (var player : minecraft.level.players()) {
            if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
                || weapon.kind() != WeaponKind.BALL_AND_CHAIN || player.isInvisible()) continue;
            boolean local = player == minecraft.player;
            long now = minecraft.level.getGameTime();
            int swingTicks = ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem(),
                local && ClientControls.ballWindBoost());
            var pose = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            float frame = event.getPartialTick();
            pose.pushPose();
            pose.translate(Mth.lerp(frame, player.xOld, player.getX()) - camera.x,
                Mth.lerp(frame, player.yOld, player.getY()) - camera.y,
                Mth.lerp(frame, player.zOld, player.getZ()) - camera.z);
            int light = minecraft.getEntityRenderDispatcher().getPackedLightCoords(player, frame);
            double partial = minecraft.getFrameTime();
            double bodyYaw = Math.toRadians(Mth.rotLerp((float)partial, player.yRotO, player.getYRot()));
            double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
            double handX = -cosBody * 0.28D - sinBody * 0.18D;
            double handY = player.isCrouching() ? 1.05D : 1.25D;
            double handZ = -sinBody * 0.28D + cosBody * 0.18D;
            var synced=player.getMainHandItem().getOrCreateTag();
            boolean remoteThrow=!local && synced.getBoolean("ArsenalBallThrown");
            boolean attackVisual = local
                ? ClientControls.ballWindup(now) || ClientControls.ballRelease(now)
                : customModelFlag(player.getMainHandItem());
            boolean guarding = player.isUsingItem() && player.getUseItem() == player.getMainHandItem()
                && player.getOffhandItem().isEmpty() && !attackVisual;
            if (guarding && !(local && minecraft.options.getCameraType().isFirstPerson())) {
                // Guarding carries only the large ball between both hands. The
                // chain and ordinary held sprite are intentionally hidden.
                renderItem(pose, buffers, light, 0,
                    ballVisualStack(weapon.tier()), -sinBody * 0.28D,
                    1.18D, cosBody * 0.28D, 0.94F);
            } else if ((local && ClientControls.ballWindup(now)) || (!local && attackVisual && !remoteThrow)) {
                double angle = (now - (local ? ClientControls.ballStarted() : synced.getLong("ArsenalBallStarted")) + partial) / swingTicks * Math.PI * 2.0D;
                double scale = ChainWeaponStats.ballWindupReach(player, player.getMainHandItem()) / 3.0D;
                double localBallZ = (0.75D + Math.cos(angle) * 0.45D) * scale;
                renderChain(pose, buffers, light, 0, weapon.tier(),
                    handX, handY, handZ,
                    handX - sinBody * localBallZ, handY + Math.sin(angle) * 0.72D * scale,
                    handZ + cosBody * localBallZ, 0.528F, 0.20F, true);
            } else if ((local && ClientControls.ballRelease(now)) || (remoteThrow && attackVisual)) {
                double progress = (now - (local?ClientControls.ballReleaseStarted():synced.getLong("ArsenalBallRelease")) + partial)
                    / Math.max(1,local?ClientControls.ballReleaseDuration():synced.getInt("ArsenalBallDuration"));
                progress=Math.max(0,Math.min(1,progress));
                double travel = Math.sin(progress * Math.PI);
                double distance = (local?ClientControls.releasedDistance():synced.getDouble("ArsenalBallDistance")) * travel;
                // Match the server's locked outgoing/returning direction.
                Vec3 look = local ? ClientControls.releasedDirection().normalize()
                    : new Vec3(synced.getDouble("ArsenalBallX"),synced.getDouble("ArsenalBallY"),synced.getDouble("ArsenalBallZ"));
                // Anchor tracks the player; flight direction stays fixed after release.
                double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
                if (horizontal > 0.0001D) {
                    handX = look.x / horizontal * 0.18D - look.z / horizontal * 0.28D;
                    handZ = look.z / horizontal * 0.18D + look.x / horizontal * 0.28D;
                }
                renderChain(pose, buffers, light, 0, weapon.tier(),
                    handX, handY, handZ,
                    handX + look.x * distance, handY + look.y * distance, handZ + look.z * distance,
                    0.48F + (local ? ClientControls.releasedCharge() : synced.getInt("ArsenalBallCharge")) * 0.05F,
                    0.20F, true);
            }
            pose.popPose();
        }
        buffers.endBatch();
    }

    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (isDualScimitarGuard(minecraft, event.getItemStack())) {
            // Own this ability's hand render, bypassing vanilla block/swing transforms.
            event.setCanceled(true);
            var pose=event.getPoseStack();
            boolean right=(event.getHand()==InteractionHand.MAIN_HAND)
                == (minecraft.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT);
            pose.pushPose();
            pose.translate(right?0.015D:-0.015D,-0.22D,right?-0.92D:-0.94D);
            if(right)pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.scale(1.25F,1.25F,1.25F);
            submitItem(pose,event.getMultiBufferSource(),event.getPackedLight(),0,event.getItemStack());
            pose.popPose();
            return;
        }
        if (minecraft.player.getMainHandItem().getItem() instanceof ArsenalWeaponItem ball
            && ball.kind()==WeaponKind.BALL_AND_CHAIN && minecraft.player.getOffhandItem().isEmpty()
            && minecraft.player.isUsingItem() && minecraft.player.getUseItem()==minecraft.player.getMainHandItem()
            && !ClientControls.ballWindup(minecraft.level.getGameTime()) && !ClientControls.ballRelease(minecraft.level.getGameTime())) {
            event.setCanceled(true);
            if(event.getHand()==InteractionHand.MAIN_HAND) renderItem(event.getPoseStack(),event.getMultiBufferSource(),
                event.getPackedLight(),0,ballVisualStack(ball.tier()),0,-.52D,-1.05D,1.25F);
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND
            || !(event.getItemStack().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        double partial = event.getPartialTick();
        int swingTicks = ChainWeaponStats.swingIntervalTicks(minecraft.player, event.getItemStack(),
            weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballWindBoost());
        if (weapon.kind() == WeaponKind.BLADE_STAFF && AbilityVisualState.staffReflecting(minecraft.player)) {
            event.setCanceled(true);
            renderSpinningItem(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), 0,
                event.getItemStack().copy(), 0.0D, -0.18D, -1.05D, 1.6875F, 0.0F,
                (float)((now + partial) * 48.0D));
        } else if (weapon.kind() == WeaponKind.MORNING_STAR && ClientControls.morningStarCharging()) {
            float charge = ClientControls.morningStarChargeProgress(now);
            event.getPoseStack().translate(-0.10D, 0.08D + charge * 0.10D, -0.18D);
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-28.0F - charge * 28.0F));
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(8.0F));
        } else if (weapon.kind() == WeaponKind.MORNING_STAR && ClientControls.morningStarSwing(now)) {
            float swing = ClientControls.morningStarSwingProgress(now, (float)partial);
            float eased = Mth.sin(swing * Mth.PI);
            event.getPoseStack().translate(-0.18D, -0.03D, -0.20D);
            event.getPoseStack().mulPose(Axis.YP.rotationDegrees(-105.0F + swing * 210.0F));
            event.getPoseStack().mulPose(Axis.ZP.rotationDegrees(-18.0F * eased));
        } else if (weapon.kind() == WeaponKind.BATTERING_RAM && ClientControls.ramActive()) {
            // A steady forward brace for first person. This replaces the vanilla
            // mining/block animation and adds only a small running pulse, so the
            // pointed end visibly drives forward without competing transforms.
            double pulse = Math.sin((now + partial) * 0.85D);
            event.getPoseStack().translate(-0.08D, -0.08D + Math.abs(pulse) * 0.025D,
                -0.42D - Math.max(0.0D, pulse) * 0.035D);
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-7.0F));
        } else if (weapon.kind() == WeaponKind.FLAIL && ClientControls.flailActive()) {
            // Use the exact same world-space anchor and orbit as third person,
            // then transform both through the live first-person camera. This also
            // accounts for pitch, which the earlier approximation omitted.
            double angle = -(now + partial) / swingTicks * Math.PI * 2.0D;
            double reach = ChainWeaponStats.flailReach(minecraft.player, event.getItemStack());
            Vec3 hand = worldOffsetToCamera(minecraft.player, 0.34D,
                1.20D - minecraft.player.getEyeHeight(), 0.0D, (float)partial);
            Vec3 ball = worldOffsetToCamera(minecraft.player, Math.cos(angle) * reach,
                1.05D - minecraft.player.getEyeHeight(), Math.sin(angle) * reach, (float)partial);
            double gap = Math.min(Math.toRadians(9.0D), 0.42D / Math.max(0.01D, reach));
            // The orbit angle decreases over time, so older positions are at a
            // positive angular offset. Using a negative offset put the echoes
            // ahead of the ball and made them appear to travel backwards.
            Vec3 near = worldOffsetToCamera(minecraft.player, Math.cos(angle + gap) * reach,
                1.05D - minecraft.player.getEyeHeight(), Math.sin(angle + gap) * reach, (float)partial);
            Vec3 far = worldOffsetToCamera(minecraft.player, Math.cos(angle + gap * 1.75D) * reach,
                1.05D - minecraft.player.getEyeHeight(), Math.sin(angle + gap * 1.75D) * reach, (float)partial);
            renderCrossedFlailBall(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), 0,
                flailTrailStack(weapon.tier(), true), near.x, near.y, near.z, 0.51F);
            renderCrossedFlailBall(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), 0,
                flailTrailStack(weapon.tier(), false), far.x, far.y, far.z, 0.33F);
            renderChain(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), 0,
                weapon.tier(), hand.x, hand.y, hand.z, ball.x, ball.y, ball.z,
                0.87F, 0.28F, false);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && minecraft.player.isUsingItem()
            && minecraft.player.getUseItem() == event.getItemStack()
            && minecraft.player.getOffhandItem().isEmpty() && !customModelFlag(event.getItemStack())) {
            // Large and low: only the upper half rises into the first-person view.
            // No chain is rendered while the ball itself is used as a shield.
            renderItem(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), 0,
                ballVisualStack(weapon.tier()), 0.0D, -0.52D, -0.82D, 0.84F);
        }
    }

    private static void renderChain(RenderLivingEvent.Post<?, ?> event, ItemStack held, WeaponTier tier,
            double ax, double ay, double az, double bx, double by, double bz, float ballScale,
            boolean spikedHead) {
        renderChain(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
            0, tier, ax, ay, az, bx, by, bz, ballScale,
            spikedHead ? 0.20F : 0.28F, spikedHead);
    }

    private static void renderChain(PoseStack pose, MultiBufferSource collector, int light, int outline,
            WeaponTier tier, double ax, double ay, double az, double bx, double by, double bz,
            float ballScale, float linkScale, boolean spikedHead) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 0.001D) return;
        int links = Math.max(1, Math.min(512, (int)Math.ceil(length / linkScale)));
        for (int i = 1; i < links; i++) {
            double t = i / (double)links;
            renderLink(pose, collector, light, outline, ax + dx * t, ay + dy * t, az + dz * t,
                dx / length, dy / length, dz / length, linkScale,
                0.0F, true);
        }
        if (spikedHead) {
            renderItem(pose, collector, light, outline, ballVisualStack(tier), bx, by, bz, ballScale);
        } else {
            renderCrossedFlailBall(pose, collector, light, outline,
                flailSpikeStack(tier), bx, by, bz, ballScale);
        }
    }

    private static void renderLink(PoseStack pose, MultiBufferSource collector, int light, int outline,
            double x, double y, double z, double dx, double dy, double dz, float scale, float twist,
            boolean blockModel) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float)dx, (float)dy, (float)dz));
        pose.mulPose(Axis.YP.rotationDegrees(twist));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, new ItemStack(blockModel
            ? ModItems.CHAIN_LINK_UPRIGHT.get() : Items.CHAIN));
        pose.popPose();
    }

    private static void renderItem(PoseStack pose, MultiBufferSource collector, int light, int outline,
            ItemStack stack, double x, double y, double z, float scale) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, stack);
        pose.popPose();
    }

    /** Perpendicular planes give the spike volume without coincident-face flicker. */
    private static void renderCrossedFlailBall(PoseStack pose, MultiBufferSource collector,
            int light, int outline, ItemStack stack, double x, double y, double z, float scale) {
        renderRotatedItem(pose, collector, light, outline, stack, x, y, z, scale, 0.0F);
        renderRotatedItem(pose, collector, light, outline, stack, x, y, z, scale, 90.0F);
    }

    private static void renderRotatedItem(PoseStack pose, MultiBufferSource collector,
            int light, int outline, ItemStack stack, double x, double y, double z,
            float scale, float yaw) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, stack);
        pose.popPose();
    }

    private static void renderSpinningItem(PoseStack pose, MultiBufferSource collector, int light,
            int outline, ItemStack stack, double x, double y, double z, float scale,
            float facingYaw, float angle) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(facingYaw));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, stack);
        pose.popPose();
    }

    private static Vec3 worldOffsetToCamera(net.minecraft.world.entity.player.Player player,
            double worldX, double worldY, double worldZ, float partial) {
        double yaw = Math.toRadians(Mth.rotLerp(partial, player.yRotO, player.getYRot()));
        double pitch = Math.toRadians(Mth.lerp(partial, player.xRotO, player.getXRot()));
        double horizontalX = -worldX * Math.cos(yaw) - worldZ * Math.sin(yaw);
        double depth = worldX * Math.sin(yaw) - worldZ * Math.cos(yaw);
        return new Vec3(horizontalX,
            worldY * Math.cos(pitch) - depth * Math.sin(pitch),
            worldY * Math.sin(pitch) + depth * Math.cos(pitch));
    }

    private static void submitItem(PoseStack pose, MultiBufferSource collector, int light, int outline,
            ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, collector, minecraft.level, 0);
    }

    private static ItemStack materialStack(WeaponTier tier) {
        return new ItemStack(switch (tier) {
            case WOOD -> Items.OAK_PLANKS;
            case STONE -> Items.COBBLESTONE;
            case COPPER -> Items.COPPER_BLOCK;
            case GOLD -> Items.GOLD_BLOCK;
            case IRON -> Items.IRON_BLOCK;
            case DIAMOND -> Items.DIAMOND_BLOCK;
            case NETHERITE -> Items.NETHERITE_BLOCK;
            default -> Items.IRON_BLOCK;
        });
    }
    private static ItemStack ballVisualStack(WeaponTier tier) {
        return new ItemStack(ModItems.BALL_VISUALS.get(tier).get());
    }
    private static ItemStack flailSpikeStack(WeaponTier tier) {
        return new ItemStack(ModItems.FLAIL_SPIKE_VISUALS.get(tier).get());
    }
    private static ItemStack flailTrailStack(WeaponTier tier, boolean near) {
        return new ItemStack((near ? ModItems.FLAIL_SPIKE_TRAIL_NEAR : ModItems.FLAIL_SPIKE_TRAIL_FAR).get(tier).get());
    }

    private static void renderFlailTrails(PoseStack pose, MultiBufferSource collector, int light,
            int outline, WeaponTier tier, double angle, double reach, double yOffset) {
        // About 0.42 blocks of arc spacing leaves each successively smaller
        // echo almost touching the preceding ball without overlapping it.
        double gap = Math.min(Math.toRadians(9.0D), 0.42D / Math.max(0.01D, reach));
        renderCrossedFlailBall(pose, collector, light, outline, flailTrailStack(tier, true),
            Math.cos(angle + gap) * reach, 1.05D + yOffset, Math.sin(angle + gap) * reach, 0.51F);
        renderCrossedFlailBall(pose, collector, light, outline, flailTrailStack(tier, false),
            Math.cos(angle + gap * 1.75D) * reach, 1.05D + yOffset,
            Math.sin(angle + gap * 1.75D) * reach, 0.33F);
    }

    private static boolean customModelFlag(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean("ArsenalActive");
    }

    private static boolean isDualScimitarGuard(Minecraft minecraft, ItemStack stack) {
        if (!com.nanonaitor.arsenal.combat.ParityRules.guarding(minecraft.player)) return false;
        if (!minecraft.player.isUsingItem()
            || !(minecraft.player.getUseItem().getItem() instanceof ArsenalWeaponItem used)
            || used.kind() != WeaponKind.SCIMITAR) return false;
        return minecraft.player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && minecraft.player.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
            && main.kind() == WeaponKind.SCIMITAR && off.kind() == WeaponKind.SCIMITAR;
    }
    private ClientWeaponRenderer() {}
}
