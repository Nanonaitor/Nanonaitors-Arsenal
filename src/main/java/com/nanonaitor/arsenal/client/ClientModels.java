package com.nanonaitor.arsenal.client;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
final class ClientModels {
 static void register(){
  ModItems.WEAPONS.forEach((kind,tiers)->tiers.values().forEach(entry->
   ItemProperties.register(entry.get(),new ResourceLocation(ArsenalMod.MOD_ID,"active"),(stack,level,entity,seed)->{
    boolean active=stack.hasTag() && stack.getTag().getBoolean("ArsenalActive");
    if(entity!=null){
     boolean using=entity.isUsingItem() && entity.getUseItem()==stack;
     if(kind==WeaponKind.BLADE_STAFF || kind==WeaponKind.BATTERING_RAM) active|=using;
     if(kind==WeaponKind.SCIMITAR) active=entity instanceof net.minecraft.world.entity.player.Player p
         && com.nanonaitor.arsenal.combat.ParityRules.guarding(p)
         && entity.getMainHandItem().getItem() instanceof ArsenalWeaponItem main && main.kind()==WeaponKind.SCIMITAR
         && entity.getOffhandItem().getItem() instanceof ArsenalWeaponItem off && off.kind()==WeaponKind.SCIMITAR;
     if(kind==WeaponKind.BLADE_STAFF) active=AbilityVisualState.staffReflecting(entity);
     if(entity==Minecraft.getInstance().player && level!=null){
      if(kind==WeaponKind.FLAIL) active=entity instanceof net.minecraft.world.entity.player.Player p && WeaponVisualState.flailSpinning(p);
      if(kind==WeaponKind.BALL_AND_CHAIN && entity instanceof net.minecraft.world.entity.player.Player p)
       active|=!Double.isNaN(WeaponVisualState.ballWindupAngle(p,0)) || WeaponVisualState.ballThrowProgress(p,0)>=0;
     }
    }
    return active?1:0;
   })));
  ModItems.WEAPONS.get(WeaponKind.BALL_AND_CHAIN).values().forEach(entry ->
   ItemProperties.register(entry.get(),new ResourceLocation(ArsenalMod.MOD_ID,"guarding"),(stack,level,entity,seed)->
    entity!=null && entity.isUsingItem() && entity.getUseItem()==stack && entity.getOffhandItem().isEmpty()
      && entity.isCrouching()
      && !(stack.hasTag() && stack.getTag().getBoolean("ArsenalActive")) ? 1 : 0));
  // Third-person weapon poses: the staff held crosswise in both hands, the ram at the hip.
  ModItems.WEAPONS.get(WeaponKind.BLADE_STAFF).values().forEach(entry ->
   ItemProperties.register(entry.get(),new ResourceLocation(ArsenalMod.MOD_ID,"posed"),(stack,level,entity,seed)->
    entity instanceof net.minecraft.world.entity.player.Player p && p.getMainHandItem()==stack
      && com.nanonaitor.arsenal.client.pose.WeaponPoses.holdsStaffCrosswise(p) ? 1 : 0));
  ModItems.WEAPONS.get(WeaponKind.BATTERING_RAM).values().forEach(entry ->
   ItemProperties.register(entry.get(),new ResourceLocation(ArsenalMod.MOD_ID,"hip"),(stack,level,entity,seed)->
    entity instanceof net.minecraft.world.entity.player.Player p && p.getMainHandItem()==stack
      ? com.nanonaitor.arsenal.client.pose.WeaponPoses.ramAtHip(p) : 0));
  for(var shield:new net.minecraft.world.item.Item[]{ModItems.SUN_WAR.get(),ModItems.TARTSY_SHIELD.get()})
   ItemProperties.register(shield,new ResourceLocation("blocking"),(stack,level,entity,seed)->entity!=null && entity.isUsingItem() && entity.getUseItem()==stack?1:0);
 }
}
