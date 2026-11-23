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
 * angle. Links are built from whole model pixels and textured one texel per
 * pixel, like block models. Realistic chains are iron (red tendon for the
 * parasite tiers); optionally each tier's material. Shaded per face and lit
 * by the world.
 */
@SideOnly(Side.CLIENT)
final class ChainRenderer {
    /** One model pixel (1/16 block). */
    private static final double PX = 1.0D / 16.0D;
    /** Link length, width and wire thickness, and the step between links (whole pixels). */
    private static final double LENGTH = 4 * PX;
    private static final double WIDTH = 2.1D * PX;
    private static final double WIRE = 0.7D * PX;
    /** Texels per model pixel on links: a link's length spans the whole metal texture, so its pattern shows. */
    private static final double TEXEL_DENSITY = 4.0D;
    private static final double STEP = 3 * PX;
    /** Light from above-front, as items are lit. */
    private static final double[] LIGHT = normalize(new double[] {0.3D, 1.0D, 0.55D});
    private static final float TONE = 0.82F;
    /** Realistic chains: wrought iron, a shade darker than an iron head so the two read apart. */
    private static final float[] IRON = {0.78F, 0.78F, 0.80F};
    /** Living and Sentient chains are tendon, not metal. */
    private static final float[] TENDON = {0.80F, 0.19F, 0.19F};
    private static final float[] MATERIAL = {TONE, TONE, TONE};
    private static final float[] FULL = {1.0F, 1.0F, 1.0F};

    /** What is being drawn now (rendering is single-threaded). */
    private static float[] tint = MATERIAL;
    private static TextureAtlasSprite texture;
    private static float alpha = 1.0F;
    /** Link cross-section scale: the flail's chain is lighter than the Ball and Chain's. */
    private static double thin = 1.0D;

    private ChainRenderer() {}

