package com.nanonaitor.arsenal.combat;
import com.nanonaitor.arsenal.config.*;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;

/** Optional tier effects use registry IDs and never require another mod's classes. */
public final class TierEffects {
 private record AttackContext(java.util.UUID owner,ItemStack stack){}
 private static final ThreadLocal<AttackContext> ATTACK_CONTEXT=new ThreadLocal<>();
 public static ItemStack attackingStack(Player player){var context=ATTACK_CONTEXT.get();return context!=null && context.owner.equals(player.getUUID())?context.stack:player.getMainHandItem();}
 public static boolean hurtWithStack(ItemStack stack,LivingEntity target,net.minecraft.world.damagesource.DamageSource source,float amount){
  if(!(source.getEntity() instanceof Player p))return target.hurt(source,amount);
  var previous=ATTACK_CONTEXT.get();ATTACK_CONTEXT.set(new AttackContext(p.getUUID(),stack));
  try{return target.hurt(source,amount);}finally{if(previous==null)ATTACK_CONTEXT.remove();else ATTACK_CONTEXT.set(previous);}
 }
 public static void register(){
  var bus=MinecraftForge.EVENT_BUS;
  bus.addListener(TierEffects::bonusDamage);bus.addListener(TierEffects::confirmedHit);
  bus.addListener(TierEffects::stunAllowed);bus.addListener(TierEffects::heldEffects);
  bus.addListener(TierEffects::evolution);
  bus.addListener(TierEffects::anvilRestrictions);
 }
 public static boolean matchingArmor(Player player,WeaponTier tier){
  if(!ArsenalConfig.MATCHING_ARMOR_BONUSES.get() || (tier!=WeaponTier.GOLD && tier!=WeaponTier.SILVER))return false;
  for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){
   ItemStack s=player.getItemBySlot(slot);
   if(!(s.getItem() instanceof ArmorItem a) || a.getEquipmentSlot()!=slot)return false;
   if(tier==WeaponTier.GOLD){if(a.getMaterial()!=ArmorMaterials.GOLD)return false;}
   else {
    var id=ForgeRegistries.ITEMS.getKey(s.getItem());
    if(!s.is(TagKey.create(Registries.ITEM,new ResourceLocation("forge","armors/silver"))) && (id==null || !id.getPath().startsWith("silver_")))return false;
   }
  }
  return true;
 }
 public static int maxBallCharges(Player player,WeaponTier tier){return tier==WeaponTier.GOLD || (tier==WeaponTier.SILVER && matchingArmor(player,tier))?2:3;}
 public static int effectiveCharge(Player player,WeaponTier tier,int charge){return maxBallCharges(player,tier)==2 && charge>=2?3:charge;}
 public static int weakness(Player player,WeaponTier tier){return matchingArmor(player,tier)?1:tier.weaknessLevel()-1;}
 private static ArsenalWeaponItem weapon(net.minecraft.world.damagesource.DamageSource source){
  return source.getEntity() instanceof Player p && source.getDirectEntity()==p && attackingStack(p).getItem() instanceof ArsenalWeaponItem w?w:null;
 }
 private static void bonusDamage(LivingHurtEvent e){
  var w=weapon(e.getSource());if(w==null)return;
  var t=w.tier();var target=e.getEntity();float bonus=0;
  String id=ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
  if(t==WeaponTier.SILVER && target.getMobType()==MobType.UNDEAD)bonus+=2;
  if(t.id.contains("myrmex") || t.id.endsWith("venom")){
   if(target.getMobType()!=MobType.ARTHROPOD)bonus+=4;
   if(id.equals("iceandfire:deathworm"))bonus+=4;
  }
  if(t==WeaponTier.FLAMED_DRAGONBONE && id.equals("iceandfire:ice_dragon"))bonus+=13.5F;
  if(t==WeaponTier.ICED_DRAGONBONE && id.equals("iceandfire:fire_dragon"))bonus+=13.5F;
  if(t==WeaponTier.ELECTRIC_DRAGONBONE && (id.equals("iceandfire:fire_dragon") || id.equals("iceandfire:ice_dragon")))bonus+=6.75F;
  e.setAmount(e.getAmount()+bonus);
 }
 private static void confirmedHit(LivingDamageEvent e){
  if(e.getAmount()<=0 || e.getEntity().level().isClientSide)return;
  var w=weapon(e.getSource());if(w==null)return;
  var target=e.getEntity();var t=w.tier();Player player=(Player)e.getSource().getEntity();
  ConfiguredEffects.apply(target,w.kind().id+"Extra",200,0);
  if(t.id.endsWith("venom"))ConfiguredEffects.apply(target,"venomHit",200,2);
  if(t==WeaponTier.FLAMED_DRAGONBONE){target.setSecondsOnFire(5);knockBack(target,player);}
  if(t==WeaponTier.ICED_DRAGONBONE){freeze(target);ConfiguredEffects.apply(target,"icedDragonboneHit",100,2);knockBack(target,player);}
  if(t==WeaponTier.ELECTRIC_DRAGONBONE && target.level() instanceof ServerLevel level){
   long now=level.getGameTime();
   if(player.getPersistentData().getLong("ArsenalElectricTick")!=now){
    player.getPersistentData().putLong("ArsenalElectricTick",now);
    var bolt=EntityType.LIGHTNING_BOLT.create(level);
    if(bolt!=null){bolt.moveTo(target.position());bolt.setVisualOnly(true);level.addFreshEntity(bolt);}
   }
   knockBack(target,player);
  }
  if(t==WeaponTier.LIVING || t==WeaponTier.SENTIENT){
   String effect=switch(w.kind()){case MORNING_STAR->"livingMorningStar";case CLAWS,LINKED_CLAWS->"livingClaws";case FLAIL->"livingFlail";case BALL_AND_CHAIN->isParasite(target)?"livingBallAndChain":"";default->"";};
   ConfiguredEffects.apply(target,effect,200,t==WeaponTier.SENTIENT?1:0);
  }
 }
 private static boolean isParasite(LivingEntity e){return ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).getNamespace().equals("srparasites");}
 private static void evolution(LivingDeathEvent e){
  var w=weapon(e.getSource());if(w==null || w.tier()!=WeaponTier.LIVING || !isParasite(e.getEntity()))return;
  Player player=(Player)e.getSource().getEntity();ItemStack old=attackingStack(player);
  var hand=old==player.getOffhandItem()?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND;
  var tag=old.getOrCreateTag();int progress=tag.getInt("srpkills")+(int)e.getEntity().getMaxHealth();tag.putInt("srpkills",progress);
  if(progress<=ArsenalConfig.EVOLUTION_THRESHOLD.get())return;
  ItemStack evolved=new ItemStack(com.nanonaitor.arsenal.registry.ModItems.get(w.kind(),WeaponTier.SENTIENT).get());
  evolved.setTag(tag.copy());evolved.getTag().remove("srpkills");
  evolved.setDamageValue(Math.min(evolved.getMaxDamage()-1,Math.round((float)old.getDamageValue()/Math.max(1,old.getMaxDamage())*evolved.getMaxDamage())));
  player.setItemInHand(hand,evolved);
  player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,net.minecraft.sounds.SoundSource.PLAYERS,0.7F,1.3F);
 }
 private static void knockBack(LivingEntity target,Player source){target.knockback(1,source.getX()-target.getX(),source.getZ()-target.getZ());}
 private static void freeze(LivingEntity target){
  // 1.20.1 Ice & Fire exposes a LazyOptional EntityData capability, unlike 1.12.2.
  try {
   Class<?> provider=Class.forName("com.github.alexthe666.iceandfire.entity.props.EntityDataProvider");
   Object optional=provider.getMethod("getCapability",Entity.class).invoke(null,target);
   if(optional instanceof net.minecraftforge.common.util.LazyOptional<?> lazy) lazy.ifPresent(data->{
    try{Object frozen=data.getClass().getField("frozenData").get(data);frozen.getClass().getMethod("setFrozen",LivingEntity.class,int.class).invoke(frozen,target,200);}catch(ReflectiveOperationException ignored){}
   });
  }catch(ReflectiveOperationException|LinkageError ignored){}
 }
 private static void stunAllowed(MobEffectEvent.Applicable e){
  if(e.getEffectInstance().getEffect()==ModEffects.STUNNED.get() && ArsenalConfig.STUN_BLACKLIST.get().contains(ForgeRegistries.ENTITY_TYPES.getKey(e.getEntity().getType()).toString()))e.setResult(Event.Result.DENY);
 }
 private static void anvilRestrictions(net.minecraftforge.event.AnvilUpdateEvent e){
  if(!(e.getRight().getItem() instanceof EnchantedBookItem))return;
  for(var enchantment:net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(e.getRight()).keySet()){
   var id=ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
   if(id!=null && id.getNamespace().equals("nanonaitors_arsenal") && !com.nanonaitor.arsenal.enchantment.ModEnchantments.enabled(id.getPath())){e.setCanceled(true);return;}
   if(e.getLeft().getItem() instanceof ArsenalShieldItem && !ArsenalConfig.SHIELD_ENCHANTMENTS.get()
      && enchantment!=net.minecraft.world.item.enchantment.Enchantments.UNBREAKING && enchantment!=net.minecraft.world.item.enchantment.Enchantments.MENDING){e.setCanceled(true);return;}
  }
 }
 private static void heldEffects(TickEvent.PlayerTickEvent e){
  if(e.phase!=TickEvent.Phase.END || e.player.level().isClientSide)return;
  Player p=e.player;
  if(!(p.getMainHandItem().getItem() instanceof ArsenalWeaponItem w) || (w.tier()!=WeaponTier.LIVING && w.tier()!=WeaponTier.SENTIENT))return;
  int amp=w.tier()==WeaponTier.SENTIENT?1:0;
  if(w.kind()==WeaponKind.BATTERING_RAM){var rage=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("srparasites","rage"));if(rage!=null)p.addEffect(new MobEffectInstance(rage,10,amp,false,false));}
  if(w.kind()==WeaponKind.BLADE_STAFF && p.tickCount%20==0){
   var coth=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("srparasites","coth"));double radius=amp==1?10:5;
   if(coth!=null)for(var target:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),x->x.distanceToSqr(p)<=radius*radius))target.removeEffect(coth);
  }
 }
}
