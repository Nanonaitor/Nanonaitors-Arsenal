package com.nanonaitor.arsenal.registry;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.*;

public final class ModRecipes {
 public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,ArsenalMod.MOD_ID);
 public static final RegistryObject<RecipeSerializer<ShapelessRecipe>> UPGRADE=SERIALIZERS.register("upgrade",()->new RecipeSerializer<>(){
  private final ShapelessRecipe.Serializer delegate=new ShapelessRecipe.Serializer();
  public ShapelessRecipe fromJson(ResourceLocation id,JsonObject json){return new Upgrade(delegate.fromJson(id,json));}
  public ShapelessRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf){return new Upgrade(delegate.fromNetwork(id,buf));}
  public void toNetwork(FriendlyByteBuf buf,ShapelessRecipe recipe){delegate.toNetwork(buf,recipe);}
 });
 /** Retains commissioned item identity, names, enchantments, qualities and proportional wear. */
 private static final class Upgrade extends ShapelessRecipe {
  Upgrade(ShapelessRecipe base){super(base.getId(),base.getGroup(),base.category(),base.getResultItem(RegistryAccess.EMPTY),base.getIngredients());}
  @Override public RecipeSerializer<?> getSerializer(){return UPGRADE.get();}
  @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){
   ItemStack result=super.assemble(grid,access);
   for(int i=0;i<grid.getContainerSize();i++){
    ItemStack old=grid.getItem(i);
    if(!(old.getItem() instanceof ArsenalWeaponItem))continue;
    if(old.hasTag())result.setTag(old.getTag().copy());
    result.getOrCreateTag().remove("ArsenalActive");result.getOrCreateTag().remove("ArsenalBallThrown");
    result.setDamageValue(Math.min(result.getMaxDamage()-1,Math.round((float)old.getDamageValue()/Math.max(1,old.getMaxDamage())*result.getMaxDamage())));
    break;
   }
   return result;
  }
 }
}