    /**
     * @param texturePart the weapon's head part (its material, and its tier)
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
        if (parasite) {
            texture = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/iron_block");
            tint = TENDON;
        } else if (com.nanonaitor.arsenal.config.ArsenalConfig.animations.chainMaterialColors) {
            texture = mc.getRenderItem().getItemModelWithOverrides(texturePart, null, null).getParticleTexture();
            tint = MATERIAL;
        } else {
            texture = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/iron_block");
            tint = IRON;
        }
        alpha = 1.0F;
        thin = item instanceof com.nanonaitor.arsenal.item.ItemFlail ? 0.7D : 1.0D;
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
        links(buffer, path);
        tessellator.draw();
        if (texturePart.hasEffect()) glint(path);
        GlStateManager.enableCull();
    }

    /** Every link along the path, alternating by 90 degrees. */
    private static void links(BufferBuilder buffer, double[][] path) {
        double travelled = STEP * 0.5D;
        int index = 0;
        for (int seg = 0; seg + 1 < path.length; seg++) {
            double[] a = path[seg], b = path[seg + 1];
            double[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            double len = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            if (len < 1.0E-5D) continue;
            d = new double[] {d[0] / len, d[1] / len, d[2] / len};
            double[] side = normalize(cross(d, Math.abs(d[1]) > 0.95D ? new double[] {1, 0, 0} : new double[] {0, 1, 0}));
            double[] other = cross(d, side);
            while (travelled <= len) {
                double t = travelled;
                double[] c = {a[0] + d[0] * t, a[1] + d[1] * t, a[2] + d[2] * t};
                boolean turned = (index & 1) == 1;
                link(buffer, c, d, turned ? other : side, turned ? side : other);
                index++;
                travelled += STEP;
            }
            travelled -= len;
        }
    }

    private static final net.minecraft.util.ResourceLocation GLINT =
        new net.minecraft.util.ResourceLocation("textures/misc/enchanted_item_glint.png");

    /** The enchantment shimmer over the links, drawn like vanilla's item glint (two scrolling passes). */
    private static void glint(double[][] path) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(GLINT);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_EQUAL);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
        float[] saved = tint;
        tint = GLINT_COLOR;
        glintPass = true;
        RenderManager manager = mc.getRenderManager();
        glintX = manager.viewerPosX;
        glintY = manager.viewerPosY;
        glintZ = manager.viewerPosZ;
        Tessellator tessellator = Tessellator.getInstance();
        for (int pass = 0; pass < 2; pass++) {
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            long period = pass == 0 ? 3000L : 4873L;
            float scroll = (float) (Minecraft.getSystemTime() % period) / period / 8.0F;
            GlStateManager.translate(pass == 0 ? scroll : -scroll, 0.0F, 0.0F);
            GlStateManager.rotate(pass == 0 ? -50.0F : 10.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            links(buffer, path);
            tessellator.draw();
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        }
        glintPass = false;
        tint = saved;
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.depthMask(true);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    /** Vanilla's glint colour (0xFF8040CC). */
    private static final float[] GLINT_COLOR = {0.5F, 0.25F, 0.8F};
    private static boolean glintPass;
    private static double glintX, glintY, glintZ;

    private static void glintVertex(BufferBuilder buffer, double[] p) {
        double x = p[0] + glintX, y = p[1] + glintY, z = p[2] + glintZ;
        buffer.pos(p[0], p[1], p[2]).tex((x + z) * 0.12D, (y - x * 0.5D) * 0.12D)
            .color(tint[0], tint[1], tint[2], 1.0F).endVertex();
    }

    /**
     * A see-through copy of the Ball and Chain head (its rounded voxel shape),
     * for the trail behind a fully charged throw.
     *
     * @param size the head's model scale (16 px = size blocks)
     */
    static void renderGhostBall(ItemStack ballPart, double x, double y, double z, double size, float opacity) {
        Minecraft mc = Minecraft.getMinecraft();
        texture = mc.getRenderItem().getItemModelWithOverrides(ballPart, null, null).getParticleTexture();
        tint = FULL;
        alpha = opacity;
        applyWorldLight(x, y, z);
        mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        double[] c = {x, y, z};
        double[] ax = {1, 0, 0}, ay = {0, 1, 0}, az = {0, 0, 1};
        double k = size / 16.0D;
        // The head model's voxels, centred on its (8, 8, 8) px middle.
        box(buffer, c, ax, ay, az, -4 * k, 4 * k, -4 * k, 4 * k, -4 * k, 4 * k);
        box(buffer, c, ax, ay, az, -5 * k, 5 * k, -3 * k, 3 * k, -3 * k, 3 * k);
        box(buffer, c, ax, ay, az, -3 * k, 3 * k, -5 * k, 5 * k, -3 * k, 3 * k);
        box(buffer, c, ax, ay, az, -3 * k, 3 * k, -3 * k, 3 * k, -5 * k, 5 * k);
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        alpha = 1.0F;
    }

    /** One oval link: two long side bars and two short end bars around an open centre. */
    private static void link(BufferBuilder buffer, double[] c, double[] axis, double[] across, double[] depth) {
        double wire = WIRE * thin;
        double l = LENGTH * 0.5D, w = WIDTH * thin * 0.5D, t = wire * 0.5D;
        box(buffer, c, axis, across, depth, -l, l, w - wire, w, -t, t);
        box(buffer, c, axis, across, depth, -l, l, -w, -w + wire, -t, t);
        box(buffer, c, axis, across, depth, l - wire, l, -w + wire, w - wire, -t, t);
        box(buffer, c, axis, across, depth, -l, -l + wire, -w + wire, w - wire, -t, t);
    }

    private static void box(BufferBuilder buffer, double[] c, double[] a, double[] b, double[] n,
                            double a0, double a1, double b0, double b1, double n0, double n1) {
        double[][] p = new double[8][];
        for (int i = 0; i < 8; i++) {
            double ka = (i & 1) == 0 ? a0 : a1, kb = (i & 2) == 0 ? b0 : b1, kn = (i & 4) == 0 ? n0 : n1;
            p[i] = new double[] {c[0] + a[0] * ka + b[0] * kb + n[0] * kn,
                c[1] + a[1] * ka + b[1] * kb + n[1] * kn, c[2] + a[2] * ka + b[2] * kb + n[2] * kn};
        }
        face(buffer, p[0], p[1], p[3], p[2], neg(n));
        face(buffer, p[4], p[5], p[7], p[6], n);
        face(buffer, p[0], p[1], p[5], p[4], neg(b));
        face(buffer, p[2], p[3], p[7], p[6], b);
        face(buffer, p[0], p[2], p[6], p[4], neg(a));
        face(buffer, p[1], p[3], p[7], p[5], a);
    }

    /**
     * One quad at true pixel scale: one texel per 1/16 block along each edge
     * (a 4 x 1 px bar samples a 4 x 1 texel strip), like block models.
     */
    private static void face(BufferBuilder buffer, double[] p0, double[] p1, double[] p2, double[] p3,
                             double[] normal) {
        if (glintPass) {
            // World-space glint UVs, so the shimmer sweeps along the chain as one piece.
            glintVertex(buffer, p0);
            glintVertex(buffer, p1);
            glintVertex(buffer, p2);
            glintVertex(buffer, p3);
            return;
        }
        double lambert = Math.max(0.0D, dot(normal, LIGHT));
        float shade = (float) (0.45D + 0.55D * lambert);
        float r = shade * tint[0], g = shade * tint[1], b = shade * tint[2];
        double density = alpha < 1.0F ? 2.0D : TEXEL_DENSITY;
        double width = Math.min(16.0D, distance(p0, p1) * 16.0D * density);
        double height = Math.min(16.0D, distance(p0, p3) * 16.0D * density);
        float u0 = texture.getInterpolatedU(8.0D - width / 2.0D), u1 = texture.getInterpolatedU(8.0D + width / 2.0D);
        float v0 = texture.getInterpolatedV(8.0D - height / 2.0D), v1 = texture.getInterpolatedV(8.0D + height / 2.0D);
        buffer.pos(p0[0], p0[1], p0[2]).tex(u0, v0).color(r, g, b, alpha).endVertex();
        buffer.pos(p1[0], p1[1], p1[2]).tex(u1, v0).color(r, g, b, alpha).endVertex();
        buffer.pos(p2[0], p2[1], p2[2]).tex(u1, v1).color(r, g, b, alpha).endVertex();
        buffer.pos(p3[0], p3[1], p3[2]).tex(u0, v1).color(r, g, b, alpha).endVertex();
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

    private static double distance(double[] a, double[] b) {
        double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return Math.sqrt(x * x + y * y + z * z);
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
