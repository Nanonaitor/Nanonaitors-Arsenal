package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.pose.WeaponPoses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Air-slash streaks behind a raking claw: three thin, parallel lines (one per
 * blade) that follow the claw tip through the strike and fade out behind it.
 * Scimitar swings get the same streaks along the blade's tip and middle.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ClawSlashTrails {
    /** The claws' blade spacing (2.25 model px at the claws' held scale). */
    private static final double SPACING = 0.14D;
    private static final double WIDTH = 0.022D;

    private ClawSlashTrails() {}

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        float partial = event.getPartialTicks();
        RenderManager manager = mc.getRenderManager();
        boolean begun = false;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (EntityPlayer player : mc.world.playerEntities) {
          double[][][] swords = WeaponPoses.scimitarTrail(player, partial);
          for (int t = 0; t < 2 + (swords == null ? 0 : swords.length); t++) {
            boolean claw = t < 2;
            double[][] trail = claw ? WeaponPoses.clawTrail(player, partial, t == 1) : swords[t - 2];
            if (trail == null) continue;
            if (!begun) {
                begun = true;
                GlStateManager.disableTexture2D();
                GlStateManager.disableLighting();
                GlStateManager.disableCull();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                GlStateManager.depthMask(false);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            }
            double fx = player.lastTickPosX + (player.posX - player.lastTickPosX) * partial - manager.viewerPosX;
            double fy = player.lastTickPosY + (player.posY - player.lastTickPosY) * partial - manager.viewerPosY;
            double fz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partial - manager.viewerPosZ;
            for (int i = 0; i + 1 < trail.length; i++) {
                if (trail[i] == null || trail[i + 1] == null) continue;
                double[] a = {fx + trail[i][0], fy + trail[i][1], fz + trail[i][2]};
                double[] b = {fx + trail[i + 1][0], fy + trail[i + 1][1], fz + trail[i + 1][2]};
                double[] dir = normalize(new double[] {b[0] - a[0], b[1] - a[1], b[2] - a[2]});
                double[] across = normalize(cross(dir, Math.abs(dir[1]) > 0.95D ? new double[] {1, 0, 0} : new double[] {0, 1, 0}));
                double[] thick = normalize(cross(dir, across));
                float alphaA = 0.75F * (1.0F - i / (float) (trail.length - 1));
                float alphaB = 0.75F * (1.0F - (i + 1) / (float) (trail.length - 1));
                if (!claw) {
                    // A scimitar leaves one streak at its tip and a fainter one mid-blade.
                    float k = t == 2 ? 0.8F : 0.45F;
                    quad(buffer, a, b, across, alphaA * k, alphaB * k);
                    quad(buffer, a, b, thick, alphaA * k, alphaB * k);
                    continue;
                }
                // Three streaks, one per blade, spread across the sweep the way the
                // blades sit side by side (stacked, not trailing one behind another).
                for (int blade = -1; blade <= 1; blade++) {
                    double o = blade * SPACING;
                    double[] oa = {a[0] + thick[0] * o, a[1] + thick[1] * o, a[2] + thick[2] * o};
                    double[] ob = {b[0] + thick[0] * o, b[1] + thick[1] * o, b[2] + thick[2] * o};
                    quad(buffer, oa, ob, across, alphaA, alphaB);
                    quad(buffer, oa, ob, thick, alphaA, alphaB);
                }
            }
          }
        }
        if (!begun) return;
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
    }

    /** A thin ribbon from a to b, widened along w, fading from alphaA to alphaB. */
    private static void quad(BufferBuilder buffer, double[] a, double[] b, double[] w, float alphaA, float alphaB) {
        double h = WIDTH * 0.5D;
        buffer.pos(a[0] - w[0] * h, a[1] - w[1] * h, a[2] - w[2] * h).color(0.92F, 0.95F, 1.0F, alphaA).endVertex();
        buffer.pos(a[0] + w[0] * h, a[1] + w[1] * h, a[2] + w[2] * h).color(0.92F, 0.95F, 1.0F, alphaA).endVertex();
        buffer.pos(b[0] + w[0] * h, b[1] + w[1] * h, b[2] + w[2] * h).color(0.92F, 0.95F, 1.0F, alphaB).endVertex();
        buffer.pos(b[0] - w[0] * h, b[1] - w[1] * h, b[2] - w[2] * h).color(0.92F, 0.95F, 1.0F, alphaB).endVertex();
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return l < 1.0E-9D ? new double[] {0, 1, 0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
