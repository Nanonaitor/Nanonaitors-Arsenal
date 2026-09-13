package com.nanonaitor.arsenal.client;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.client.Minecraft;

/** Opt-in development-only model bake check; never activates in a normal profile. */
final class ClientSmokeTest {
 private static boolean finished;
 static void tick(){
  if(finished || !Boolean.getBoolean("arsenal.clientSmokeTest"))return;
  Minecraft mc=Minecraft.getInstance();
  if(mc.getOverlay()!=null || mc.screen==null)return;
  var manager=mc.getModelManager();var missing=manager.getMissingModel();
  int count=0;
  for(var item:ModItems.ITEMS.getEntries()){
   var extension=net.minecraftforge.client.extensions.common.IClientItemExtensions.of(item.get());
   if(item.get() instanceof com.nanonaitor.arsenal.item.ArsenalWeaponItem && !(extension instanceof WeaponClientExtensions))
    throw new IllegalStateException("Missing weapon client extension: "+item.getId());
   if(item.get() instanceof com.nanonaitor.arsenal.item.ArsenalShieldItem){
    if(!(extension instanceof ShieldClientExtensions))throw new IllegalStateException("Missing shield client extension: "+item.getId());
    extension.getCustomRenderer(); // Bake the original entity geometry too.
   }
   var model=manager.getModel(new net.minecraft.client.resources.model.ModelResourceLocation(item.getId(),"inventory"));
   if(model==missing)throw new IllegalStateException("Arsenal missing baked model: "+item.getId());
   if(item.get() instanceof com.nanonaitor.arsenal.item.ArsenalWeaponItem weapon
       && (weapon.kind()==com.nanonaitor.arsenal.item.WeaponKind.FLAIL
           || weapon.kind()==com.nanonaitor.arsenal.item.WeaponKind.BALL_AND_CHAIN)){
    var stack=new net.minecraft.world.item.ItemStack(item.get());
    stack.getOrCreateTag().putBoolean("ArsenalActive",true);
    var active=mc.getItemRenderer().getModel(stack,null,null,0);
    if(active.usesBlockLight() || active.usesBlockLight()!=model.usesBlockLight())
     throw new IllegalStateException("Animated inventory icon must retain flat GUI lighting: "+item.getId());
    if(weapon.tier()==com.nanonaitor.arsenal.item.WeaponTier.DIAMOND)
     com.mojang.logging.LogUtils.getLogger().info("ARSENAL GUI LIGHTING {}: idleBlockLight={}, activeBlockLight={}",
         item.getId(),model.usesBlockLight(),active.usesBlockLight());
    for(var context:new net.minecraft.world.item.ItemDisplayContext[]{
        net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
        net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
        net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND}){
     var hand=active.applyTransform(context,new com.mojang.blaze3d.vertex.PoseStack(),false);
     if(!hand.getQuads(null,null,net.minecraft.util.RandomSource.create()).isEmpty())
      throw new IllegalStateException("Active chain-weapon hand sprite must be hidden: "+item.getId()+" "+context);
    }
    var gui=active.applyTransform(net.minecraft.world.item.ItemDisplayContext.GUI,new com.mojang.blaze3d.vertex.PoseStack(),false);
    if(gui.usesBlockLight())
     throw new IllegalStateException("GUI perspective must use flat lighting: "+item.getId());
    if(gui.getQuads(null,null,net.minecraft.util.RandomSource.create()).stream()
        .noneMatch(q->q.getSprite().contents().name().getPath().contains(
            weapon.kind()==com.nanonaitor.arsenal.item.WeaponKind.FLAIL?"flail_swinging_":"_active_sprite")))
     throw new IllegalStateException("Active chain-weapon inventory icon must animate: "+item.getId());
   }
   count++;
  }
  finished=true;
  try {
   // Reproduce the table involved in the reported dual-Scimitar crash.
   Class<?> switches=Class.forName("net.minecraft.client.model.HumanoidModel$1");
   for(var field:switches.getDeclaredFields()){
    if(field.getType()==int[].class && field.getName().contains("ArmPose")){
     field.setAccessible(true);
     if(((int[])field.get(null)).length<net.minecraft.client.model.HumanoidModel.ArmPose.values().length)
      throw new IllegalStateException("Humanoid arm-pose switch table is stale");
    }
   }
  } catch(ReflectiveOperationException error){throw new IllegalStateException("Arm-pose regression check failed",error);}
  com.mojang.logging.LogUtils.getLogger().info("ARSENAL CLIENT SMOKE PASS: {} registered item models baked",count);
  mc.stop();
 }
}
