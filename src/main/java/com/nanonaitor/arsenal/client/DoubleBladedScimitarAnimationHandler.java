package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class DoubleBladedScimitarAnimationHandler {
    private DoubleBladedScimitarAnimationHandler() {}

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST,
        receiveCanceled = true)
    public static void firstPerson(RenderSpecificHandEvent event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || event.getHand() != EnumHand.MAIN_HAND
            || !(event.getItemStack().getItem() instanceof ItemDoubleBladedScimitar)
            || !DoubleBladedScimitarCombat.isReflecting(player)) return;
        event.setCanceled(true);
        float angle = (player.ticksExisted + event.getPartialTicks()) * 72.0F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.34D, -0.34D, -0.82D);
        GlStateManager.rotate(18.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
        GlStateManager.scale(1.725D, 1.725D, 1.725D);
        Minecraft.getMinecraft().getRenderItem().renderItem(event.getItemStack(),
            ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public static void thirdPerson(RenderWorldLastEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) return;
        RenderManager manager = minecraft.getRenderManager();
        for (EntityPlayer player : minecraft.world.playerEntities) {
            if (!DoubleBladedScimitarCombat.isReflecting(player)) continue;
            if (player == minecraft.player && minecraft.gameSettings.thirdPersonView == 0) continue;
            ItemStack held = player.getHeldItemMainhand();
            double x = player.lastTickPosX + (player.posX-player.lastTickPosX)*event.getPartialTicks()
                - manager.viewerPosX;
            double y = player.lastTickPosY + (player.posY-player.lastTickPosY)*event.getPartialTicks()
                - manager.viewerPosY + 1.15D;
            double z = player.lastTickPosZ + (player.posZ-player.lastTickPosZ)*event.getPartialTicks()
                - manager.viewerPosZ;
            float angle = (player.ticksExisted + event.getPartialTicks()) * 72.0F;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-player.rotationYaw, 0.0F, 1.0F, 0.0F);
            // Center the staff on the torso and project it forward in the
            // player's local facing direction. Keeping this translation after
            // the yaw transform makes it follow the body without orbiting it.
            GlStateManager.translate(0.0D, 0.0D, 0.58D);
            GlStateManager.rotate(angle, 0.0F, 0.0F, 1.0F);
            GlStateManager.scale(1.35D, 1.35D, 1.35D);
            minecraft.getRenderItem().renderItem(held, ItemCameraTransforms.TransformType.NONE);
            GlStateManager.popMatrix();
        }
    }
}
