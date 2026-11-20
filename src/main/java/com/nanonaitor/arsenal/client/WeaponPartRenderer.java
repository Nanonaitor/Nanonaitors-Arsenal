package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.ItemScimitar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.opengl.GL11;

public final class WeaponPartRenderer {
    public static final String PART_TAG = "ArsenalAnimationPart";
    public static final int BALL = 2;
    public static final int BALL_TRAIL_NEAR = 4;
    public static final int BALL_TRAIL_FAR = 5;

    private WeaponPartRenderer() {}

    public static void renderChainAndBall(ItemStack held,
                                          double anchorX, double anchorY, double anchorZ,
                                          double ballX, double ballY, double ballZ,
                                          double ballRadius) {
        renderChainAndBall(held, anchorX, anchorY, anchorZ,
            ballX, ballY, ballZ, ballRadius, 0.0F);
    }

    public static void renderStraightChainAndBall(ItemStack held,
                                                  double anchorX, double anchorY, double anchorZ,
                                                  double ballX, double ballY, double ballZ,
                                                  double ballRadius) {
        renderChainAndBall(held, anchorX, anchorY, anchorZ,
            ballX, ballY, ballZ, ballRadius, 0.0F);
    }

    /**
     * @param spin roll of the head around the chain line, in degrees: a thrown
     *             ball tumbles while its ring keeps facing the chain
     */
    public static void renderSpinningChainAndBall(ItemStack held,
                                                  double anchorX, double anchorY, double anchorZ,
                                                  double ballX, double ballY, double ballZ,
                                                  double ballRadius, float spin) {
        renderChainAndBall(held, anchorX, anchorY, anchorZ,
            ballX, ballY, ballZ, ballRadius, spin);
    }

    /** A see-through copy of the head trailing a fully charged throw. */
    public static void renderBallTrail(ItemStack held, double x, double y, double z,
                                       double radius, float opacity) {
        ChainRenderer.renderGhostBall(animationPart(held, BALL), x, y, z, radius * 2.0D, opacity);
    }

    /** Renders only the tier head, used when a Ball & Chain is raised as a shield. */
    public static void renderBall(ItemStack held, double x, double y, double z,
                                  double radius) {
        renderBall(held, x, y, z, radius, 0.0F);
    }

    /** @param yaw turns the head's chain ring to face this way (degrees, as entity yaw) */
    public static void renderBall(ItemStack held, double x, double y, double z,
                                  double radius, float yaw) {
        renderPart(animationPart(held, BALL), x, y, z, radius * 2.0D,
            yaw, 0.0F, 0.0F);
    }

