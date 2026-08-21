package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class WeaponPartRenderer {
    public static final String PART_TAG = "ArsenalAnimationPart";
    public static final int CHAIN_LINK_FLAT = 1;
    public static final int BALL = 2;
    public static final int CHAIN_LINK_UPRIGHT = 3;
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
