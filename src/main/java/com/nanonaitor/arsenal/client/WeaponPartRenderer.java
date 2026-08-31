package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;

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
    public static final int CHAIN_LINK_FLAT = 1;
    public static final int BALL = 2;
    public static final int CHAIN_LINK_UPRIGHT = 3;
    public static final int BALL_TRAIL_NEAR = 4;
    public static final int BALL_TRAIL_FAR = 5;
    private static final double LINK_SPACING = 0.28D;

    private WeaponPartRenderer() {}

    public static void renderChainAndBall(ItemStack held,
                                          double anchorX, double anchorY, double anchorZ,
                                          double ballX, double ballY, double ballZ,
                                          double ballRadius) {
        renderChainAndBall(held, anchorX, anchorY, anchorZ,
            ballX, ballY, ballZ, ballRadius, false);
    }

    public static void renderStraightChainAndBall(ItemStack held,
                                                  double anchorX, double anchorY, double anchorZ,
                                                  double ballX, double ballY, double ballZ,
                                                  double ballRadius) {
        renderChainAndBall(held, anchorX, anchorY, anchorZ,
            ballX, ballY, ballZ, ballRadius, true);
    }

    /** Renders only the tier head, used when a Ball & Chain is raised as a shield. */
    public static void renderBall(ItemStack held, double x, double y, double z,
                                  double radius) {
        renderPart(animationPart(held, BALL), x, y, z, radius * 2.0D,
            0.0F, 0.0F, 0.0F);
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

    /** Draws the tier ball as a large first-person guard and no held sprite. */
    public static void renderShieldBall(ItemStack held, EnumHand hand) {
        double side = hand == EnumHand.MAIN_HAND ? 1.0D : -1.0D;
        GlStateManager.pushMatrix();
        GlStateManager.translate(side * 0.16D, -0.78D, -0.72D);
        GlStateManager.rotate(18.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((float)(side * -8.0D), 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(1.10D, 1.10D, 1.10D);
        Minecraft.getMinecraft().getRenderItem().renderItem(
            animationPart(held, BALL), ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }

    public static void renderCarryOnShieldBall(ItemStack held) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.scale(2.35D, 2.35D, 2.35D);
        GlStateManager.translate(0.0D, -0.60D, -1.0D);
        GlStateManager.rotate(8.0F, 1.0F, 0.0F, 0.0F);
        Minecraft.getMinecraft().getRenderItem().renderItem(
            animationPart(held, BALL), ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderBlockingScimitar(ItemStack held, EnumHand hand) {
        double side = hand == EnumHand.MAIN_HAND ? 1.0D : -1.0D;
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.translate(side * 0.18D, -0.25D, -0.86D);
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

    public static void renderWorldHeldItem(ItemStack held, double x, double y,
                                           double z, double scale, float yaw,
                                           float pitch, float roll) {
        renderPart(held, x, y, z, scale, yaw, pitch, roll);
    }

    /** Repeats the standalone chain item along a straight visual tether. */
    public static void renderItemChain(double startX,double startY,double startZ,
                                       double endX,double endY,double endZ,double scale) {
        if (ModContent.IRON_CHAIN_ITEM==null) return;
        double dx=endX-startX,dy=endY-startY,dz=endZ-startZ;
        double length=Math.sqrt(dx*dx+dy*dy+dz*dz);
        int links=Math.max(1,(int)Math.ceil(length/0.24D));
        float yaw=(float)Math.toDegrees(Math.atan2(dx,dz));
        float pitch=(float)Math.toDegrees(Math.atan2(Math.sqrt(dx*dx+dz*dz),dy));
        ItemStack link=new ItemStack(ModContent.IRON_CHAIN_ITEM);
        for (int i=1;i<links;i++) {
            double t=i/(double)links;
            renderPart(link,startX+dx*t,startY+dy*t,startZ+dz*t,
                scale,yaw,pitch,0.0F);
        }
    }

    private static void renderChainAndBall(ItemStack held,
                                           double anchorX, double anchorY, double anchorZ,
                                           double ballX, double ballY, double ballZ,
                                           double ballRadius, boolean straightLinks) {
        ItemStack ball = animationPart(held, BALL);
        double dx = ballX - anchorX;
        double dy = ballY - anchorY;
        double dz = ballZ - anchorZ;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int links = Math.max(1, (int) Math.ceil(length / LINK_SPACING));
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        float tilt = (float) Math.toDegrees(Math.atan2(horizontal, dy));
        for (int index = 1; index < links; index++) {
            double t = index / (double) links;
            // The 1.12 renderer uses a flat item sprite for each link. Rotating
            // alternating sprites by 90 degrees makes the chain appear jagged or
            // partially disappear. Keep every centered link on the same axis so
            // the repeated segments form one clean, straight chain.
            ItemStack chain = animationPart(held, CHAIN_LINK_UPRIGHT);
            renderPart(chain,
                anchorX + dx * t, anchorY + dy * t, anchorZ + dz * t,
                0.26D, yaw, tilt, 0.0F);
        }
        renderPart(ball, ballX, ballY, ballZ, ballRadius * 2.0D,
            yaw, 0.0F, 0.0F);
    }

    private static ItemStack animationPart(ItemStack held, int part) {
        ItemStack stack = new ItemStack(held.getItem());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(PART_TAG, part);
        stack.setTagCompound(tag);
        return stack;
    }

    private static void renderPart(ItemStack stack, double x, double y, double z,
                                   double scale, float yaw, float tilt,
                                   float alternatingRotation) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(tilt, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(alternatingRotation, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(scale, scale, scale);
        Minecraft.getMinecraft().getRenderItem().renderItem(
            stack, ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }
}
