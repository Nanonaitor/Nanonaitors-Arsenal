package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ScimitarShieldCompat;
import com.nanonaitor.arsenal.item.ItemScimitar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/** Cancel the previous tool's digging session when entering custom scimitar controls. */
@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID,value=Side.CLIENT)
public final class ScimitarMiningInputHandler {
    private static Item previousItem;
    private static int previousSlot=-1;
    private static boolean wasPair;
    private ScimitarMiningInputHandler() {}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.START)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || mc.playerController==null){previousItem=null;previousSlot=-1;wasPair=false;return;}
        Item item=mc.player.getHeldItemMainhand().getItem();
        boolean pair=ScimitarShieldCompat.isPair(mc.player);
        boolean switched=item!=previousItem || mc.player.inventory.currentItem!=previousSlot || pair!=wasPair;
        if((item instanceof ItemScimitar && switched) || pair) {
            mc.playerController.resetBlockRemoving();
            // The cancelled MouseEvent cannot deliver vanilla's key-up. Clear its latch
            // and queued presses; the paired attack controller reads physical input.
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(),false);
            while(mc.gameSettings.keyBindAttack.isPressed()) { /* drain old tool clicks */ }
        }
        previousItem=item;
        previousSlot=mc.player.inventory.currentItem;
        wasPair=pair;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void mining(PlayerInteractEvent.LeftClickBlock event) {
        if(ScimitarShieldCompat.isPair(event.getEntityPlayer())) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        }
    }
}
