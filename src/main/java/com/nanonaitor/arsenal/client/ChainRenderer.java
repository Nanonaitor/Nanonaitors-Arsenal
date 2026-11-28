package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nanonaitor.arsenal.config.ArsenalClientConfig;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws chains as real 3D links: oval rings that alternate by 90 degrees along
 * the chain, the way modern Minecraft chains read, but with depth from every
 * angle. Links are built from whole model pixels and textured one texel per
 * pixel, like block models. Realistic chains are iron (red tendon for the
 * parasite tiers); optionally each tier's material. Lit by the world.
 */
public final class ChainRenderer {
    /** One model pixel (1/16 block). */
    private static final double PX = 1.0D / 16.0D;
    /** Link length, width and wire thickness, and the step between links. */
    private static final double LENGTH = 4 * PX, WIDTH = 2.1D * PX, WIRE = 0.7D * PX, STEP = 3 * PX;
    /** Texels per model pixel on links: a link's length spans the metal texture, so its pattern shows. */
    private static final double TEXEL_DENSITY = 4.0D;
    private static final float[] IRON = {0.78F, 0.78F, 0.80F};
    /** Living and Sentient chains are tendon, not metal. */
    private static final float[] TENDON = {0.80F, 0.19F, 0.19F};
    private static final float[] MATERIAL = {0.82F, 0.82F, 0.82F};
    private static final float[] FULL = {1.0F, 1.0F, 1.0F};
    private static final ResourceLocation IRON_BLOCK = new ResourceLocation("minecraft", "block/iron_block");

    /** The weapon being drawn is enchanted: its chain, head and handle glint. Set per weapon drawn. */
    static boolean glint;

    /** A render-only copy of a part that glints like the enchanted weapon it belongs to. */
    static ItemStack glinting(ItemStack part) {
        if (!glint) return part;
        ItemStack copy = part.copy();
        var list = new net.minecraft.nbt.ListTag();
        var entry = new net.minecraft.nbt.CompoundTag();
        entry.putString("id", "minecraft:unbreaking");
        entry.putShort("lvl", (short)1);
        list.add(entry);
        copy.getOrCreateTag().put("Enchantments", list);
        return copy;
    }

    /** How one chain or ghost is drawn. */
    private record Style(VertexConsumer buffer, TextureAtlasSprite sprite, float[] tint, float alpha, int light, double density) {}

