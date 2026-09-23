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
    private static MouseEvent handledAttack;
    private static boolean bashOwnsAttack;
    public static boolean suppressWeaponAttack() {
        if (!BallAndChainInputHandler.isAttackPhysicallyDown()) bashOwnsAttack = false;
        return bashOwnsAttack;
    }
    private ShieldInputHandler() {}

    @SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMouse(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        int use = mc.gameSettings.keyBindUseItem.getKeyCode();
        if (mc.player != null && mc.currentScreen == null && use < 0
            && event.getButton() == use + 100 && event.isButtonstate()
            && com.nanonaitor.arsenal.combat.AbilityUseRules.shield(mc.player, mc.player.getHeldItemOffhand())) {
            // Stop the weapon before vanilla checks isHandActive, without starting
            // a shield ourselves or bypassing block/entity interactions.
            BallAndChainInputHandler.cancelForShield(mc.player);
            ModernWeaponInputHandler.cancelMorningCharge(mc.player);
            ModNetwork.CHANNEL.sendToServer(new com.nanonaitor.arsenal.network.ModernWeaponControlMessage(
                com.nanonaitor.arsenal.network.ModernWeaponControlMessage.SHIELD_TAKEOVER, true));
        }
        handleShieldAttack(event);
    }

    /** Called before weapon input as well, making dispatch independent of subscriber order. */
    public static boolean handleShieldAttack(MouseEvent event) {
        if (event == handledAttack) return true;
        Minecraft mc = Minecraft.getMinecraft();
        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        if (mc.currentScreen != null || attackKey >= 0
            || event.getButton() != attackKey + 100) return false;
        EntityPlayerSP player = mc.player;
        if (player == null) return false;
        if (bashOwnsAttack) {
            event.setCanceled(true);
            if (!event.isButtonstate()) bashOwnsAttack = false;
            handledAttack = event;
            return true;
        }
        boolean tartsy = ShieldCombat.isGuarding(player, ItemTartsyShield.class);
        boolean bulwark = ShieldCombat.isGuarding(player, ItemSunWarBulwark.class);
        if (!tartsy && !bulwark) return false;
        handledAttack = event;
        event.setCanceled(true);
        net.minecraft.client.settings.KeyBinding.setKeyBindState(attackKey, false);
        while (mc.gameSettings.keyBindAttack.isPressed()) { /* discard weapon/mining clicks */ }
        if (!event.isButtonstate()) return true;
        bashOwnsAttack = true;
        if (tartsy) {
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
            return true;
        }
        event.setCanceled(true);
        player.swingArm(player.getActiveHand());
        ModNetwork.CHANNEL.sendToServer(new BulwarkBashMessage());
        return true;
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