    /** Renders a baked-alpha ball-only echo without writing to the depth buffer. */
    public static void renderBallAfterimage(ItemStack held, double x, double y,
                                             double z, double radius, int part) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01F);
        GlStateManager.depthMask(false);
        renderPart(animationPart(held, part), x, y, z, radius * 2.0D,
            0.0F, 0.0F, 0.0F);
        GlStateManager.depthMask(true);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.disableBlend();
    }

    public static void renderCarryOnShieldBall(ItemStack held) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate(0.0D, -0.52D, -1.05D);
        GlStateManager.scale(1.25D, 1.25D, 1.25D);
        GlStateManager.rotate(8.0F, 1.0F, 0.0F, 0.0F);
        Minecraft.getMinecraft().getRenderItem().renderItem(
            animationPart(held, BALL), ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderBlockingScimitar(ItemStack held, EnumHand hand) {
        renderBlockingScimitar(held, hand, -1.0F);
    }

    /**
     * @param bash shield-bash progress 0..1 (or -1): both blades uncross and
     *             slash outward, then return to the crossed guard.
     */
    public static void renderBlockingScimitar(ItemStack held, EnumHand hand, float bash) {
        double side = hand == EnumHand.MAIN_HAND ? 1.0D : -1.0D;
        double out = bash < 0.0F ? 0.0D : Math.sin(Math.min(1.0F, bash / 0.65F) * Math.PI * 0.5D)
            * (bash < 0.65F ? 1.0D : 1.0D - (bash - 0.65F) / 0.35F);
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate(side * 0.18D, -0.25D, -0.86D);
        if (out > 0.0D) {
            GlStateManager.translate(side * 0.34D * out, 0.04D * out, -0.06D * out);
            GlStateManager.rotate((float) (-side * 40.0D * out), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate((float) (side * 60.0D * out), 0.0F, 0.0F, 1.0F);
        }
        GlStateManager.rotate((float)(-side * 66.0D), 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        if (hand == EnumHand.MAIN_HAND) {
            GlStateManager.disableCull();
            GlStateManager.scale(-1.0D, 1.0D, 1.0D);
        }
        double scale = usesCompactScimitarArt(held) ? 0.79D : 1.15D;
        GlStateManager.scale(scale, scale, scale);
        Minecraft.getMinecraft().getRenderItem().renderItem(
            held, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
        GlStateManager.enableCull();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderChargingMorningStar(ItemStack held, float charge) {
        float clamped = Math.max(0.0F, Math.min(1.0F, charge));
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.30D, -0.34D + clamped * 0.25D,
            -0.72D + clamped * 0.16D);
        GlStateManager.rotate(-18.0F - clamped * 72.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(18.0F + clamped * 18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(0.85D, 0.85D, 0.85D);
        Minecraft.getMinecraft().getRenderItem().renderItem(
            held, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }

    private static boolean usesCompactScimitarArt(ItemStack held) {
        // Every recolor uses the exact same 16x16 silhouette and compact model.
        // Treat every tier identically in the custom blocking renderer as well.
        return held.getItem() instanceof ItemScimitar;
    }

    /**
     * A chain that bends around a point on its way to the head (the flail's chain
     * passing over the wielder's head instead of through it).
     */
    public static void renderChainAndBallVia(ItemStack held,
                                             double anchorX, double anchorY, double anchorZ,
                                             double viaX, double viaY, double viaZ,
                                             double ballX, double ballY, double ballZ,
                                             double ballRadius) {
        ItemStack ball = animationPart(held, BALL);
        double dx = ballX - viaX, dy = ballY - viaY, dz = ballZ - viaZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double k = length < 1.0E-4D ? 0.0D : Math.max(0.0D, length - ballRadius * 0.8D) / length;
        ChainRenderer.render(ball, new double[][] {{anchorX, anchorY, anchorZ}, {viaX, viaY, viaZ},
            {viaX + dx * k, viaY + dy * k, viaZ + dz * k}});
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float tilt = length < 1.0E-4D ? 0.0F
            : (float) -Math.toDegrees(Math.asin(Math.max(-1.0D, Math.min(1.0D, dy / length))));
        renderPart(ball, ballX, ballY, ballZ, ballRadius * 2.0D, yaw, tilt, 0.0F);
    }

    private static void renderChainAndBall(ItemStack held,
                                           double anchorX, double anchorY, double anchorZ,
                                           double ballX, double ballY, double ballZ,
                                           double ballRadius, float spin) {
        ItemStack ball = animationPart(held, BALL);
        double dx = ballX - anchorX;
        double dy = ballY - anchorY;
        double dz = ballZ - anchorZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        // Tip the head so its chain ring (model -Z) points back along the chain.
        float tilt = length < 1.0E-4D ? 0.0F
            : (float) -Math.toDegrees(Math.asin(Math.max(-1.0D, Math.min(1.0D, dy / length))));
        // Stop the chain at the ball's surface so links do not poke through it.
        double reach = Math.max(0.0D, length - ballRadius * 0.8D);
        if (length > 1.0E-4D && reach > 0.0D) {
            double k = reach / length;
            ChainRenderer.render(ball, new double[][] {
                {anchorX, anchorY, anchorZ}, {anchorX + dx * k, anchorY + dy * k, anchorZ + dz * k}});
        }
        renderPart(ball, ballX, ballY, ballZ, ballRadius * 2.0D,
            yaw, tilt, spin);
    }

    /**
     * A loose length of chain between two points (the hand holding the slack and
     * the swinging hand), sagging under its own weight.
     */
    public static void renderSlackChain(ItemStack held, double fromX, double fromY, double fromZ,
                                        double toX, double toY, double toZ) {
        double dx = toX - fromX, dy = toY - fromY, dz = toZ - fromZ;
        double span = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double sag = 0.10D + span * 0.35D;
        int segments = 10;
        double[][] path = new double[segments + 1][];
        for (int i = 0; i <= segments; i++) {
            double t = i / (double) segments;
            path[i] = new double[] {fromX + dx * t, fromY + dy * t - sag * 4.0D * t * (1.0D - t), fromZ + dz * t};
        }
        ChainRenderer.render(animationPart(held, BALL), path);
    }

    private static final ItemStack STICK = new ItemStack(net.minecraft.init.Items.STICK);

    /**
     * The vanilla stick sprite laid from one camera-relative point to another
     * (the flail's handle): its diagonal runs along the line, at the usual item scale.
     */
    public static void renderStickSprite(double fromX, double fromY, double fromZ,
                                         double toX, double toY, double toZ) {
        double dx = toX - fromX, dy = toY - fromY, dz = toZ - fromZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-4D) return;
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float pitch = (float) Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, dy / length))));
        // The stick is drawn corner to corner, about 14 px on each side (19.8 px diagonal).
        double scale = length / (19.8D / 16.0D);
        GlStateManager.pushMatrix();
        GlStateManager.translate((fromX + toX) * 0.5D, (fromY + toY) * 0.5D, (fromZ + toZ) * 0.5D);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);   // then turn toward the tip
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F); // tip +Y over toward it
        GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F); // sprite diagonal onto +Y
        GlStateManager.scale(scale, scale, scale);
        ChainRenderer.applyWorldLight(fromX, fromY, fromZ);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(STICK, ItemCameraTransforms.TransformType.NONE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    /** Render-only part stacks, built once per item and part instead of every frame. */
    private static final java.util.Map<net.minecraft.item.Item, java.util.Map<Integer, ItemStack>> PART_STACKS =
        new java.util.IdentityHashMap<>();

    private static ItemStack animationPart(ItemStack held, int part) {
        return PART_STACKS.computeIfAbsent(held.getItem(), item -> new java.util.HashMap<>())
            .computeIfAbsent(part, key -> {
                ItemStack stack = new ItemStack(held.getItem());
                NBTTagCompound tag = new NBTTagCompound();
                tag.setInteger(PART_TAG, key);
                stack.setTagCompound(tag);
                return stack;
            });
    }

    private static void renderPart(ItemStack stack, double x, double y, double z,
                                   double scale, float yaw, float tilt,
                                   float roll) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(tilt, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(roll, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(scale, scale, scale);
        // Shade the faces like a held item and take the light level where it is,
        // instead of drawing flat full-bright (which washed pale metals out to white).
        ChainRenderer.applyWorldLight(x, y, z);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(
            stack, ItemCameraTransforms.TransformType.NONE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}
