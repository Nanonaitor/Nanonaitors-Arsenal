package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemScimitar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShield;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID,value=Side.CLIENT)
public final class ShieldUsePriority {
    private ShieldUsePriority() {}
    public static boolean requested(EntityPlayer player) {
        Minecraft mc=Minecraft.getMinecraft();
        boolean shield=player.getHeldItemMainhand().getItem() instanceof ItemShield
            || player.getHeldItemOffhand().getItem() instanceof ItemShield;
        return shield && (player.isHandActive() && player.getActiveItemStack().getItem() instanceof ItemShield
            || mc.gameSettings.keyBindUseItem.isKeyDown() || org.lwjgl.input.Mouse.isButtonDown(1));
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST,receiveCanceled=true)
    public static void mouse(MouseEvent event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || mc.currentScreen!=null || event.getButton()!=mc.gameSettings.keyBindUseItem.getKeyCode()+100)return;
        Item main=mc.player.getHeldItemMainhand().getItem(),off=mc.player.getHeldItemOffhand().getItem();
        EnumHand shield=main instanceof ItemShield ? EnumHand.MAIN_HAND:off instanceof ItemShield ? EnumHand.OFF_HAND:null;
        boolean weapon=main instanceof ItemScimitar || main instanceof ItemBallAndChain || off instanceof ItemScimitar;
        if(shield==null || !weapon)return;
        event.setCanceled(true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(),event.isButtonstate());
        if(event.isButtonstate() && mc.playerController!=null) {
            if(mc.player.isHandActive() && mc.player.getActiveHand()!=shield) mc.playerController.onStoppedUsingItem(mc.player);
            if(!mc.player.isHandActive())mc.playerController.processRightClick(mc.player,mc.world,shield);
        }
    }
}
