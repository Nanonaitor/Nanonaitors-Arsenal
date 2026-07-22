package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.FlailCombat;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.network.FlailSwingMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class FlailInputHandler {
    private static long lastRequestTick = Long.MIN_VALUE;

    private FlailInputHandler() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || minecraft.currentScreen != null
            || !(player.getHeldItemMainhand().getItem() instanceof ItemFlail)
            || !minecraft.gameSettings.keyBindAttack.isKeyDown()) {
            return;
        }
        requestSwing(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote
            && player.getHeldItemMainhand().getItem() instanceof ItemFlail) {
            event.setCanceled(true);
            requestSwing(player);
        }
    }

    private static void requestSwing(EntityPlayer player) {
        long now = player.world.getTotalWorldTime();
        if (lastRequestTick == Long.MIN_VALUE || now < lastRequestTick
            || now - lastRequestTick >= FlailCombat.SWING_INTERVAL_TICKS) {
            lastRequestTick = now;
            player.resetCooldown();
            ModNetwork.CHANNEL.sendToServer(new FlailSwingMessage());
        }
    }
}
