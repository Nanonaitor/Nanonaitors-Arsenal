package com.nanonaitor.arsenal.item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
public enum WeaponTier {
 WOOD("wood",Tiers.WOOD,2,0), STONE("stone",Tiers.STONE,3,1), COPPER("copper",copper(),3,1), GOLD("gold",Tiers.GOLD,3,1), IRON("iron",Tiers.IRON,4,2), DIAMOND("diamond",Tiers.DIAMOND,5,3), NETHERITE("netherite",Tiers.NETHERITE,5,4),
 SILVER("silver",material(2,460,7,1.5F,16,"forge:ingots/silver"),4,2),
 BRONZE("bronze",material(2,200,6,2,12,"forge:ingots/bronze"),4,2),
 STEEL("steel",material(3,480,8,2.5F,14,"forge:ingots/steel"),5,3),
 UMBRIUM("umbrium",material(2,320,7,2,20,"nanonaitors_arsenal:materials/umbrium"),4,3),
 DRAGONBONE("dragonbone",material(3,1660,10,4,22,"nanonaitors_arsenal:materials/dragonbone"),5,3),
 FLAMED_DRAGONBONE("flamed_dragonbone",material(3,2000,12,5.5F,22,"nanonaitors_arsenal:materials/dragonbone"),5,3),
 ICED_DRAGONBONE("iced_dragonbone",material(3,2000,12,5.5F,22,"nanonaitors_arsenal:materials/dragonbone"),5,3),
 ELECTRIC_DRAGONBONE("electric_dragonbone",material(3,2000,12,5.5F,22,"nanonaitors_arsenal:materials/dragonbone"),5,3),
 DESERT_MYRMEX("desert_myrmex",material(2,600,7,1,8,"nanonaitors_arsenal:materials/desert_myrmex"),4,2),
 JUNGLE_MYRMEX("jungle_myrmex",material(2,600,7,1,8,"nanonaitors_arsenal:materials/jungle_myrmex"),4,2),
 DESERT_VENOM("desert_venom",material(2,600,7,1,8,"nanonaitors_arsenal:materials/desert_myrmex"),4,2),
 JUNGLE_VENOM("jungle_venom",material(2,600,7,1,8,"nanonaitors_arsenal:materials/jungle_myrmex"),4,2),
 LIVING("living",material(3,1000,12,11,1,"nanonaitors_arsenal:materials/living"),5,3),
 SENTIENT("sentient",material(3,1000,14,16,1,"nanonaitors_arsenal:materials/living"),5,3);
 public final String id; public final Tier material; public final int fractureCap,ramBreakLevel;
 WeaponTier(String id,Tier material,int fractureCap,int ramBreakLevel){this.id=id;this.material=material;this.fractureCap=fractureCap;this.ramBreakLevel=ramBreakLevel;}
 private static Tier copper(){return new Tier(){
  public int getUses(){return 190;} public float getSpeed(){return 5;}
  public float getAttackDamageBonus(){return 1;} public int getLevel(){return 1;}
  public int getEnchantmentValue(){return 13;} public Ingredient getRepairIngredient(){return Ingredient.of(Items.COPPER_INGOT);}
 };}
 public float swordDamage(){return 4+material.getAttackDamageBonus();}
 public float clawDamage(){return swordDamage()*0.5F;}
 private static Tier material(int harvest,int uses,float speed,float damage,int enchant,String tag){return new Tier(){
  public int getUses(){return uses;} public float getSpeed(){return speed;}
  public float getAttackDamageBonus(){return damage;} public int getLevel(){return harvest;}
  public int getEnchantmentValue(){return enchant;}
  public Ingredient getRepairIngredient(){return Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new net.minecraft.resources.ResourceLocation(tag)));}
 };}
 public boolean available(){return ordinal()<=NETHERITE.ordinal() || !material.getRepairIngredient().isEmpty();}
 public int weaknessLevel(){return this==SENTIENT?3:switch(this){case WOOD,STONE,COPPER,GOLD,IRON,SILVER,BRONZE->1;default->2;};}
 public int armorPiercePercent(){return switch(this){case WOOD->25;case STONE,COPPER,GOLD,BRONZE->50;case IRON,SILVER,STEEL,UMBRIUM->75;default->100;};}
}