    private static TextureAtlasSprite blockSprite(ResourceLocation id) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(id);
    }

    /**
     * @param head  the drawn head (its material colours the chain when chainMaterialColors is on)
     * @param thin  link cross-section scale: the flail's chain is 30% lighter than the Ball and Chain's
     * @param path  points the chain follows, in order, in the pose's space
     */
    public static void render(PoseStack pose, MultiBufferSource buffers, int light, WeaponTier tier,
                              ItemStack head, double thin, double[]... path) {
        if (path.length < 2) return;
        boolean parasite = tier == WeaponTier.LIVING || tier == WeaponTier.SENTIENT;
        TextureAtlasSprite sprite;
        float[] tint;
        if (parasite) { sprite = blockSprite(IRON_BLOCK); tint = TENDON; }
        else if (ArsenalClientConfig.get(ArsenalClientConfig.CHAIN_MATERIAL_COLORS) && !head.isEmpty()) {
            sprite = Minecraft.getInstance().getItemRenderer().getModel(head, null, null, 0).getParticleIcon(
                net.minecraftforge.client.model.data.ModelData.EMPTY);
            tint = MATERIAL;
        } else { sprite = blockSprite(IRON_BLOCK); tint = IRON; }
        // Enchanted weapons glint on their links too.
        Style style = new Style(net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(buffers,
                RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS), false, glint),
            sprite, tint, 1.0F, light, TEXEL_DENSITY);
        double travelled = STEP * 0.5D;
        int index = 0;
        for (int seg = 0; seg + 1 < path.length; seg++) {
            double[] a = path[seg], b = path[seg + 1];
            double[] d = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
            double len = Math.sqrt(dot(d, d));
            if (len < 1.0E-5D) continue;
            d = new double[] {d[0] / len, d[1] / len, d[2] / len};
            double[] side = normalize(cross(d, Math.abs(d[1]) > 0.95D ? new double[] {1, 0, 0} : new double[] {0, 1, 0}));
            double[] other = cross(d, side);
            while (travelled <= len) {
                double t = travelled;
                double[] c = {a[0] + d[0] * t, a[1] + d[1] * t, a[2] + d[2] * t};
                boolean turned = (index & 1) == 1;
                link(pose, style, c, d, turned ? other : side, turned ? side : other, thin);
                index++;
                travelled += STEP;
            }
            travelled -= len;
        }
    }

    /**
     * A see-through copy of the Ball and Chain head (its rounded voxel shape), for
     * the trail behind a fully charged throw.
     *
     * @param size the head's model scale (16 px = size blocks)
     */
    public static void renderGhostBall(PoseStack pose, MultiBufferSource buffers, int light, ItemStack head,
                                       double x, double y, double z, double size, float opacity) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getItemRenderer().getModel(head, null, null, 0)
            .getParticleIcon(net.minecraftforge.client.model.data.ModelData.EMPTY);
        Style style = new Style(buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),
            sprite, FULL, opacity, light, 2.0D);
        double[] c = {x, y, z}, ax = {1, 0, 0}, ay = {0, 1, 0}, az = {0, 0, 1};
        double k = size / 16.0D;
        // The head model's voxels, centred on its (8, 8, 8) px middle.
        box(pose, style, c, ax, ay, az, -4 * k, 4 * k, -4 * k, 4 * k, -4 * k, 4 * k);
        box(pose, style, c, ax, ay, az, -5 * k, 5 * k, -3 * k, 3 * k, -3 * k, 3 * k);
        box(pose, style, c, ax, ay, az, -3 * k, 3 * k, -5 * k, 5 * k, -3 * k, 3 * k);
        box(pose, style, c, ax, ay, az, -3 * k, 3 * k, -3 * k, 3 * k, -5 * k, 5 * k);
    }

    /** One oval link: two long side bars and two short end bars around an open centre. */
    private static void link(PoseStack pose, Style style, double[] c, double[] axis, double[] across, double[] depth, double thin) {
        double wire = WIRE * thin;
        double l = LENGTH * 0.5D, w = WIDTH * thin * 0.5D, t = wire * 0.5D;
        box(pose, style, c, axis, across, depth, -l, l, w - wire, w, -t, t);
        box(pose, style, c, axis, across, depth, -l, l, -w, -w + wire, -t, t);
        box(pose, style, c, axis, across, depth, l - wire, l, -w + wire, w - wire, -t, t);
        box(pose, style, c, axis, across, depth, -l, -l + wire, -w + wire, w - wire, -t, t);
    }

    private static void box(PoseStack pose, Style style, double[] c, double[] a, double[] b, double[] n,
                            double a0, double a1, double b0, double b1, double n0, double n1) {
        double[][] p = new double[8][];
        for (int i = 0; i < 8; i++) {
            double ka = (i & 1) == 0 ? a0 : a1, kb = (i & 2) == 0 ? b0 : b1, kn = (i & 4) == 0 ? n0 : n1;
            p[i] = new double[] {c[0] + a[0] * ka + b[0] * kb + n[0] * kn,
                c[1] + a[1] * ka + b[1] * kb + n[1] * kn, c[2] + a[2] * ka + b[2] * kb + n[2] * kn};
        }
        face(pose, style, p[0], p[2], p[3], p[1], neg(n));
        face(pose, style, p[4], p[5], p[7], p[6], n);
        face(pose, style, p[0], p[1], p[5], p[4], neg(b));
        face(pose, style, p[2], p[6], p[7], p[3], b);
        face(pose, style, p[0], p[4], p[6], p[2], neg(a));
        face(pose, style, p[1], p[3], p[7], p[5], a);
    }

    /** One quad at true pixel scale: one texel per 1/16 block along each edge, like block models. */
    private static void face(PoseStack pose, Style style, double[] p0, double[] p1, double[] p2, double[] p3, double[] normal) {
        Matrix4f m = pose.last().pose();
        Matrix3f nm = pose.last().normal();
        TextureAtlasSprite s = style.sprite;
        double width = Math.min(16.0D, distance(p0, p1) * 16.0D * style.density);
        double height = Math.min(16.0D, distance(p0, p3) * 16.0D * style.density);
        float u0 = s.getU(8.0D - width / 2.0D), u1 = s.getU(8.0D + width / 2.0D);
        float v0 = s.getV(8.0D - height / 2.0D), v1 = s.getV(8.0D + height / 2.0D);
        float r = style.tint[0], g = style.tint[1], b = style.tint[2];
        float nx = (float)normal[0], ny = (float)normal[1], nz = (float)normal[2];
        vertex(style, m, nm, p0, u0, v0, r, g, b, nx, ny, nz);
        vertex(style, m, nm, p1, u1, v0, r, g, b, nx, ny, nz);
        vertex(style, m, nm, p2, u1, v1, r, g, b, nx, ny, nz);
        vertex(style, m, nm, p3, u0, v1, r, g, b, nx, ny, nz);
    }

    private static void vertex(Style style, Matrix4f m, Matrix3f nm, double[] p, float u, float v,
                               float r, float g, float b, float nx, float ny, float nz) {
        style.buffer.vertex(m, (float)p[0], (float)p[1], (float)p[2]).color(r, g, b, style.alpha).uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(style.light).normal(nm, nx, ny, nz).endVertex();
    }

    private static double distance(double[] a, double[] b) {
        double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private static double[] neg(double[] a) { return new double[] {-a[0], -a[1], -a[2]}; }

    private static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1.0E-9D ? new double[] {0, 1, 0} : new double[] {a[0] / l, a[1] / l, a[2] / l};
    }

    private ChainRenderer() {}
}
