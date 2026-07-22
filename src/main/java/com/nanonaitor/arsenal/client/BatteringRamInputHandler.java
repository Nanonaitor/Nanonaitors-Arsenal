package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.network.BatteringRamChargeMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class BatteringRamInputHandler {
    private static long lastHeartbeatTick = Long.MIN_VALUE;
    private static boolean cameraLocked;
    private static float lockedYaw;
    private static float lockedPitch;

    private BatteringRamInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null) {
            cameraLocked = false;
            return;
        }
        boolean holdingRam = player.getHeldItemMainhand().getItem() instanceof ItemBatteringRam;
        boolean hasChargeEnergy = player.capabilities.isCreativeMode
            || player.getFoodStats().getFoodLevel() > 6;
        boolean canCharge = holdingRam && player.getHeldItemOffhand().isEmpty()
            && hasChargeEnergy
            && minecraft.currentScreen == null
            && minecraft.gameSettings.keyBindAttack.isKeyDown();
        if (!canCharge) {
            cameraLocked = false;
            if (holdingRam && player.isHandActive()
                && player.getActiveHand() == EnumHand.MAIN_HAND) {
                player.resetActiveHand();
            }
            return;
        }
        if (!cameraLocked) {
            cameraLocked = true;
            lockedYaw = player.rotationYaw;
            lockedPitch = player.rotationPitch;
        }
        player.rotationYaw = lockedYaw;
        player.prevRotationYaw = lockedYaw;
        player.rotationYawHead = lockedYaw;
        player.prevRotationYawHead = lockedYaw;
        player.renderYawOffset = lockedYaw;
        player.prevRenderYawOffset = lockedYaw;
        player.rotationPitch = lockedPitch;
        player.prevRotationPitch = lockedPitch;
        if (!player.isHandActive()) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        long now = player.world.getTotalWorldTime();
        if (lastHeartbeatTick == Long.MIN_VALUE || now < lastHeartbeatTick
            || now - lastHeartbeatTick >= 2L) {
            lastHeartbeatTick = now;
            ModNetwork.CHANNEL.sendToServer(new BatteringRamChargeMessage());
        }
    }
}
