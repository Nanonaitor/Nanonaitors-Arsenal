package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/**
 * Draws chains as real 3D links: oval rings that alternate by 90 degrees along
 * the chain, the way modern Minecraft chains read, but with depth from every
 * angle. Links use the weapon's own metal texture (the ball's), so each tier's
 * chain matches its head, and are shaded per face and lit by the world.
 */
@SideOnly(Side.CLIENT)
final class ChainRenderer {
    /** Link length, width and wire thickness (blocks), and the step between links. */
    private static final double LENGTH = 0.17D;
    private static final double WIDTH = 0.10D;
    private static final double WIRE = 0.028D;
    private static final double STEP = 0.125D;
    /** Light from above-front, as items are lit; the chain is a shade darker than the head. */
    private static final double[] LIGHT = normalize(new double[] {0.3D, 1.0D, 0.55D});
    private static final float TONE = 0.82F;
    /** Realistic chains: wrought iron, a shade darker than an iron head so the two read apart. */
    private static final float[] IRON = {0.66F, 0.66F, 0.68F};
    /** Living and Sentient chains are tendon, not metal. */
    private static final float[] TENDON = {0.80F, 0.19F, 0.19F};
    private static final float[] MATERIAL = {TONE, TONE, TONE};
    /** Colour multiplier for the chain being drawn (rendering is single-threaded). */
    private static float[] tint = MATERIAL;

    private ChainRenderer() {}

    /**
     * @param texturePart the stack whose model's particle texture skins the links
     * @param path        camera-relative points the chain follows, in order
     */
    static void render(ItemStack texturePart, double[][] path) {
        if (path.length < 2) return;
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.item.Item item = texturePart.getItem();
        com.nanonaitor.arsenal.item.WeaponTier tier = item instanceof com.nanonaitor.arsenal.item.ItemArsenalWeapon
            ? ((com.nanonaitor.arsenal.item.ItemArsenalWeapon) item).getTier() : null;
        boolean parasite = tier == com.nanonaitor.arsenal.item.WeaponTier.LIVING
            || tier == com.nanonaitor.arsenal.item.WeaponTier.SENTIENT;
        TextureAtlasSprite sprite;
        if (parasite) {
            sprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/iron_block");
            tint = TENDON;
        } else if (com.nanonaitor.arsenal.config.ArsenalConfig.animations.chainMaterialColors) {
            sprite = mc.getRenderItem().getItemModelWithOverrides(texturePart, null, null).getParticleTexture();
            tint = MATERIAL;
        } else {
            sprite = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/iron_block");
            tint = IRON;
        }
        double[] mid = path[path.length / 2];
        applyWorldLight(mid[0], mid[1], mid[2]);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float u0 = sprite.getInterpolatedU(5.0D), u1 = sprite.getInterpolatedU(11.0D);
        float v0 = sprite.getInterpolatedV(5.0D), v1 = sprite.getInterpolatedV(11.0D);
        double travelled = STEP * 0.5D;
        int index = 0;
        for (int seg = 0; seg + 1 < path.length; seg++) {
            double[] a = path[seg], b = path[seg + 1];
            double[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            if (len < 1.0E-5D) continue;
            d = new double[] {d[0] / len, d[1] / len, d[2] / len};
            double[] side = cross(d, Math.abs(d[1]) > 0.95D ? new double[] {1, 0, 0} : new double[] {0, 1, 0});
            side = normalize(side);
            double[] other = cross(d, side);
            while (travelled <= len) {
                double t = travelled;
                double[] c = {a[0] + d[0] * t, a[1] + d[1] * t, a[2] + d[2] * t};
                boolean turned = (index & 1) == 1;
                link(buffer, c, d, turned ? other : side, turned ? side : other, u0, u1, v0, v1);
                index++;
                travelled += STEP;
            }
            travelled -= len;
        }
        tessellator.draw();
        GlStateManager.enableCull();
    }

    /** One oval link: two long side bars and two short end bars around an open centre. */
    private static void link(BufferBuilder buffer, double[] c, double[] axis, double[] across, double[] depth,
                             float u0, float u1, float v0, float v1) {
        double l = LENGTH * 0.5D, w = WIDTH * 0.5D, t = WIRE * 0.5D;
        box(buffer, c, axis, across, depth, -l, l, w - WIRE, w, -t, t, u0, u1, v0, v1);
        box(buffer, c, axis, across, depth, -l, l, -w, -w + WIRE, -t, t, u0, u1, v0, v1);
        box(buffer, c, axis, across, depth, l - WIRE, l, -w + WIRE, w - WIRE, -t, t, u0, u1, v0, v1);
        box(buffer, c, axis, across, depth, -l, -l + WIRE, -w + WIRE, w - WIRE, -t, t, u0, u1, v0, v1);
    }

    private static void box(BufferBuilder buffer, double[] c, double[] a, double[] b, double[] n,
                            double a0, double a1, double b0, double b1, double n0, double n1,
                            float u0, float u1, float v0, float v1) {
        double[][] p = new double[8][];
        for (int i = 0; i < 8; i++) {
            double ka = (i & 1) == 0 ? a0 : a1, kb = (i & 2) == 0 ? b0 : b1, kn = (i & 4) == 0 ? n0 : n1;
            p[i] = new double[] {c[0] + a[0] * ka + b[0] * kb + n[0] * kn,
                c[1] + a[1] * ka + b[1] * kb + n[1] * kn, c[2] + a[2] * ka + b[2] * kb + n[2] * kn};
        }
        face(buffer, p[0], p[1], p[3], p[2], neg(n), u0, u1, v0, v1);
        face(buffer, p[4], p[5], p[7], p[6], n, u0, u1, v0, v1);
        face(buffer, p[0], p[1], p[5], p[4], neg(b), u0, u1, v0, v1);
        face(buffer, p[2], p[3], p[7], p[6], b, u0, u1, v0, v1);
        face(buffer, p[0], p[2], p[6], p[4], neg(a), u0, u1, v0, v1);
        face(buffer, p[1], p[3], p[7], p[5], a, u0, u1, v0, v1);
    }

    private static void face(BufferBuilder buffer, double[] p0, double[] p1, double[] p2, double[] p3,
                             double[] normal, float u0, float u1, float v0, float v1) {
        double lambert = Math.max(0.0D, dot(normal, LIGHT));
        float shade = (float) (0.45D + 0.55D * lambert);
        float r = shade * tint[0], g = shade * tint[1], b = shade * tint[2];
        buffer.pos(p0[0], p0[1], p0[2]).tex(u0, v0).color(r, g, b, 1.0F).endVertex();
        buffer.pos(p1[0], p1[1], p1[2]).tex(u1, v0).color(r, g, b, 1.0F).endVertex();
        buffer.pos(p2[0], p2[1], p2[2]).tex(u1, v1).color(r, g, b, 1.0F).endVertex();
        buffer.pos(p3[0], p3[1], p3[2]).tex(u0, v1).color(r, g, b, 1.0F).endVertex();
    }

    /** Lightmap coordinates for the block at this camera-relative point. */
    static void applyWorldLight(double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        RenderManager manager = mc.getRenderManager();
        BlockPos pos = new BlockPos(x + manager.viewerPosX, y + manager.viewerPosY, z + manager.viewerPosZ);
        int light = mc.world.isBlockLoaded(pos) ? mc.world.getCombinedLight(pos, 0) : 0xF000F0;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
            (float) (light & 0xFFFF), (float) (light >>> 16));
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] neg(double[] a) {
        return new double[] {-a[0], -a[1], -a[2]};
    }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-9D ? new double[] {0, 1, 0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
