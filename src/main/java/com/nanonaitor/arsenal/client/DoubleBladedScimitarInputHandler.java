package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Mouse;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class DoubleBladedScimitarInputHandler {
    private DoubleBladedScimitarInputHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelAttackDuringReflection(MouseEvent event) {
        if (event.getButton() != 0 || !event.isButtonstate()) return;
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null && DoubleBladedScimitarCombat.isReflecting(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void autoAttack(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (player == null || minecraft.currentScreen != null
            || minecraft.playerController == null
            || !(player.getHeldItemMainhand().getItem() instanceof ItemDoubleBladedScimitar)
            || !player.getHeldItemOffhand().isEmpty()
            || DoubleBladedScimitarCombat.isReflecting(player)
            || !attackHeld(minecraft)
            || player.getCooledAttackStrength(0.5F) < 1.0F) return;
        RayTraceResult hit = minecraft.objectMouseOver;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.ENTITY
            && hit.entityHit instanceof EntityLivingBase) {
            minecraft.playerController.attackEntity(player, hit.entityHit);
            player.swingArm(EnumHand.MAIN_HAND);
        } else if (hit == null || hit.typeOfHit == RayTraceResult.Type.MISS) {
            player.swingArm(EnumHand.MAIN_HAND);
            player.resetCooldown();
        }
    }

    private static boolean attackHeld(Minecraft minecraft) {
        if (minecraft.gameSettings.keyBindAttack.isKeyDown()) return true;
        int key = minecraft.gameSettings.keyBindAttack.getKeyCode();
        int button = key + 100;
        return key < 0 && button >= 0 && button < Mouse.getButtonCount()
            && Mouse.isButtonDown(button);
    }
}
