package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

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

}
