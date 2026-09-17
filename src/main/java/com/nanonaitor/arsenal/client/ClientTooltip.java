package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;

/** Client-only keyboard state kept out of tooltip method signatures for dedicated-server safety. */
public final class ClientTooltip {
    public static boolean expanded() { return net.minecraft.client.gui.screens.Screen.hasShiftDown(); }
    public static void requirements(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        if(!(event.getItemStack().getItem() instanceof com.nanonaitor.arsenal.item.ArsenalWeaponItem)
            || !com.nanonaitor.arsenal.compat.LevelRequirements.active())return;
        int required=com.nanonaitor.arsenal.compat.LevelRequirements.required(event.getItemStack());
        if(required<=0)return;
        var player=event.getEntity()!=null?event.getEntity():Minecraft.getInstance().player;
        int current=player==null?-1:com.nanonaitor.arsenal.compat.LevelRequirements.level(player);
        com.nanonaitor.arsenal.compat.RequirementTooltip.append(event.getToolTip(),required,current);
    }
    private ClientTooltip() {}
}
