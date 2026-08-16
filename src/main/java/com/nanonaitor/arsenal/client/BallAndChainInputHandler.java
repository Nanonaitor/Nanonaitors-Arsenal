package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.network.BallAndChainSwingMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class BallAndChainInputHandler {
    private static final java.util.UUID USE_SPEED_UUID = java.util.UUID.fromString(
        "ddf96815-9596-4d0c-93ea-a08c00a16ae4");
    private static long lastHeartbeatTick = Long.MIN_VALUE;
    private static boolean wasSwinging;

    private BallAndChainInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null) {
            wasSwinging = false;
            lastHeartbeatTick = Long.MIN_VALUE;
            return;
        }
        boolean holdingWeapon = player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain;
        boolean retrieving = BallAndChainAnimationHandler.isReleaseAnimationActive(player);
        boolean canSwing = holdingWeapon && ArsenalCompatManager.canUseTwoHanded(player)
            && !retrieving
            && minecraft.currentScreen == null
            && minecraft.gameSettings.keyBindAttack.isKeyDown();
        updateUseSpeed(player, canSwing || retrieving);
        player.getEntityData().setBoolean("ArsenalBallAndChainActive",
            holdingWeapon && (canSwing || retrieving));
        if (!canSwing) {
            if (wasSwinging) {
                ModNetwork.CHANNEL.sendToServer(new BallAndChainSwingMessage(false));
                wasSwinging = false;
            }
            if (holdingWeapon && !retrieving && player.isHandActive()
                && player.getActiveHand() == EnumHand.MAIN_HAND) {
                player.resetActiveHand();
            }
            return;
        }
        if (!player.isHandActive()) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        wasSwinging = true;
        long now = player.world.getTotalWorldTime();
        if (lastHeartbeatTick == Long.MIN_VALUE || now < lastHeartbeatTick
            || now - lastHeartbeatTick >= 2L) {
            lastHeartbeatTick = now;
            ModNetwork.CHANNEL.sendToServer(new BallAndChainSwingMessage(true));
        }
    }

    private static void updateUseSpeed(EntityPlayer player, boolean active) {
        IAttributeInstance speed = player.getEntityAttribute(
            SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier old = speed.getModifier(USE_SPEED_UUID);
        if (old != null) speed.removeModifier(old);
        if (active && player.isHandActive()) {
            // Minecraft 1.12.2 multiplies movement input by 0.2 whenever an
            // item is active. This transient x5 modifier cancels only that
            // animation-state penalty; all other speed effects still apply.
            speed.applyModifier(new AttributeModifier(USE_SPEED_UUID,
                "Ball and Chain animation movement compensation", 4.0D, 2)
                .setSaved(false));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote && player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            event.setCanceled(true);
        }
    }
}
