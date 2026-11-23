package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.network.ArsenalFxMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Client side of {@link ArsenalFxMessage}: camera shake and the armour-break icon. */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ArsenalClientFx {
    private static final ResourceLocation ARMOR_ICON = new ResourceLocation(
        NanonaitorsArsenal.MOD_ID, "textures/mob_effect/armor_fracture.png");
    private static final int SHAKE_TICKS = 8;
    /** Entity id -> world tick the icon disappears. */
    private static final Map<Integer, long[]> ICONS = new HashMap<>();
    private static long shakeStart = Long.MIN_VALUE;
    private static float shakeStrength;

    private ArsenalClientFx() {}

    public static void receive(byte type, int entityId, int value) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) return;
        long now = mc.world.getTotalWorldTime();
        if (type == ArsenalFxMessage.RAM_CRASH) BatteringRamInputHandler.crashed();
        if (type == ArsenalFxMessage.SHAKE || type == ArsenalFxMessage.RAM_CRASH) {
            shakeStart = now;
            shakeStrength = value / 100.0F;
        } else if (type == ArsenalFxMessage.ARMOR_BREAK) {
            ICONS.put(entityId, new long[] {now, now + Math.max(1, value)});
        }
    }

    /** Screen shake: a quick decaying jolt of the camera's pitch, yaw and roll. */
    @SubscribeEvent
    public static void onCamera(EntityViewRenderEvent.CameraSetup event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || shakeStart == Long.MIN_VALUE) return;
        float t = (float) (mc.world.getTotalWorldTime() - shakeStart + event.getRenderPartialTicks());
        if (t < 0.0F || t >= SHAKE_TICKS) {
            if (t >= SHAKE_TICKS) shakeStart = Long.MIN_VALUE;
            return;
        }
        float decay = 1.0F - t / SHAKE_TICKS;
        float k = shakeStrength * decay * decay;
        event.setPitch(event.getPitch() + (float) Math.sin(t * 2.9F) * 2.2F * k);
        event.setYaw(event.getYaw() + (float) Math.sin(t * 2.3F + 1.0F) * 1.6F * k);
        event.setRoll(event.getRoll() + (float) Math.sin(t * 3.7F + 2.0F) * 2.8F * k);
    }

    /** The Armor Fracture icon, floating over the struck target for a moment. */
    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || ICONS.isEmpty()) return;
        long now = mc.world.getTotalWorldTime();
        float partial = event.getPartialTicks();
        RenderManager manager = mc.getRenderManager();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
            GlStateManager.DestFactor.ZERO);
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        mc.getTextureManager().bindTexture(ARMOR_ICON);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (Iterator<Map.Entry<Integer, long[]>> it = ICONS.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, long[]> entry = it.next();
            long[] span = entry.getValue();
            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (entity == null || now >= span[1] || now < span[0] - 20L) {
                it.remove();
                continue;
            }
            float age = now - span[0] + partial;
            float life = span[1] - span[0];
            // Pops in, rises a little and fades out over its short life.
            float alpha = Math.min(1.0F, 3.0F * (1.0F - age / life));
            if (alpha <= 0.0F) continue;
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partial - manager.viewerPosX;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partial - manager.viewerPosY
                + entity.height + 0.45D + 0.15D * age / life;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partial - manager.viewerPosZ;
            float size = 0.28F * (0.85F + 0.15F * Math.min(1.0F, age / 1.5F));
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-manager.playerViewY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * manager.playerViewX,
                1.0F, 0.0F, 0.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            // Seen from the back of this frame, so u runs right to left.
            buffer.pos(-size, -size, 0).tex(1, 1).endVertex();
            buffer.pos(size, -size, 0).tex(0, 1).endVertex();
            buffer.pos(size, size, 0).tex(0, 0).endVertex();
            buffer.pos(-size, size, 0).tex(1, 0).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
    }
}
