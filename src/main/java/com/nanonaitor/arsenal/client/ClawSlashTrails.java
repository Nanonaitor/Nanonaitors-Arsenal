package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nanonaitor.arsenal.client.pose.WeaponPoses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Air-slash streaks behind a raking claw: three thin, parallel lines (one per
 * blade) that follow the claw tip through the strike and fade out behind it.
 */
public final class ClawSlashTrails {
    /** The claws' blade spacing (2.25 model px at the claws' held scale). */
    private static final double SPACING = 0.14D;
    private static final double WIDTH = 0.022D;

    static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = null;
        PoseStack pose = event.getPoseStack();
        Matrix4f m = pose.last().pose();
        for (Player player : mc.level.players()) {
            // Two claw hands, then a scimitar's tip and mid-blade streaks.
            double[][][] swords = WeaponPoses.scimitarTrail(player, partial);
            for (int t = 0; t < 2 + (swords == null ? 0 : swords.length); t++) {
                boolean claw = t < 2;
                double[][] trail = claw ? WeaponPoses.clawTrail(player, partial, t == 1) : swords[t - 2];
                if (trail == null) continue;
                if (buffer == null) buffer = buffers.getBuffer(RenderType.lightning());
                double fx = Mth.lerp(partial, player.xOld, player.getX()) - camera.x;
                double fy = Mth.lerp(partial, player.yOld, player.getY()) - camera.y;
                double fz = Mth.lerp(partial, player.zOld, player.getZ()) - camera.z;
                for (int i = 0; i + 1 < trail.length; i++) {
                    if (trail[i] == null || trail[i + 1] == null) continue;
                    double[] a = {fx + trail[i][0], fy + trail[i][1], fz + trail[i][2]};
                    double[] b = {fx + trail[i + 1][0], fy + trail[i + 1][1], fz + trail[i + 1][2]};
                    double[] dir = normalize(new double[] {b[0] - a[0], b[1] - a[1], b[2] - a[2]});
                    double[] across = normalize(cross(dir, Math.abs(dir[1]) > 0.95D ? new double[] {1, 0, 0} : new double[] {0, 1, 0}));
                    double[] thick = normalize(cross(dir, across));
                    float alphaA = 0.75F * (1.0F - i / (float)(trail.length - 1));
                    float alphaB = 0.75F * (1.0F - (i + 1) / (float)(trail.length - 1));
                    if (!claw) {
                        // A scimitar leaves one streak at its tip and a fainter one mid-blade.
                        float k = t == 2 ? 0.8F : 0.45F;
                        quad(buffer, m, a, b, across, alphaA * k, alphaB * k);
                        quad(buffer, m, a, b, thick, alphaA * k, alphaB * k);
                        continue;
                    }
                    // Three streaks, one per blade, side by side across the sweep.
                    for (int blade = -1; blade <= 1; blade++) {
                        double o = blade * SPACING;
                        double[] oa = {a[0] + thick[0] * o, a[1] + thick[1] * o, a[2] + thick[2] * o};
                        double[] ob = {b[0] + thick[0] * o, b[1] + thick[1] * o, b[2] + thick[2] * o};
                        quad(buffer, m, oa, ob, across, alphaA, alphaB);
                        quad(buffer, m, oa, ob, thick, alphaA, alphaB);
                    }
                }
            }
        }
        if (buffer != null) buffers.endBatch(RenderType.lightning());
    }

    /** A thin ribbon from a to b, widened along w, fading from alphaA to alphaB (both faces). */
    private static void quad(VertexConsumer buffer, Matrix4f m, double[] a, double[] b, double[] w, float alphaA, float alphaB) {
        double h = WIDTH * 0.5D;
        double[][] p = {{a[0] - w[0] * h, a[1] - w[1] * h, a[2] - w[2] * h}, {a[0] + w[0] * h, a[1] + w[1] * h, a[2] + w[2] * h},
            {b[0] + w[0] * h, b[1] + w[1] * h, b[2] + w[2] * h}, {b[0] - w[0] * h, b[1] - w[1] * h, b[2] - w[2] * h}};
        float[] alpha = {alphaA, alphaA, alphaB, alphaB};
        for (int i = 0; i < 4; i++) v(buffer, m, p[i], alpha[i]);
        for (int i = 3; i >= 0; i--) v(buffer, m, p[i], alpha[i]);
    }

    private static void v(VertexConsumer buffer, Matrix4f m, double[] p, float alpha) {
        buffer.vertex(m, (float)p[0], (float)p[1], (float)p[2]).color(0.92F, 0.95F, 1.0F, alpha).endVertex();
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9D ? new double[] {0, 1, 0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    private ClawSlashTrails() {}
}
