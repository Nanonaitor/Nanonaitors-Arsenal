package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nanonaitor.arsenal.client.pose.WeaponPoses;
import com.nanonaitor.arsenal.combat.BallPath;
import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.combat.TierEffects;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

public final class ClientWeaponRenderer {
    /** Four complete rotations per second throughout the reflection window. */
    private static final double BLADE_STAFF_REFLECT_DEGREES_PER_TICK = 72.0D;
    /** Flail head drawn size, and its lighter chain. */
    private static final float FLAIL_HEAD = 0.87F;
    private static final double FLAIL_CHAIN = 0.7D;
    private static final ItemStack STICK = new ItemStack(Items.STICK);

    public static void render(RenderLivingEvent.Post<?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        float partial = minecraft.getFrameTime();
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (weapon.kind() == WeaponKind.BLADE_STAFF && AbilityVisualState.staffReflecting(player)) {
            double bodyYaw = Math.toRadians(Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot));
            double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
            // Centred in front of the torso; only the staff rotates, on its own midpoint.
            renderSpinningItem(pose, buffers, light, player.getMainHandItem().copy(), -sinBody * 0.72D, 1.15D,
                cosBody * 0.72D, 1.25F * 1.1F, (float)-Math.toDegrees(bodyYaw),
                (float)((now + partial) * BLADE_STAFF_REFLECT_DEGREES_PER_TICK));
        } else if (weapon.kind() == WeaponKind.SCIMITAR && com.nanonaitor.arsenal.combat.ParityRules.guarding(player)
            && !WeaponPoses.enabled()) {
            // With weapon poses on, the crossed arms hold the blades themselves.
            renderCrossedScimitars(pose, buffers, light, player);
        } else if (weapon.kind() == WeaponKind.FLAIL && WeaponVisualState.flailSpinning(player)) {
            renderThirdPersonFlail(pose, buffers, light, player, weapon.tier(), partial);
        }
    }

    /** The flail in third person: whirled from a stick handle, the chain arcing over the head. */
    private static void renderThirdPersonFlail(PoseStack pose, MultiBufferSource buffers, int light, Player player,
                                               WeaponTier tier, float partial) {
        double angle = WeaponVisualState.flailAngle(player, partial);
        double reach = ChainWeaponStats.flailReach(player, player.getMainHandItem());
        renderFlailTrails(pose, buffers, light, tier, angle, reach);
        double[] anchor = {0.34D, player.isCrouching() ? 1.0D : 1.2D, 0.0D};
        boolean leftArm = player.getMainArm() == HumanoidArm.LEFT;
        double[] grip = WeaponPoses.armPoint(player, partial, leftArm, 8.5D);
        double[] tip = WeaponPoses.armPoint(player, partial, leftArm, 19.0D);
        if (grip != null && tip != null) {
            // A short wooden handle sticks out of the whirling hand; the chain hangs from its tip.
            renderStick(pose, buffers, light, grip, tip);
            anchor = tip;
        }
        double[] ball = {Math.cos(angle) * reach, 1.05D, Math.sin(angle) * reach};
        // Keep the chain out of the wielder's head: if the straight line would pass
        // through it, bend the chain over the head on its way out to the ball.
        double[] head = {0.0D, player.isCrouching() ? 1.30D : 1.62D, 0.0D};
        double[] s = sub(ball, anchor);
        double span = dot(s, s);
        double t = span < 1.0E-6D ? 0.0D : Math.max(0.0D, Math.min(1.0D, dot(sub(head, anchor), s) / span));
        double[] closest = {anchor[0] + s[0] * t, anchor[1] + s[1] * t, anchor[2] + s[2] * t};
        double[] off = sub(closest, head);
        double clearance = Math.sqrt(dot(off, off));
        ItemStack spike = flailSpikeStack(tier);
        double radius = FLAIL_HEAD * 5.0D / 16.0D;
        if (t > 0.0D && t < 1.0D && clearance < 0.45D) {
            double lift = 0.45D - clearance + 0.12D;
            double[] via = {closest[0], closest[1] + lift + Math.max(0.0D, -off[1]), closest[2]};
            ChainRenderer.render(pose, buffers, light, tier, spike, FLAIL_CHAIN, anchor, via, toSurface(via, ball, radius));
        } else {
            ChainRenderer.render(pose, buffers, light, tier, spike, FLAIL_CHAIN, anchor, toSurface(anchor, ball, radius));
        }
        renderCrossedFlailBall(pose, buffers, light, spike, ball[0], ball[1], ball[2], FLAIL_HEAD);
    }

    /** One world-space attack, including the local player in first person. */
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            ClawSlashTrails.render(event);
            ArsenalClientFx.render(event);
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        var buffers = minecraft.renderBuffers().bufferSource();
        for (var player : minecraft.level.players()) {
            if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
                || weapon.kind() != WeaponKind.BALL_AND_CHAIN || player.isInvisible()) continue;
            var pose = event.getPoseStack();
            Vec3 camera = event.getCamera().getPosition();
            float frame = event.getPartialTick();
            pose.pushPose();
            pose.translate(Mth.lerp(frame, player.xOld, player.getX()) - camera.x,
                Mth.lerp(frame, player.yOld, player.getY()) - camera.y,
                Mth.lerp(frame, player.zOld, player.getZ()) - camera.z);
            renderBall(pose, buffers, minecraft.getEntityRenderDispatcher().getPackedLightCoords(player, frame),
                player, weapon.tier(), frame);
            pose.popPose();
        }
        buffers.endBatch();
    }

    private static void renderBall(PoseStack pose, MultiBufferSource buffers, int light, Player player,
                                   WeaponTier tier, float partial) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean firstPerson = player == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson();
        double bodyYaw = Math.toRadians(Mth.rotLerp(partial, player.yRotO, player.getYRot()));
        double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
        double[] anchor = {-cosBody * 0.28D - sinBody * 0.18D, player.isCrouching() ? 1.05D : 1.25D,
            -sinBody * 0.28D + cosBody * 0.18D};
        double[] hand = WeaponPoses.handOffset(player, partial, true);
        double[] other = player.getOffhandItem().isEmpty() ? WeaponPoses.handOffset(player, partial, false) : null;
        double windup = WeaponVisualState.ballWindupAngle(player, partial);
        double progress = WeaponVisualState.ballThrowProgress(player, partial);
        ItemStack head = ballVisualStack(tier);
        if (WeaponVisualState.ballGuarding(player) && !firstPerson) {
            // Held up in front like a shield, as in first person: centred between both
            // hands and pushed a little ahead of them; the chain ring faces the hands.
            float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
            double[] at = {-sinBody * 0.28D, 1.18D, cosBody * 0.28D};
            if (hand != null && other != null) at = new double[] {(hand[0] + other[0]) * 0.5D + fx * 0.30D,
                (hand[1] + other[1]) * 0.5D + 0.05D, (hand[2] + other[2]) * 0.5D + fz * 0.30D};
            renderHead(pose, buffers, light, head, at, -yaw, 0.0F, 0.0F, 0.94F * 1.15F);
        } else if (!Double.isNaN(windup)) {
            double scale = ChainWeaponStats.ballWindupReach(player, player.getMainHandItem()) / 3.0D;
            double localBallZ = (0.75D + Math.cos(windup) * 0.45D) * scale;
            double[] ball = {anchor[0] - sinBody * localBallZ, anchor[1] + Math.sin(windup) * 0.72D * scale,
                anchor[2] + cosBody * localBallZ};
            renderChainAndBall(pose, buffers, light, tier, head, hand != null ? hand : anchor, other, ball,
                BallPath.windupModelScale(), 0.0F);
        } else if (progress >= 0.0D) {
            int charge = WeaponVisualState.throwCharge(player);
            double distance = WeaponVisualState.throwDistance(player);
            Vec3 look = WeaponVisualState.throwDirection(player);
            double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            if (horizontal > 0.0001D) {
                anchor[0] = look.x / horizontal * 0.18D - look.z / horizontal * 0.28D;
                anchor[2] = look.z / horizontal * 0.18D + look.x / horizontal * 0.28D;
            }
            float scale = BallPath.throwModelScale(charge);
            if (charge >= TierEffects.maxBallCharges(player, tier)) {
                // A fully charged throw leaves a fading trail behind the ball.
                for (int i = 1; i <= 2; i++) {
                    double trail = BallPath.travel(distance, Math.max(0.0D, progress - i * 0.05D));
                    ChainRenderer.renderGhostBall(pose, buffers, light, head, anchor[0] + look.x * trail,
                        anchor[1] + look.y * trail, anchor[2] + look.z * trail, scale, i == 1 ? 0.35F : 0.18F);
                }
            }
            double travel = BallPath.travel(distance, progress);
            double[] ball = {anchor[0] + look.x * travel, anchor[1] + look.y * travel, anchor[2] + look.z * travel};
            // The ball tumbles around the chain line in flight.
            double age = progress * Math.max(1, ClientControls.ballReleaseDuration());
            renderChainAndBall(pose, buffers, light, tier, head, hand != null ? hand : anchor, other, ball,
                scale, (float)(age * 38.0D));
        }
    }

    /**
     * Chain from the swinging hand to the head (stopping at its surface), the
     * head turned so its chain ring faces the chain, and with an empty off-hand a
     * sagging length of chain held in the other hand.
     */
    private static void renderChainAndBall(PoseStack pose, MultiBufferSource buffers, int light, WeaponTier tier,
                                           ItemStack head, double[] from, double[] other, double[] ball,
                                           float scale, float spin) {
        double radius = scale * 5.0D / 16.0D;
        double[] d = sub(ball, from);
        double length = Math.sqrt(dot(d, d));
        if (length > radius * 0.8D) ChainRenderer.render(pose, buffers, light, tier, head, 1.0D, from, toSurface(from, ball, radius));
        float yaw = (float)Math.toDegrees(Math.atan2(d[0], d[2]));
        float tilt = length < 1.0E-4D ? 0.0F : (float)-Math.toDegrees(Math.asin(Math.max(-1.0D, Math.min(1.0D, d[1] / length))));
        renderHead(pose, buffers, light, head, ball, yaw, tilt, spin, scale);
        if (other != null) {
            double[] span = sub(from, other);
            double sag = 0.10D + Math.sqrt(dot(span, span)) * 0.35D;
            double[][] path = new double[11][];
            for (int i = 0; i <= 10; i++) {
                double t = i / 10.0D;
                path[i] = new double[] {other[0] + span[0] * t, other[1] + span[1] * t - sag * 4.0D * t * (1.0D - t),
                    other[2] + span[2] * t};
            }
            ChainRenderer.render(pose, buffers, light, tier, head, 1.0D, path);
        }
    }

    /** The point on the line from a to b that sits just inside a head of this radius at b. */
    private static double[] toSurface(double[] a, double[] b, double radius) {
        double[] d = sub(b, a);
        double length = Math.sqrt(dot(d, d));
        if (length < 1.0E-4D) return b;
        double k = Math.max(0.0D, length - radius * 0.8D) / length;
        return new double[] {a[0] + d[0] * k, a[1] + d[1] * k, a[2] + d[2] * k};
    }

    /** The head model, turned so its ring (model -Z) points back along yaw/tilt, rolled by spin. */
    private static void renderHead(PoseStack pose, MultiBufferSource buffers, int light, ItemStack head, double[] at,
                                   float yaw, float tilt, float spin, float scale) {
        pose.pushPose();
        pose.translate(at[0], at[1], at[2]);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        pose.scale(scale, scale, scale);
        submitItem(pose, buffers, light, head);
        pose.popPose();
    }

    /** The vanilla stick sprite laid from one point to another (the flail's handle), at item scale. */
    private static void renderStick(PoseStack pose, MultiBufferSource buffers, int light, double[] from, double[] to) {
        double[] d = sub(to, from);
        double length = Math.sqrt(dot(d, d));
        if (length < 1.0E-4D) return;
        float yaw = (float)Math.toDegrees(Math.atan2(d[0], d[2]));
        float pitch = (float)Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, d[1] / length))));
        // The stick is drawn corner to corner, about 14 px on each side (19.8 px diagonal).
        float scale = (float)(length / (19.8D / 16.0D));
        pose.pushPose();
        pose.translate((from[0] + to[0]) * 0.5D, (from[1] + to[1]) * 0.5D, (from[2] + to[2]) * 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F));
        pose.scale(scale, scale, scale);
        submitItem(pose, buffers, light, STICK);
        pose.popPose();
    }

    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        Player player = minecraft.player;
        float bash = ClientControls.scimitarBashPhase(event.getPartialTick());
        boolean dual = isScimitar(player.getMainHandItem()) && isScimitar(player.getOffhandItem());
        if (dual && isScimitar(event.getItemStack()) && (bash >= 0.0F || isDualScimitarGuard(minecraft))) {
            // Own this ability's hand render, bypassing vanilla block/swing transforms.
            event.setCanceled(true);
            renderGuardBlade(event, player, bash);
            return;
        }
        if (player.getMainHandItem().getItem() instanceof ArsenalWeaponItem ball
            && ball.kind() == WeaponKind.BALL_AND_CHAIN && WeaponVisualState.ballGuarding(player)) {
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                var pose = event.getPoseStack();
                pose.pushPose();
                pose.translate(0.0D, -0.52D, -1.05D);
                // The chain ring faces the player.
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                pose.scale(1.25F, 1.25F, 1.25F);
                submitItem(pose, event.getMultiBufferSource(), event.getPackedLight(), ballVisualStack(ball.tier()));
                pose.popPose();
            }
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND
            || !(event.getItemStack().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        float partial = event.getPartialTick();
        var pose = event.getPoseStack();
        if (weapon.kind() == WeaponKind.BLADE_STAFF && AbilityVisualState.staffReflecting(player)) {
            event.setCanceled(true);
            renderSpinningItem(pose, event.getMultiBufferSource(), event.getPackedLight(),
                event.getItemStack().copy(), 0.0D, -0.18D, -1.05D, 1.6875F * 1.1F, 0.0F,
                (float)((now + partial) * BLADE_STAFF_REFLECT_DEGREES_PER_TICK));
        } else if (weapon.kind() == WeaponKind.MORNING_STAR && ClientControls.morningStarCharging()) {
            // Raised up and a little back as the charge builds (eased), highest
            // exactly at full charge, where it trembles, ready to come down.
            float c = ClientControls.morningStarChargeProgress(now, partial);
            float e = c * c * (3.0F - 2.0F * c);
            float shake = c >= 1.0F ? Mth.sin((player.tickCount + partial) * 1.7F) * 1.2F : 0.0F;
            pose.translate(-0.10D - 0.04D * e, 0.02D + 0.34D * e, -0.18D - 0.05D * e);
            pose.mulPose(Axis.XP.rotationDegrees(-6.0F - 24.0F * e + shake));
            pose.mulPose(Axis.ZP.rotationDegrees(8.0F + 10.0F * e));
        } else if (weapon.kind() == WeaponKind.MORNING_STAR && ClientControls.morningStarSwing(now)) {
            float swing = ClientControls.morningStarSwingProgress(now, partial);
            float eased = Mth.sin(swing * Mth.PI);
            pose.translate(-0.18D, -0.03D, -0.20D);
            pose.mulPose(Axis.YP.rotationDegrees(-105.0F + swing * 210.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(-18.0F * eased));
        } else if (weapon.kind() == WeaponKind.BATTERING_RAM && ClientControls.ramActive()) {
            // A steady forward brace with a small running pulse.
            double pulse = Math.sin((now + partial) * 0.85D);
            pose.translate(-0.08D, -0.08D + Math.abs(pulse) * 0.025D, -0.42D - Math.max(0.0D, pulse) * 0.035D);
            pose.mulPose(Axis.XP.rotationDegrees(-7.0F));
        } else if (weapon.kind() == WeaponKind.FLAIL && WeaponVisualState.flailSpinning(player)) {
            renderFirstPersonFlail(event, player, weapon.tier(), partial);
        }
    }

    /**
     * First-person flail: the exact third-person orbit, through the live camera.
     * The chain starts a short way out from the hip toward the head, so it circles
     * the player without ever crossing the view.
     */
    private static void renderFirstPersonFlail(RenderHandEvent event, Player player, WeaponTier tier, float partial) {
        double angle = WeaponVisualState.flailAngle(player, partial);
        double reach = ChainWeaponStats.flailReach(player, event.getItemStack());
        double eye = player.getEyeHeight();
        Vec3 ball = worldOffsetToCamera(player, Math.cos(angle) * reach, 1.05D - eye, Math.sin(angle) * reach, partial);
        double gap = Math.min(Math.toRadians(9.0D), 0.42D / Math.max(0.01D, reach));
        Vec3 near = worldOffsetToCamera(player, Math.cos(angle + gap) * reach, 1.05D - eye, Math.sin(angle + gap) * reach, partial);
        Vec3 far = worldOffsetToCamera(player, Math.cos(angle + gap * 1.75D) * reach, 1.05D - eye,
            Math.sin(angle + gap * 1.75D) * reach, partial);
        var pose = event.getPoseStack();
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        renderCrossedFlailBall(pose, buffers, light, flailTrailStack(tier, true), near.x, near.y, near.z, 0.51F);
        renderCrossedFlailBall(pose, buffers, light, flailTrailStack(tier, false), far.x, far.y, far.z, 0.33F);
        double hx = 0.34D, hy = 1.20D - eye - 0.15D, hz = 0.0D;
        double fx = Math.cos(angle) * reach - hx, fy = 1.05D - eye - hy, fz = Math.sin(angle) * reach - hz;
        double span = Math.sqrt(fx * fx + fy * fy + fz * fz);
        double skip = Math.min(0.85D, span * 0.5D) / Math.max(1.0E-4D, span);
        Vec3 start = worldOffsetToCamera(player, hx + fx * skip, hy + fy * skip, hz + fz * skip, partial);
        double radius = FLAIL_HEAD * 5.0D / 16.0D;
        double[] b = {ball.x, ball.y, ball.z}, a = {start.x, start.y, start.z};
        ChainRenderer.render(pose, buffers, light, tier, flailSpikeStack(tier), FLAIL_CHAIN, a, toSurface(a, b, radius));
        renderCrossedFlailBall(pose, buffers, light, flailSpikeStack(tier), ball.x, ball.y, ball.z, FLAIL_HEAD);
    }

    /**
     * First-person crossed scimitar guard; on a bash both blades uncross and
     * slash outward, then return to the crossed guard.
     */
    private static void renderGuardBlade(RenderHandEvent event, Player player, float bash) {
        boolean right = (event.getHand() == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        double side = right ? 1.0D : -1.0D;
        double out = bash < 0.0F ? 0.0D : Math.sin(Math.min(1.0F, bash / 0.65F) * Math.PI * 0.5D)
            * (bash < 0.65F ? 1.0D : 1.0D - (bash - 0.65F) / 0.35F);
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(right ? 0.015D : -0.015D, -0.22D, right ? -0.92D : -0.94D);
        if (out > 0.0D) {
            pose.translate(side * 0.34D * out, 0.04D * out, -0.06D * out);
            pose.mulPose(Axis.YP.rotationDegrees((float)(-side * 40.0D * out)));
            pose.mulPose(Axis.ZP.rotationDegrees((float)(-side * 60.0D * out)));
        }
        if (right) pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(1.25F, 1.25F, 1.25F);
        submitItem(pose, event.getMultiBufferSource(), event.getPackedLight(), event.getItemStack());
        pose.popPose();
    }

    private static void renderItem(PoseStack pose, MultiBufferSource collector, int light,
            ItemStack stack, double x, double y, double z, float scale) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, stack);
        pose.popPose();
    }

    /** Perpendicular planes give the spike volume without coincident-face flicker. */
    private static void renderCrossedFlailBall(PoseStack pose, MultiBufferSource collector,
            int light, ItemStack stack, double x, double y, double z, float scale) {
        renderRotatedItem(pose, collector, light, stack, x, y, z, scale, 0.0F);
        renderRotatedItem(pose, collector, light, stack, x, y, z, scale, 90.0F);
    }

    private static void renderRotatedItem(PoseStack pose, MultiBufferSource collector,
            int light, ItemStack stack, double x, double y, double z, float scale, float yaw) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, stack);
        pose.popPose();
    }

    private static void renderSpinningItem(PoseStack pose, MultiBufferSource collector, int light,
            ItemStack stack, double x, double y, double z, float scale, float facingYaw, float angle) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(facingYaw));
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, stack);
        pose.popPose();
    }

    private static void renderCrossedScimitars(PoseStack pose, MultiBufferSource collector, int light, Player player) {
        float partial = Minecraft.getInstance().getFrameTime();
        float yaw = -Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        renderGuardBlade(pose, collector, light, player.getMainHandItem().copy(), yaw, -56.0F, true);
        renderGuardBlade(pose, collector, light, player.getOffhandItem().copy(), yaw, 56.0F, false);
    }

    private static void renderGuardBlade(PoseStack pose, MultiBufferSource collector, int light,
            ItemStack stack, float yaw, float roll, boolean mirror) {
        pose.pushPose();
        pose.translate(0.0D, 1.18D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0.0D, 0.0D, 0.48D);
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.scale(mirror ? -0.84F : 0.84F, 0.84F, 0.84F);
        submitItem(pose, collector, light, stack);
        pose.popPose();
    }

    private static Vec3 worldOffsetToCamera(Player player, double worldX, double worldY, double worldZ, float partial) {
        double yaw = Math.toRadians(Mth.rotLerp(partial, player.yRotO, player.getYRot()));
        double pitch = Math.toRadians(Mth.lerp(partial, player.xRotO, player.getXRot()));
        double horizontalX = -worldX * Math.cos(yaw) - worldZ * Math.sin(yaw);
        double depth = worldX * Math.sin(yaw) - worldZ * Math.cos(yaw);
        return new Vec3(horizontalX, worldY * Math.cos(pitch) - depth * Math.sin(pitch),
            worldY * Math.sin(pitch) + depth * Math.cos(pitch));
    }

    private static void submitItem(PoseStack pose, MultiBufferSource collector, int light, ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
            pose, collector, minecraft.level, 0);
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
            WeaponTier tier, double angle, double reach) {
        // About 0.42 blocks of arc spacing leaves each smaller echo almost touching the one before.
        double gap = Math.min(Math.toRadians(9.0D), 0.42D / Math.max(0.01D, reach));
        renderCrossedFlailBall(pose, collector, light, flailTrailStack(tier, true),
            Math.cos(angle + gap) * reach, 1.05D, Math.sin(angle + gap) * reach, 0.51F);
        renderCrossedFlailBall(pose, collector, light, flailTrailStack(tier, false),
            Math.cos(angle + gap * 1.75D) * reach, 1.05D, Math.sin(angle + gap * 1.75D) * reach, 0.33F);
    }

    private static boolean isScimitar(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == WeaponKind.SCIMITAR;
    }

    private static boolean isDualScimitarGuard(Minecraft minecraft) {
        return com.nanonaitor.arsenal.combat.ParityRules.guarding(minecraft.player)
            && minecraft.player.isUsingItem() && isScimitar(minecraft.player.getUseItem());
    }

    private static double[] sub(double[] a, double[] b) { return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }

    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private ClientWeaponRenderer() {}
}
