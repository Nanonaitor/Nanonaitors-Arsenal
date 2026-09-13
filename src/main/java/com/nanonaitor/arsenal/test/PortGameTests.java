package com.nanonaitor.arsenal.test;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortGameTests {
 @GameTest(template="empty",templateNamespace="forge")
 public static void onlyBallAccelerationRequiresEmptyOffhand(GameTestHelper h){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
   new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ArsenalBallTest"));
  var ball=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.BALL_AND_CHAIN,WeaponTier.IRON).get());
  player.setItemSlot(EquipmentSlot.MAINHAND,ball);
  player.setItemSlot(EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.BALL_CHAIN,true);
  h.assertTrue(ball.getOrCreateTag().getBoolean("ArsenalActive"),"Occupied offhand permits normal charge");
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.BALL_WIND_BOOST,true);
  h.assertTrue(!ball.getOrCreateTag().getBoolean("ArsenalBallBoost"),"Occupied offhand rejects acceleration");
  player.setItemSlot(EquipmentSlot.OFFHAND,net.minecraft.world.item.ItemStack.EMPTY);
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.BALL_CHAIN,true);
  h.assertTrue(ball.getOrCreateTag().getBoolean("ArsenalActive"),"Empty offhand permits charge");
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.BALL_WIND_BOOST,true);
  h.assertTrue(ball.getOrCreateTag().getBoolean("ArsenalBallBoost"),"Empty offhand permits acceleration");
  player.setItemSlot(EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
  com.nanonaitor.arsenal.combat.CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
  h.assertTrue(ball.getOrCreateTag().getBoolean("ArsenalActive") && !ball.getOrCreateTag().getBoolean("ArsenalBallBoost"),"Equipping offhand stops acceleration, not charging");
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.BALL_CHAIN,false);
  h.assertTrue(ball.getOrCreateTag().getBoolean("ArsenalBallThrown"),"Occupied offhand permits throwing");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void shieldInputIsExclusive(GameTestHelper h){
  var player=net.minecraftforge.common.util.FakePlayerFactory.get(h.getLevel(),
   new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ArsenalShieldTest"));
  var flail=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.FLAIL,WeaponTier.IRON).get());
  player.setItemSlot(EquipmentSlot.MAINHAND,flail);
  player.setItemSlot(EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(ModItems.TARTSY_SHIELD.get()));
  player.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
  float cooldown=player.getAttackStrengthScale(0.5F);
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.FLAIL,true);
  h.assertTrue(!flail.getOrCreateTag().getBoolean("ArsenalActive") && player.isUsingItem(),"Shield blocks weapon input without stopping guard");
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.TARTSY_BASH,true);
  h.assertTrue(com.nanonaitor.arsenal.combat.CombatEvents.shieldOwnsInput(player),"Dash continues to own input after guard ends");
  com.nanonaitor.arsenal.combat.CombatEvents.handleControl(player,com.nanonaitor.arsenal.network.ModNetwork.FLAIL,true);
  h.assertTrue(!flail.getOrCreateTag().getBoolean("ArsenalActive") && player.getAttackStrengthScale(0.5F)==cooldown,"Dash does not fire flail or consume its cooldown");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void offhandUsesItsOwnTier(GameTestHelper h){
  var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.getInventory().clearContent();
  player.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.DIAMOND).get()));
  var off=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.SCIMITAR,WeaponTier.WOOD).get());player.setItemSlot(EquipmentSlot.OFFHAND,off);
  var target=h.spawn(EntityType.ZOMBIE,1,2,1);
  h.assertTrue(com.nanonaitor.arsenal.combat.TierEffects.hurtWithStack(off,target,player.damageSources().playerAttack(player),2),"Offhand hit accepted");
  var weakness=target.getEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
  h.assertTrue(weakness!=null && weakness.getAmplifier()==0,"Wood offhand must not inherit diamond main-hand Weakness II");
  player.getInventory().clearContent();h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void armorPiercingCurve(GameTestHelper h){
  var mob=h.spawn(EntityType.ZOMBIE,1,2,1);
  for(float armor:new float[]{0,10,20,40}) for(float tough:new float[]{0,8,16}) for(float damage:new float[]{4,10,30}) for(var tier:WeaponTier.values()){
   mob.getAttribute(Attributes.ARMOR).setBaseValue(armor);
   mob.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(tough);
   float corrected=com.nanonaitor.arsenal.combat.CombatEvents.compensateForArmor(mob,damage,tier.armorPiercePercent()/100F);
   float effectiveArmor=mob.getArmorValue(),effectiveToughness=(float)mob.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
   float reduced=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(damage,effectiveArmor,effectiveToughness);
   float actual=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(corrected,effectiveArmor,effectiveToughness);
   float expected=reduced+(damage-reduced)*tier.armorPiercePercent()/100F;
   h.assertTrue(Math.abs(actual-expected)<0.02F,"Armor piercing curve: "+tier+" damage="+damage+" armor="+armor);
  }
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void linkedClawLifecycle(GameTestHelper h){
  var player=net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());
  player.getInventory().clearContent();
  var main=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.CLAWS,WeaponTier.IRON).get());
  main.setDamageValue(23);main.getOrCreateTag().putString("quality_test","preserve-me");
  player.setItemSlot(EquipmentSlot.MAINHAND,main);
  com.nanonaitor.arsenal.combat.CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
  h.assertTrue(player.getOffhandItem().is(ModItems.get(WeaponKind.LINKED_CLAWS,WeaponTier.IRON).get()),"Linked claw created");
  h.assertTrue(player.getOffhandItem().getDamageValue()==23,"Linked durability copied");
  h.assertTrue(player.getOffhandItem().getOrCreateTag().getString("quality_test").equals("preserve-me"),"Linked item data copied");
  player.setItemSlot(EquipmentSlot.MAINHAND,net.minecraft.world.item.ItemStack.EMPTY);
  com.nanonaitor.arsenal.combat.CombatEvents.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,player));
  h.assertTrue(player.getOffhandItem().isEmpty(),"Linked claw removed when unequipped");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void upgradePreservesData(GameTestHelper h){
  var json=com.google.gson.JsonParser.parseString("{\"ingredients\":[{\"item\":\"nanonaitors_arsenal:claws_iron\"},{\"item\":\"minecraft:diamond\"}],\"result\":{\"item\":\"nanonaitors_arsenal:claws_diamond\"}}").getAsJsonObject();
  var recipe=ModRecipes.UPGRADE.get().fromJson(new net.minecraft.resources.ResourceLocation("nanonaitors_arsenal","test_upgrade"),json);
  var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
   public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
   public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return net.minecraft.world.item.ItemStack.EMPTY;}
  };
  var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,2,1);
  var old=new net.minecraft.world.item.ItemStack(ModItems.get(WeaponKind.CLAWS,WeaponTier.IRON).get());
  old.setDamageValue(old.getMaxDamage()/2);old.setHoverName(net.minecraft.network.chat.Component.literal("Named claw"));old.getOrCreateTag().putString("quality_test","preserved");
  old.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
  grid.setItem(0,old);grid.setItem(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
  var result=recipe.assemble(grid,h.getLevel().registryAccess());
  h.assertTrue(result.getHoverName().getString().equals("Named claw"),"Upgrade name retained");
  h.assertTrue(result.getOrCreateTag().getString("quality_test").equals("preserved"),"Upgrade quality data retained");
  h.assertTrue(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,result)==3,"Upgrade enchantment retained");
  h.assertTrue(Math.abs((float)result.getDamageValue()/result.getMaxDamage()-0.5F)<0.01F,"Upgrade proportional wear retained");
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void registrationAndStats(GameTestHelper h){
  for(var kind:WeaponKind.values()) for(var tier:WeaponTier.values()){
   var item=(ArsenalWeaponItem)ModItems.get(kind,tier).get();
   h.assertTrue(item.tier()==tier && item.kind()==kind,"Registry identity "+kind+"/"+tier);
   h.assertTrue(item.getDefaultInstance().getMaxStackSize()==1,"Weapon must not stack");
   double damage=item.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum()+1;
   if(kind==WeaponKind.SCIMITAR) h.assertTrue(Math.abs(damage-ModItems.roundedScimitarDamage(tier))<0.001,"Scimitar half-point damage");
   if(kind==WeaponKind.CLAWS) h.assertTrue(Math.abs(damage-tier.clawDamage())<0.001,"Claw half-damage");
  }
  h.assertTrue(!h.getLevel().getRecipeManager().getRecipes().isEmpty(),"Recipes loaded");
  for(String tier:new String[]{"wood","stone","iron","diamond"})
   h.assertTrue(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK).getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,new net.minecraft.resources.ResourceLocation(ArsenalMod.MOD_ID,"battering_ram/"+tier))).isPresent(),"Ram block tier must load: "+tier);
  for(var kind:WeaponKind.values()) if(kind!=WeaponKind.LINKED_CLAWS)
   h.assertTrue(h.getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(ArsenalMod.MOD_ID,kind.id+"_iron")).isPresent(),"Iron crafting recipe: "+kind);
  h.succeed();
 }
 @GameTest(template="empty",templateNamespace="forge",timeoutTicks=80)
 public static void stunRestoresAi(GameTestHelper h){
  var mob=h.spawn(EntityType.ZOMBIE,1,2,1);
  mob.setNoAi(false);mob.addEffect(new MobEffectInstance(ModEffects.STUNNED.get(),10));
  h.runAfterDelay(3,()->h.assertTrue(mob.isNoAi(),"Stun must suspend AI"));
  h.runAfterDelay(20,()->{h.assertTrue(!mob.isNoAi(),"AI must resume after stun");h.succeed();});
 }
 @GameTest(template="empty",templateNamespace="forge")
 public static void fractureDoesNotDamageEquipment(GameTestHelper h){
  var mob=h.spawn(EntityType.ZOMBIE,1,2,1);
  var chest=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_CHESTPLATE);
  mob.setItemSlot(EquipmentSlot.CHEST,chest);
  double before=mob.getAttributeValue(Attributes.ARMOR);
  mob.addEffect(new MobEffectInstance(ModEffects.ARMOR_FRACTURE.get(),100,0));
  h.assertTrue(mob.getAttributeValue(Attributes.ARMOR)<before,"Fracture lowers armor stat");
  h.assertTrue(chest.getDamageValue()==0,"Fracture must not damage dropped armor");
  h.succeed();
 }
}
