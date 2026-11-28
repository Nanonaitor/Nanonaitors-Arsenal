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
    /**
     * Explains Arsenal's enchantments (and Knockback on the Ball and Chain and the
     * Battering Ram) right under their names, on weapons and enchanted books.
     */
    public static void explainEnchantments(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        var stack=event.getItemStack();
        var enchantments=net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack);
        if(enchantments.isEmpty())return;
        var tip=event.getToolTip();
        for(var entry:enchantments.entrySet()){
            var id=net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if(id==null)continue;
            String explanation=null;
            if(com.nanonaitor.arsenal.ArsenalMod.MOD_ID.equals(id.getNamespace())){
                String key="enchantment."+id.getNamespace()+"."+id.getPath()+".desc";
                if(net.minecraft.client.resources.language.I18n.exists(key))explanation=net.minecraft.client.resources.language.I18n.get(key);
            } else if(entry.getKey()==net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK
                && stack.getItem() instanceof com.nanonaitor.arsenal.item.ArsenalWeaponItem weapon){
                if(weapon.kind()==com.nanonaitor.arsenal.item.WeaponKind.BALL_AND_CHAIN)explanation="Also shoves back melee attackers you guard against.";
                if(weapon.kind()==com.nanonaitor.arsenal.item.WeaponKind.BATTERING_RAM)explanation="Adds to the ram's own push, on normal and charge hits.";
            }
            if(explanation==null)continue;
            String name=entry.getKey().getFullname(entry.getValue()).getString();
            for(int i=0;i<tip.size();i++){
                if(tip.get(i).getString().equals(name)){
                    tip.add(i+1,net.minecraft.network.chat.Component.literal("  "+explanation).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    break;
                }
            }
        }
    }
    private ClientTooltip() {}
}
