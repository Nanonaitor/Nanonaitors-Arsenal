package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShield;

/** Prevent weapon abilities from competing with shields, without intercepting use.
 * Minecraft must try entity/block interactions before falling back to shield use.
 */
public final class ShieldUsePriority {
    private ShieldUsePriority() {}
    public static boolean requested(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().player && ShieldInputHandler.suppressWeaponAttack()) return true;
        Minecraft mc=Minecraft.getMinecraft();
        boolean shield=com.nanonaitor.arsenal.combat.AbilityUseRules.shield(player,player.getHeldItemMainhand())
            || com.nanonaitor.arsenal.combat.AbilityUseRules.shield(player,player.getHeldItemOffhand());
        return shield && (com.nanonaitor.arsenal.combat.AbilityUseRules.activeShield(player)
            || mc.gameSettings.keyBindUseItem.isKeyDown() || physicallyUsing(mc));
    }
    private static boolean physicallyUsing(Minecraft mc){int k=mc.gameSettings.keyBindUseItem.getKeyCode();return k<0?k+100>=0&&k+100<org.lwjgl.input.Mouse.getButtonCount()&&org.lwjgl.input.Mouse.isButtonDown(k+100):k>0&&k<org.lwjgl.input.Keyboard.KEYBOARD_SIZE&&org.lwjgl.input.Keyboard.isKeyDown(k);}
}
