package com.nanonaitor.arsenal.enchantment;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.*;
public final class ModEnchantments {
 public static final DeferredRegister<Enchantment> ENCHANTMENTS=DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,ArsenalMod.MOD_ID);
 public static final RegistryObject<Enchantment> LONG_CHAIN=register("long_chain",2,true);
 public static final RegistryObject<Enchantment> ROTATION_FORCE=register("rotation_force",2,true);
 public static final RegistryObject<Enchantment> RECOVERY=register("recovery",1,false);
 public static final RegistryObject<Enchantment> BREECHED=register("breeched",1,false);
 private static RegistryObject<Enchantment> register(String id,int max,boolean chain){
  return ENCHANTMENTS.register(id,()->new Enchantment(Enchantment.Rarity.RARE,EnchantmentCategory.BREAKABLE,new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND}){
   public int getMaxLevel(){return max;} public int getMinCost(int level){return 10+15*(level-1);} public int getMaxCost(int level){return getMinCost(level)+30;}
   public boolean isDiscoverable(){return enabled(id);}
   public boolean isTradeable(){return enabled(id);}
   public boolean isAllowedOnBooks(){return enabled(id);}
   public boolean isCurse(){return id.equals("breeched");}
   public boolean isTreasureOnly(){return id.equals("breeched");}
   public boolean canEnchant(ItemStack s){return enabled(id) && (chain ? s.getItem() instanceof ArsenalWeaponItem w && (w.kind()==WeaponKind.FLAIL || w.kind()==WeaponKind.BALL_AND_CHAIN) : s.getItem() instanceof ArsenalShieldItem && ArsenalConfig.SHIELD_ENCHANTMENTS.get());}
   public boolean canApplyAtEnchantingTable(ItemStack s){return !isCurse() && canEnchant(s);}
   protected boolean checkCompatibility(Enchantment other){return super.checkCompatibility(other) && !(!chain && (other==RECOVERY.get() || other==BREECHED.get()));}
  });
 }
 public static int level(LivingEntity owner,ItemStack stack,RegistryObject<Enchantment> enchantment){
  if(!enabled(enchantment.getId().getPath()))return 0;
  int value=EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(),stack);
  return Math.min(value,enchantment==LONG_CHAIN?ArsenalConfig.LONG_CHAIN_MAX_LEVEL.get():enchantment==ROTATION_FORCE?ArsenalConfig.ROTATION_FORCE_MAX_LEVEL.get():value);
 }
 public static boolean enabled(String id){var value=ArsenalConfig.ENCHANTMENTS.get(id);return value!=null && value.get();}
 private ModEnchantments(){}
}
