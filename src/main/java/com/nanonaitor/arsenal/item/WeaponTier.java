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
 SENTIENT("sentient",material(3,1000,14,16,1,"nanonaitors_arsenal:materials/living"),5,3),
 // Player-defined materials (nanonaitors_arsenal-materials.toml). Unused slots register nothing.
 CUSTOM_1("custom_1",Tiers.IRON,4,2), CUSTOM_2("custom_2",Tiers.IRON,4,2), CUSTOM_3("custom_3",Tiers.IRON,4,2),
 CUSTOM_4("custom_4",Tiers.IRON,4,2), CUSTOM_5("custom_5",Tiers.IRON,4,2), CUSTOM_6("custom_6",Tiers.IRON,4,2),
 CUSTOM_7("custom_7",Tiers.IRON,4,2), CUSTOM_8("custom_8",Tiers.IRON,4,2);
 // Built-in tiers never change these; a custom slot takes them from its config entry.
 public String id; public Tier material; public int fractureCap,ramBreakLevel;
 private boolean configured; private String displayName=""; private int color=0xFFFFFF, harvest=2;
 WeaponTier(String id,Tier material,int fractureCap,int ramBreakLevel){this.id=id;this.material=material;this.fractureCap=fractureCap;this.ramBreakLevel=ramBreakLevel;}
 public boolean custom(){return name().startsWith("CUSTOM_");}
 /** Built-in tiers always; a custom slot only once a config entry fills it. */
 public boolean active(){return !custom() || configured;}
 /** Custom tiers: the name shown in place of "Iron", and the tint of their metal. */
 public String displayName(){return displayName;}
 public int color(){return color;}
 /** Built-in tiers, then the configured custom ones. */
 public static java.util.List<WeaponTier> activeValues(){
  return java.util.Arrays.stream(values()).filter(WeaponTier::active).toList();
 }
 /** The built-in tier a custom material behaves like for tier rules (by harvest level). */
 public WeaponTier baseline(){
  if(!custom())return this;
  return switch(Math.max(0,harvest)){case 0->WOOD;case 1->STONE;case 2->IRON;case 3->DIAMOND;default->NETHERITE;};
 }
 /**
  * Fills the custom slots from the config, once, before items are registered.
  * Entry: id, Display Name, #RRGGBB, ingredient (modid:item or #tag), harvest level,
  * durability, attack damage, enchantability.
  */
 public static synchronized java.util.List<String> configureCustom(java.util.List<? extends String> entries){
  java.util.List<String> problems=new java.util.ArrayList<>();
  java.util.Set<String> taken=new java.util.HashSet<>();
  for(WeaponTier tier:values())if(!tier.custom())taken.add(tier.id);
  WeaponTier[] slots={CUSTOM_1,CUSTOM_2,CUSTOM_3,CUSTOM_4,CUSTOM_5,CUSTOM_6,CUSTOM_7,CUSTOM_8};
  int slot=0;
  for(String entry:entries){
   if(entry==null || entry.isBlank() || entry.trim().startsWith("#"))continue;
   if(slot>=slots.length){problems.add("only "+slots.length+" custom materials are supported; ignoring '"+entry+"'");continue;}
   try{
    CustomSpec spec=CustomSpec.parse(entry);
    if(!taken.add(spec.id()))throw new IllegalArgumentException("id '"+spec.id()+"' is already used");
    WeaponTier tier=slots[slot++];
    tier.id=spec.id(); tier.displayName=spec.displayName(); tier.color=spec.color(); tier.harvest=spec.harvest();
    tier.ramBreakLevel=Math.min(3,spec.harvest()); tier.fractureCap=Math.min(5,2+spec.harvest());
    tier.material=customTier(spec); tier.configured=true;
   }catch(IllegalArgumentException bad){problems.add("skipping custom material '"+entry+"': "+bad.getMessage());}
  }
  return problems;
 }
 /** The ingredient that crafts and repairs a custom material, as written in its entry. */
 public String ingredient(){return ingredient;}
 private String ingredient="minecraft:iron_ingot";
 private static Tier customTier(CustomSpec spec){
  for(WeaponTier tier:values())if(tier.custom() && tier.id.equals(spec.id()))tier.ingredient=spec.ingredient();
  return new Tier(){
   public int getUses(){return spec.durability();} public float getSpeed(){return 2.0F+2.0F*spec.harvest();}
   public float getAttackDamageBonus(){return spec.damage();} public int getLevel(){return spec.harvest();}
   public int getEnchantmentValue(){return spec.enchantability();}
   public Ingredient getRepairIngredient(){return spec.ingredientValue();}
  };
 }
 /** One materials entry, validated. */
 public record CustomSpec(String id,String displayName,int color,String ingredient,int harvest,int durability,float damage,int enchantability){
  public static CustomSpec parse(String entry){
   String[] p=entry.split(",");
   if(p.length!=8)throw new IllegalArgumentException("needs 8 comma-separated values, got "+p.length);
   for(int i=0;i<p.length;i++)p[i]=p[i].trim();
   if(!p[0].matches("[a-z0-9_]{1,24}"))throw new IllegalArgumentException("id must be 1-24 of a-z, 0-9, _");
   if(p[1].isEmpty())throw new IllegalArgumentException("display name is empty");
   String hex=p[2].startsWith("#")?p[2].substring(1):p[2];
   if(!hex.matches("[0-9a-fA-F]{6}"))throw new IllegalArgumentException("color must be #RRGGBB");
   String item=p[3].startsWith("#")?p[3].substring(1):p[3];
   if(net.minecraft.resources.ResourceLocation.tryParse(item)==null || !item.contains(":"))
    throw new IllegalArgumentException("ingredient must be modid:item or #modid:tag");
   try{
    int harvest=Integer.parseInt(p[4]),durability=Integer.parseInt(p[5]),enchant=Integer.parseInt(p[7]);
    float damage=Float.parseFloat(p[6]);
    if(harvest<0 || harvest>4)throw new IllegalArgumentException("harvest level must be 0-4");
    if(durability<1)throw new IllegalArgumentException("durability must be at least 1");
    if(damage<0)throw new IllegalArgumentException("attack damage must not be negative");
    if(enchant<0)throw new IllegalArgumentException("enchantability must not be negative");
    return new CustomSpec(p[0],p[1],Integer.parseInt(hex,16),p[3],harvest,durability,damage,enchant);
   }catch(NumberFormatException bad){throw new IllegalArgumentException("harvest, durability, damage and enchantability must be numbers");}
  }
  /** The ingredient as JSON, for generated recipes. */
  public com.google.gson.JsonObject ingredientJson(){
   var json=new com.google.gson.JsonObject();
   if(ingredient.startsWith("#"))json.addProperty("tag",ingredient.substring(1));else json.addProperty("item",ingredient);
   return json;
  }
  Ingredient ingredientValue(){
   if(ingredient.startsWith("#"))return Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new net.minecraft.resources.ResourceLocation(ingredient.substring(1))));
   var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(ingredient));
   return item==null?Ingredient.EMPTY:Ingredient.of(item);
  }
 }
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
 public boolean available(){return custom()?configured:ordinal()<=NETHERITE.ordinal() || !material.getRepairIngredient().isEmpty();}
 public int weaknessLevel(){if(custom())return baseline().weaknessLevel();return this==SENTIENT?3:switch(this){case WOOD,STONE,COPPER,GOLD,IRON,SILVER,BRONZE->1;default->2;};}
 public int armorPiercePercent(){if(custom())return baseline().armorPiercePercent();return switch(this){case WOOD->25;case STONE,COPPER,GOLD,BRONZE->50;case IRON,SILVER,STEEL,UMBRIUM->75;default->100;};}
 /** Fraction of incoming damage returned by a Blade Staff. Wood is handled as a flat point. */
 public float reflectionMultiplier(){if(custom())return baseline().reflectionMultiplier();return switch(this){
  case WOOD->0; case STONE->.10F; case COPPER,BRONZE->.25F;
  case IRON,GOLD,SILVER,STEEL,UMBRIUM->.50F;
  case DIAMOND,DRAGONBONE->.75F; default->1.0F;
 };}
}
