package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.ShieldCombat;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import com.nanonaitor.arsenal.item.ItemTartsyShield;
import com.nanonaitor.arsenal.network.BulwarkBashMessage;
import com.nanonaitor.arsenal.network.TartsyBashMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ShieldInputHandler {
    private ShieldInputHandler() {}

    @SubscribeEvent
    public static void onMouse(MouseEvent event) {
        if (event.getButton() != 0 || !event.isButtonstate()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null) return;
        if (ShieldCombat.isGuarding(player, ItemTartsyShield.class)) {
            event.setCanceled(true);
            net.minecraft.util.math.Vec3d look = player.getLookVec();
            double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            if (horizontal > 0.001D) {
                player.motionX = look.x / horizontal * 1.0125D;
                player.motionY = Math.max(player.motionY, 0.12D);
                player.motionZ = look.z / horizontal * 1.0125D;
            }
            player.swingArm(player.getActiveHand());
            ModNetwork.CHANNEL.sendToServer(new TartsyBashMessage());
            return;
        }
        if (!ShieldCombat.isGuarding(player, ItemSunWarBulwark.class)) return;
        event.setCanceled(true);
        player.swingArm(player.getActiveHand());
        ModNetwork.CHANNEL.sendToServer(new BulwarkBashMessage());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || !ShieldCombat.isGuarding(player, ItemSunWarBulwark.class)
            || !ShieldCombat.isBulwarkReady(player)) return;
        if (minecraft.gameSettings.keyBindSprint.isKeyDown()
            && player.movementInput.moveForward >= 0.8F
            && (player.getFoodStats().getFoodLevel() > 6 || player.capabilities.allowFlying)) {
            player.setSprinting(true);
        }
    }
}
