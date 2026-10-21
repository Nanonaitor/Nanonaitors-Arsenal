package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.combat.ChainWeaponStats;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.joml.Quaternionf;

public final class ClientWeaponRenderer {
    public static void render(RenderLivingEvent.Post<?, ?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        var player = minecraft.level.players().stream().min(java.util.Comparator.comparingDouble(candidate -> {
            double dx = event.getState().x - candidate.getX();
            double dy = event.getState().y - candidate.getY();
            double dz = event.getState().z - candidate.getZ();
            return dx * dx + dy * dy + dz * dz;
        })).orElse(null);
        if (player == null) return;
        double dx = event.getState().x - player.getX(), dy = event.getState().y - player.getY(), dz = event.getState().z - player.getZ();
        if (dx * dx + dy * dy + dz * dz > 0.5D) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        boolean local = player == minecraft.player;
        int swingTicks = ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem(),
            weapon.kind() == WeaponKind.BALL_AND_CHAIN && local && ClientControls.ballWindBoost());
        if (weapon.kind() == WeaponKind.BLADE_STAFF && player.isUsingItem()
            && player.getUseItem() == player.getMainHandItem() && player.getOffhandItem().isEmpty()) {
            double partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double bodyYaw = Math.toRadians(Mth.rotLerp((float)partial, player.yBodyRotO, player.yBodyRot));
            double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
            // Keep the weapon centered in front of the torso. Only the staff
            // rotates, so it spins on its own midpoint rather than orbiting the arm.
            renderSpinningItem(event.getPoseStack(), event.getNodeCollector(),
                event.getState().lightCoords, event.getState().outlineColor,
                player.getMainHandItem().copy(), -sinBody * 0.72D, 1.15D,
                cosBody * 0.72D, 1.25F, (float)-Math.toDegrees(bodyYaw),
                (float)((now + partial) * 48.0D));
        } else if (weapon.kind() == WeaponKind.FLAIL && (local ? ClientControls.flailActive() : player.isUsingItem())) {
            float partial=(float)minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double angle = -(now + partial) / swingTicks * Math.PI * 2.0D;
            double reach = ChainWeaponStats.flailReach(player, player.getMainHandItem());
            Vec3 anchor=flailAnchor(player,partial);
            Vec3 head=flailPoint(player,partial,angle,reach);
            renderChain(event, player.getMainHandItem(), weapon.tier(),
                anchor.x,anchor.y,anchor.z,head.x,head.y,head.z,0.87F,false);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN) {
            double partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double bodyYaw = Math.toRadians(Mth.rotLerp((float)partial, player.yBodyRotO, player.yBodyRot));
            double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
            double handX = cosBody * 0.32D - sinBody * 0.12D;
            double handY = 1.2D;
            double handZ = sinBody * 0.32D + cosBody * 0.12D;
            boolean attackVisual = local
                ? ClientControls.ballWindup(now) || ClientControls.ballRelease(now)
                : customModelFlag(player.getMainHandItem());
            boolean guarding = player.isUsingItem() && player.getUseItem() == player.getMainHandItem()
                && player.getOffhandItem().isEmpty() && !attackVisual;
            if (guarding) {
                // Guarding carries only the large ball between both hands. The
                // chain and ordinary held sprite are intentionally hidden.
                renderItem(event.getPoseStack(), event.getNodeCollector(),
                    event.getState().lightCoords, event.getState().outlineColor,
                    ballVisualStack(weapon.tier()), -sinBody * 0.28D,
                    1.18D, cosBody * 0.28D, 0.94F);
            } else if ((local && ClientControls.ballWindup(now)) || (!local && attackVisual)) {
                double angle = (now - (local ? ClientControls.ballStarted() : now - player.getTicksUsingItem()) + partial) / swingTicks * Math.PI * 2.0D;
                double scale = ChainWeaponStats.ballWindupReach(player, player.getMainHandItem()) / 3.0D;
                Vec3 anchor=ballWindupAnchor(player,(float)partial);
                Vec3 ball=ballWindupHead(player,(float)partial,angle,scale);
                renderChain(event, player.getMainHandItem(), weapon.tier(),
                    anchor.x,anchor.y,anchor.z,ball.x,ball.y,ball.z,0.48F,true);
            } else if (local && ClientControls.ballRelease(now)) {
                double progress = (now - ClientControls.ballReleaseStarted() + partial)
                    / ClientControls.ballReleaseDuration();
                double travel = Math.sin(progress * Math.PI);
                double distance = ClientControls.releasedDistance() * travel;
                Vec3 look = ClientControls.releasedDirection();
                Vec3 anchor=ballAnchor(look,player.isCrouching());
                renderChain(event, player.getMainHandItem(), weapon.tier(),
                    anchor.x,anchor.y,anchor.z,
                    anchor.x+look.x*distance,anchor.y+look.y*distance,
                    anchor.z+look.z*distance,
                    0.48F + ClientControls.releasedCharge() * 0.05F, true);
            }
        }
    }

    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        if (isDualScimitarGuard(minecraft, event.getItemStack())) {
            // The dedicated using-item model supplies mirrored transforms for a
            // centered X. Avoid another camera-space transform here.
            return;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND
            || !(event.getItemStack().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        double partial = event.getPartialTick();
        int swingTicks = ChainWeaponStats.swingIntervalTicks(minecraft.player, event.getItemStack(),
            weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballWindBoost());
        if (weapon.kind() == WeaponKind.BLADE_STAFF && ClientControls.bladeStaffReflecting(now)) {
            renderSpinningItem(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
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
            double angle = -(now + partial) / swingTicks * Math.PI * 2.0D;
            double reach = ChainWeaponStats.flailReach(minecraft.player, event.getItemStack());
            Vec3 worldHand=flailAnchor(minecraft.player,(float)partial);
            Vec3 worldBall=flailPoint(minecraft.player,(float)partial,angle,reach);
            Vec3 hand=worldOffsetToCamera(minecraft.player,worldHand.x,
                worldHand.y-minecraft.player.getEyeHeight(),worldHand.z,(float)partial);
            Vec3 ball=worldOffsetToCamera(minecraft.player,worldBall.x,
                worldBall.y-minecraft.player.getEyeHeight(),worldBall.z,(float)partial);
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), hand.x, hand.y, hand.z, ball.x, ball.y, ball.z,
                0.87F, 0.28F, false);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && minecraft.player.isUsingItem()
            && minecraft.player.getUseItem() == event.getItemStack()
            && minecraft.player.getOffhandItem().isEmpty()
            && !ClientControls.ballWindup(now) && !ClientControls.ballRelease(now)) {
            // Large and low: only the upper half rises into the first-person view.
            // No chain is rendered while the ball itself is used as a shield.
            renderItem(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                ballVisualStack(weapon.tier()), 0.0D, -0.52D, -0.82D, 0.84F);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballWindup(now)) {
            double angle = (now - ClientControls.ballStarted() + partial) / swingTicks * Math.PI * 2.0D;
            double scale = ChainWeaponStats.ballWindupReach(minecraft.player, event.getItemStack()) / 3.0D;
            Vec3 worldHand=ballWindupAnchor(minecraft.player,(float)partial);
            Vec3 worldBall=ballWindupHead(minecraft.player,(float)partial,angle,scale);
            Vec3 hand=worldOffsetToCamera(minecraft.player,worldHand.x,
                worldHand.y-minecraft.player.getEyeHeight(),worldHand.z,(float)partial);
            Vec3 ball=worldOffsetToCamera(minecraft.player,worldBall.x,
                worldBall.y-minecraft.player.getEyeHeight(),worldBall.z,(float)partial);
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(),hand.x,hand.y,hand.z,ball.x,ball.y,ball.z,
                0.48F,0.20F,true);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballRelease(now)) {
            double progress = (now - ClientControls.ballReleaseStarted() + partial)
                / ClientControls.ballReleaseDuration();
            double travel = Math.sin(progress * Math.PI);
            double distance = ClientControls.releasedDistance() * travel;
            Vec3 direction=ClientControls.releasedDirection();
            Vec3 worldHand=ballAnchor(direction,minecraft.player.isCrouching());
            Vec3 worldBall=worldHand.add(direction.scale(distance));
            Vec3 hand=worldOffsetToCamera(minecraft.player,worldHand.x,
                worldHand.y-minecraft.player.getEyeHeight(),worldHand.z,(float)partial);
            Vec3 ball=worldOffsetToCamera(minecraft.player,worldBall.x,
                worldBall.y-minecraft.player.getEyeHeight(),worldBall.z,(float)partial);
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(),hand.x,hand.y,hand.z,ball.x,ball.y,ball.z,
                0.48F + ClientControls.releasedCharge() * 0.05F, 0.20F, true);
        }
    }

    private static void renderChain(RenderLivingEvent.Post<?, ?, ?> event, ItemStack held, WeaponTier tier,
            double ax, double ay, double az, double bx, double by, double bz, float ballScale,
            boolean spikedHead) {
        renderChain(event.getPoseStack(), event.getNodeCollector(), event.getState().lightCoords,
            event.getState().outlineColor, tier, ax, ay, az, bx, by, bz, ballScale,
            spikedHead ? 0.20F : 0.28F, spikedHead);
    }

    private static void renderChain(PoseStack pose, SubmitNodeCollector collector, int light, int outline,
            WeaponTier tier, double ax, double ay, double az, double bx, double by, double bz,
            float ballScale, float linkScale, boolean spikedHead) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int links = Math.max(1, (int)Math.ceil(length / 0.28D));
        for (int i = 1; i < links; i++) {
            double t = i / (double)links;
            renderLink(pose, collector, light, outline, ax + dx * t, ay + dy * t, az + dz * t,
                dx / length, dy / length, dz / length, linkScale,
                i % 2 == 0 ? 0.0F : 45.0F, !spikedHead);
        }
        if (spikedHead) {
            renderItem(pose, collector, light, outline, ballVisualStack(tier), bx, by, bz, ballScale);
        } else {
            renderCrossedFlailBall(pose, collector, light, outline,
                flailSpikeStack(tier), bx, by, bz, ballScale);
        }
    }

    private static void renderLink(PoseStack pose, SubmitNodeCollector collector, int light, int outline,
            double x, double y, double z, double dx, double dy, double dz, float scale, float twist,
            boolean blockModel) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float)dx, (float)dy, (float)dz));
        pose.mulPose(Axis.YP.rotationDegrees(twist));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, new ItemStack(blockModel
            ? ModItems.CHAIN_LINK_UPRIGHT.get() : Items.IRON_CHAIN));
        pose.popPose();
    }

    private static void renderItem(PoseStack pose, SubmitNodeCollector collector, int light, int outline,
            ItemStack stack, double x, double y, double z, float scale) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, stack);
        pose.popPose();
    }

    /** Two opposed sprite planes make the tiered spike read consistently from either side. */
    private static void renderCrossedFlailBall(PoseStack pose, SubmitNodeCollector collector,
            int light, int outline, ItemStack stack, double x, double y, double z, float scale) {
        renderRotatedItem(pose, collector, light, outline, stack, x, y, z, scale, 0.0F);
        renderRotatedItem(pose, collector, light, outline, stack, x, y, z, scale, 180.0F);
    }

    private static void renderRotatedItem(PoseStack pose, SubmitNodeCollector collector,
            int light, int outline, ItemStack stack, double x, double y, double z,
            float scale, float yaw) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, stack);
        pose.popPose();
    }

    private static void renderSpinningItem(PoseStack pose, SubmitNodeCollector collector, int light,
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
        double cameraX=-Math.cos(yaw)*worldX-Math.sin(yaw)*worldZ;
        double cameraZ=Math.sin(yaw)*worldX-Math.cos(yaw)*worldZ;
        return new Vec3(cameraX,worldY*Math.cos(pitch)-cameraZ*Math.sin(pitch),
            cameraZ*Math.cos(pitch)+worldY*Math.sin(pitch));
    }

    private static Vec3 flailAnchor(net.minecraft.world.entity.player.Player player,float partial){
        double yaw=Math.toRadians(Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot));
        return new Vec3(-Math.cos(yaw)*.38D,player.isCrouching()?1.0D:1.2D,
            -Math.sin(yaw)*.38D);
    }
    private static Vec3 flailPoint(net.minecraft.world.entity.player.Player player,float partial,
            double angle,double reach){
        double yaw=Math.toRadians(Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot));
        double rightX=-Math.cos(yaw),rightZ=-Math.sin(yaw);
        double forwardX=-Math.sin(yaw),forwardZ=Math.cos(yaw);
        return new Vec3(rightX*Math.cos(angle)*reach+forwardX*Math.sin(angle)*reach,
            player.isCrouching()?.85D:1.05D,
            rightZ*Math.cos(angle)*reach+forwardZ*Math.sin(angle)*reach);
    }
    private static Vec3 ballAnchor(Vec3 forward,boolean crouching){
        double horizontal=Math.sqrt(forward.x*forward.x+forward.z*forward.z);
        double rightX=horizontal>.0001D?-forward.z/horizontal:-1.0D;
        double rightZ=horizontal>.0001D?forward.x/horizontal:0.0D;
        return new Vec3(forward.x*.18D+rightX*.28D,crouching?1.05D:1.25D,
            forward.z*.18D+rightZ*.28D);
    }
    private static Vec3 ballWindupAnchor(net.minecraft.world.entity.player.Player player,float partial){
        double yaw=Math.toRadians(Mth.rotLerp(partial,player.yRotO,player.getYRot()));
        return ballAnchor(new Vec3(-Math.sin(yaw),0.0D,Math.cos(yaw)),player.isCrouching());
    }
    private static Vec3 ballWindupHead(net.minecraft.world.entity.player.Player player,float partial,
            double angle,double scale){
        double yaw=Math.toRadians(Mth.rotLerp(partial,player.yRotO,player.getYRot()));
        Vec3 forward=new Vec3(-Math.sin(yaw),0.0D,Math.cos(yaw));
        Vec3 anchor=ballAnchor(forward,player.isCrouching());
        double distance=(.75D+Math.cos(angle)*.45D)*scale;
        return new Vec3(anchor.x+forward.x*distance,
            anchor.y+Math.sin(angle)*.72D*scale,anchor.z+forward.z*distance);
    }

    private static void submitItem(PoseStack pose, SubmitNodeCollector collector, int light, int outline,
            ItemStack stack) {
        ItemStackRenderState state = new ItemStackRenderState();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.NONE,
            minecraft.level, minecraft.player, stack.hashCode());
        state.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, outline);
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

    private static void renderFlailTrails(PoseStack pose, SubmitNodeCollector collector, int light,
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
        CustomModelData data = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        return !data.flags().isEmpty() && data.flags().get(0);
    }

    private static boolean isDualScimitarGuard(Minecraft minecraft, ItemStack stack) {
        if (!minecraft.player.isUsingItem()
            || !(minecraft.player.getUseItem().getItem() instanceof ArsenalWeaponItem used)
            || used.kind() != WeaponKind.SCIMITAR) return false;
        return minecraft.player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && minecraft.player.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
            && main.kind() == WeaponKind.SCIMITAR && off.kind() == WeaponKind.SCIMITAR;
    }
    private ClientWeaponRenderer() {}
}
