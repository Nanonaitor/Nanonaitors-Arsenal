package com.nanonaitor.arsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.network.ModNetwork;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/** Client side of Arsenal's effect packets: the Armor Fracture icon over a cracked target. */
public final class ArsenalClientFx {
    private static final ResourceLocation ARMOR_ICON = new ResourceLocation(ArsenalMod.MOD_ID,
        "textures/mob_effect/armor_fracture.png");
    /** Entity id -> {first tick, last tick}. */
    private static final Map<Integer, long[]> ICONS = new HashMap<>();

    private static final int SHAKE_TICKS = 8;
    private static long shakeStart = Long.MIN_VALUE;
    private static float shakeStrength;

    public static void receive(byte type, int entityId, int value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (type == ModNetwork.FX_ARMOR_BREAK) ICONS.put(entityId, new long[] {now, now + Math.max(1, value)});
        if (type == ModNetwork.FX_RAM_CRASH) ClientControls.ramCrashed();
        if (type == ModNetwork.FX_SHAKE || type == ModNetwork.FX_RAM_CRASH) {
            shakeStart = now;
            shakeStrength = value / 100.0F;
        }
    }

    /** Screen shake: a quick decaying jolt of the camera's pitch, yaw and roll. */
    static void shake(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || shakeStart == Long.MIN_VALUE) return;
        float t = (float)(mc.level.getGameTime() - shakeStart + event.getPartialTick());
        if (t < 0.0F || t >= SHAKE_TICKS) {
            if (t >= SHAKE_TICKS) shakeStart = Long.MIN_VALUE;
            return;
        }
        float decay = 1.0F - t / SHAKE_TICKS;
        float k = shakeStrength * decay * decay;
        event.setPitch(event.getPitch() + Mth.sin(t * 2.9F) * 2.2F * k);
        event.setYaw(event.getYaw() + Mth.sin(t * 2.3F + 1.0F) * 1.6F * k);
        event.setRoll(event.getRoll() + Mth.sin(t * 3.7F + 2.0F) * 2.8F * k);
    }

    /** Whether this entity is showing the icon right now (tests and the capture tool). */
    public static boolean showing(Entity entity) {
        return ICONS.containsKey(entity.getId());
    }

    /** The icon pops in over the struck target, rises a little and fades out. */
    static void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ICONS.isEmpty()) return;
        long now = mc.level.getGameTime();
        float partial = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(ARMOR_ICON));
        PoseStack pose = event.getPoseStack();
        for (Iterator<Map.Entry<Integer, long[]>> it = ICONS.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, long[]> entry = it.next();
            long[] span = entry.getValue();
            Entity entity = mc.level.getEntity(entry.getKey());
            if (entity == null || now >= span[1] || now < span[0] - 20L) { it.remove(); continue; }
            float age = now - span[0] + partial;
            float life = span[1] - span[0];
            float alpha = Math.min(1.0F, 3.0F * (1.0F - age / life));
            if (alpha <= 0.0F) continue;
            double x = Mth.lerp(partial, entity.xOld, entity.getX()) - camera.x;
            double y = Mth.lerp(partial, entity.yOld, entity.getY()) - camera.y + entity.getBbHeight() + 0.45D + 0.15D * age / life;
            double z = Mth.lerp(partial, entity.zOld, entity.getZ()) - camera.z;
            float size = 0.28F * (0.85F + 0.15F * Math.min(1.0F, age / 1.5F));
            pose.pushPose();
            pose.translate(x, y, z);
            // Face the camera, built from its view angles (what the world view itself uses).
            pose.mulPose(new org.joml.Quaternionf().rotationYXZ(-event.getCamera().getYRot() * Mth.DEG_TO_RAD,
                event.getCamera().getXRot() * Mth.DEG_TO_RAD, 0.0F));
            Matrix4f m = pose.last().pose();
            int a = (int)(alpha * 255.0F), light = LightTexture.FULL_BRIGHT;
            float[][] corners = {{-size, -size, 1, 1}, {size, -size, 0, 1}, {size, size, 0, 0}, {-size, size, 1, 0}};
            // Both windings, so the icon shows whichever way the face is culled.
            for (int i = 0; i < 4; i++) icon(buffer, m, corners[i], a, light);
            for (int i = 3; i >= 0; i--) icon(buffer, m, corners[i], a, light);
            pose.popPose();
        }
        buffers.endBatch(RenderType.textSeeThrough(ARMOR_ICON));
    }

    private static void icon(VertexConsumer buffer, Matrix4f m, float[] c, int alpha, int light) {
        buffer.vertex(m, c[0], c[1], 0).color(255, 255, 255, alpha).uv(c[2], c[3]).uv2(light).endVertex();
    }

    private ArsenalClientFx() {}
}
