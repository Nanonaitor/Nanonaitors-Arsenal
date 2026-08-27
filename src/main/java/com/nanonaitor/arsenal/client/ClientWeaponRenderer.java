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
        if (weapon.kind() == WeaponKind.FLAIL && (local ? ClientControls.flailActive() : player.isUsingItem())) {
            double angle = -(now + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / swingTicks * Math.PI * 2.0D;
            double reach = ChainWeaponStats.flailReach(player, player.getMainHandItem());
            renderChain(event, player.getMainHandItem(), weapon.tier(),
                0.34D, 1.2D, 0.0D, Math.cos(angle) * reach, 1.05D,
                Math.sin(angle) * reach, 0.58F, false);
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
                double localBallZ = (0.75D + Math.cos(angle) * 0.45D) * scale;
                renderChain(event, player.getMainHandItem(), weapon.tier(),
                    handX, handY, handZ,
                    -sinBody * localBallZ, 1.2D + Math.sin(angle) * 0.72D * scale,
                    cosBody * localBallZ, 0.48F, true);
            } else if (local && ClientControls.ballRelease(now)) {
                double progress = (now - ClientControls.ballReleaseStarted() + partial)
                    / ClientControls.ballReleaseDuration();
                double travel = Math.sin(progress * Math.PI);
                double distance = ClientControls.releasedDistance() * travel;
                // Third person follows the same live aim shown in first person.
                // Turning during the throw now bends the visible path toward the
                // direction used by outgoing/returning collision checks.
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
        if (weapon.kind() == WeaponKind.MORNING_STAR && ClientControls.morningStarCharging()) {
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
            // Camera space mirrors the depth axis, so use the opposite signed
            // angle here to preserve the third-person rig's apparent direction.
            double angle = (now + partial) / swingTicks * Math.PI * 2.0D;
            double cameraYaw = Math.toRadians(Mth.rotLerp((float)partial,
                minecraft.player.yRotO, minecraft.player.getYRot()));
            double relative = angle - cameraYaw;
            // Match the third-person rig's actual four-block horizontal orbit.
            // Convert its world-space circle into camera space instead of drawing
            // the old miniature ellipse directly in front of the screen.
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), 0.38D, -0.42D, -0.15D,
                Math.cos(relative) * ChainWeaponStats.flailReach(minecraft.player, event.getItemStack()), -0.57D,
                -Math.sin(relative) * ChainWeaponStats.flailReach(minecraft.player, event.getItemStack()), 0.58F, 0.28F, false);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && minecraft.player.isUsingItem()
            && minecraft.player.getUseItem() == event.getItemStack()
            && minecraft.player.getOffhandItem().isEmpty() && !customModelFlag(event.getItemStack())) {
            // Large and low: only the upper half rises into the first-person view.
            // No chain is rendered while the ball itself is used as a shield.
            renderItem(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                ballVisualStack(weapon.tier()), 0.0D, -0.52D, -0.82D, 0.84F);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballWindup(now)) {
            double angle = (now - ClientControls.ballStarted() + partial) / swingTicks * Math.PI * 2.0D;
            double scale = ChainWeaponStats.ballWindupReach(minecraft.player, event.getItemStack()) / 3.0D;
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), 0.50D, -0.40D, -0.72D,
                0.04D, -0.12D + Math.sin(angle) * 0.44D * scale,
                -1.18D + Math.cos(angle) * 0.50D * scale, 0.48F, 0.20F, true);
        } else if (weapon.kind() == WeaponKind.BALL_AND_CHAIN && ClientControls.ballRelease(now)) {
            double progress = (now - ClientControls.ballReleaseStarted() + partial)
                / ClientControls.ballReleaseDuration();
            double travel = Math.sin(progress * Math.PI);
            double distance = ClientControls.releasedDistance() * travel;
            // RenderHandEvent is already camera-relative. Applying the camera yaw/pitch
            // again makes the ball rotate twice as fast when the player looks around.
            Vec3 direction = new Vec3(0.0D, 0.0D, -1.0D);
            double anchorX = 0.50D, anchorY = -0.40D, anchorZ = -0.72D;
            renderChain(event.getPoseStack(), event.getNodeCollector(), event.getPackedLight(), 0,
                weapon.tier(), anchorX, anchorY, anchorZ,
                anchorX * (1.0D - travel) + direction.x * distance,
                anchorY * (1.0D - travel) + direction.y * distance,
                anchorZ * (1.0D - travel) + direction.z * distance,
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
        renderItem(pose, collector, light, outline,
            spikedHead ? ballVisualStack(tier) : materialStack(tier), bx, by, bz, ballScale);
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
