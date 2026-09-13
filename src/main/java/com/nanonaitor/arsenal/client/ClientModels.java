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
     if(kind==WeaponKind.BLADE_STAFF || kind==WeaponKind.BATTERING_RAM || kind==WeaponKind.BALL_AND_CHAIN) active|=using;
     if(kind==WeaponKind.SCIMITAR) active=entity.isUsingItem()
         && entity.getMainHandItem().getItem() instanceof ArsenalWeaponItem main && main.kind()==WeaponKind.SCIMITAR
         && entity.getOffhandItem().getItem() instanceof ArsenalWeaponItem off && off.kind()==WeaponKind.SCIMITAR;
     if(kind==WeaponKind.BLADE_STAFF) active=AbilityVisualState.staffReflecting(entity);
     if(entity==Minecraft.getInstance().player && level!=null){
      if(kind==WeaponKind.FLAIL) active=ClientControls.flailActive();
      if(kind==WeaponKind.BALL_AND_CHAIN) active|=ClientControls.ballWindup(level.getGameTime()) || ClientControls.ballRelease(level.getGameTime());
     }
    }
    return active?1:0;
   })));
  for(var shield:new net.minecraft.world.item.Item[]{ModItems.SUN_WAR.get(),ModItems.TARTSY_SHIELD.get()})
   ItemProperties.register(shield,new ResourceLocation("blocking"),(stack,level,entity,seed)->entity!=null && entity.isUsingItem() && entity.getUseItem()==stack?1:0);
 }
}
