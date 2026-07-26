package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
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
        if (weapon.kind() == WeaponKind.FLAIL && (local ? ClientControls.flailActive() : player.isUsingItem())) {
            double angle = -(now + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 25.0D * Math.PI * 2.0D;
            renderChain(event, player.getMainHandItem(), weapon.tier(),
                0.34D, 1.2D, 0.0D, Math.cos(angle) * 4.0D, 1.05D, Math.sin(angle) * 4.0D, 0.58F, false);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN) {
            double partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            double bodyYaw = Math.toRadians(Mth.rotLerp((float)partial, player.yBodyRotO, player.yBodyRot));
            double sinBody = Math.sin(bodyYaw), cosBody = Math.cos(bodyYaw);
            double handX = cosBody * 0.32D - sinBody * 0.12D;
            double handY = 1.2D;
            double handZ = sinBody * 0.32D + cosBody * 0.12D;
            if (local && ClientControls.ballWindup(now) || !local && player.isUsingItem()) {
                double angle = (now - (local ? ClientControls.ballStarted() : now - player.getTicksUsingItem()) + partial) / 25.0D * Math.PI * 2.0D;
                double localBallZ = 0.75D + Math.cos(angle) * 0.45D;
                renderChain(event, player.getMainHandItem(), weapon.tier(),
                    handX, handY, handZ,
                    -sinBody * localBallZ, 1.2D + Math.sin(angle) * 0.72D,
                    cosBody * localBallZ, 0.48F, true);
            } else if (local && ClientControls.ballRelease(now)) {
                double progress = (now - ClientControls.ballReleaseStarted() + partial) / 16.0D;
                double travel = Math.sin(progress * Math.PI);
                double distance = ClientControls.releasedDistance() * travel;
                Vec3 look = player.getViewVector((float)partial);
                renderChain(event, player.getMainHandItem(), weapon.tier(),
                    handX, handY, handZ,
                    handX * (1.0D - travel) + look.x * distance,
                    handY + (player.getEyeHeight() - handY) * travel + look.y * distance,
                    handZ * (1.0D - travel) + look.z * distance,
                    0.48F + ClientControls.releasedCharge() * 0.05F, true);
            }
        }
    }

    public static void renderFirstPerson(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getHand() != InteractionHand.MAIN_HAND || minecraft.player == null || minecraft.level == null
            || !(event.getItemStack().getItem() instanceof ArsenalWeaponItem weapon)) return;
        long now = minecraft.level.getGameTime();
        double partial = event.getPartialTick();
        if (weapon.kind() == WeaponKind.BATTERING_RAM && ClientControls.ramActive()) {
            // A steady forward brace for first person. This replaces the vanilla
            // mining/block animation and adds only a small running pulse, so the
            // pointed end visibly drives forward without competing transforms.
            double pulse = Math.sin((now + partial) * 0.85D);
            event.getPoseStack().translate(-0.08D, -0.08D + Math.abs(pulse) * 0.025D,
                -0.42D - Math.max(0.0D, pulse) * 0.035D);
            event.getPoseStack().mulPose(Axis.XP.rotationDegrees(-7.0F));
        } else if (weapon.kind() == WeaponKind.FLAIL && ClientControls.flailActive()) {
            // Camera space mirrors the depth axis, so use the opposite signed
            // angle here to preserve the third-person rig's apparent direction.
            double angle = (now + partial) / 25.0D * Math.PI * 2.0D;
            double cameraYaw = Math.toRadians(Mth.rotLerp((float)partial,
                minecraft.player.yRotO, minecraft.player.getYRot()));
            double relative = angle - cameraYaw;
            // Match the third-person rig's actual four-block horizontal orbit.
            // Convert its world-space circle into camera space instead of drawing
            // the old miniature ellipse directly in front of the screen.
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), 0.38D, -0.42D, -0.15D,
                Math.cos(relative) * 4.0D, -0.57D,
                -Math.sin(relative) * 4.0D, 0.58F, 0.20F, false);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballWindup(now)) {
            double angle = (now - ClientControls.ballStarted() + partial) / 25.0D * Math.PI * 2.0D;
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), 0.50D, -0.40D, -0.72D,
                0.04D, -0.12D + Math.sin(angle) * 0.44D,
                -1.18D + Math.cos(angle) * 0.50D, 0.22F, 0.12F, true);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballRelease(now)) {
            double progress = (now - ClientControls.ballReleaseStarted() + partial) / 16.0D;
            double distance = ClientControls.releasedDistance() * Math.sin(progress * Math.PI);
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), 0.50D, -0.40D, -0.72D,
                0.0D, -0.08D, -1.05D - distance, 0.20F, 0.12F, true);
        }
    }

    private static void renderChain(RenderLivingEvent.Post<?, ?, ?> event, ItemStack held, WeaponTier tier,
            double ax, double ay, double az, double bx, double by, double bz, float ballScale,
            boolean spikedHead) {
        renderChain(event.getPoseStack(), event.getNodeCollector(), event.getState().lightCoords,
            event.getState().outlineColor, tier, ax, ay, az, bx, by, bz, ballScale, 0.20F,
            spikedHead);
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
                dx / length, dy / length, dz / length, linkScale, i % 2 == 0 ? 0.0F : 90.0F);
        }
        renderItem(pose, collector, light, outline,
            spikedHead ? ballVisualStack(tier) : materialStack(tier), bx, by, bz, ballScale);
    }

    private static void renderLink(PoseStack pose, SubmitNodeCollector collector, int light, int outline,
            double x, double y, double z, double dx, double dy, double dz, float scale, float twist) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float)dx, (float)dy, (float)dz));
        pose.mulPose(Axis.YP.rotationDegrees(twist));
        pose.scale(scale, scale, scale);
        submitItem(pose, collector, light, outline, new ItemStack(Items.IRON_CHAIN));
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
    private ClientWeaponRenderer() {}
}
