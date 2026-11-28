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
 /**
  * Furnace recycling whose result is a tag ("result": {"tag": "forge:ingots/silver", "count": 1}),
  * as 1.12.2 used the first ore dictionary entry. Vanilla smelting needs one fixed item, but
  * silver, bronze, steel and umbrium come from whichever mod the pack has. The tag is resolved
  * to its first item when the result is needed, because tags are bound after recipes load.
  */
 public static final RegistryObject<RecipeSerializer<SmeltingRecipe>> TAG_SMELTING=SERIALIZERS.register("tag_smelting",()->new RecipeSerializer<>(){
  public SmeltingRecipe fromJson(ResourceLocation id,JsonObject json){
   JsonObject result=net.minecraft.util.GsonHelper.getAsJsonObject(json,"result");
   var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,new ResourceLocation(net.minecraft.util.GsonHelper.getAsString(result,"tag")));
   return new TagSmelting(id,net.minecraft.util.GsonHelper.getAsString(json,"group",""),
    Ingredient.fromJson(json.get("ingredient"),false),tag,net.minecraft.util.GsonHelper.getAsInt(result,"count",1),
    net.minecraft.util.GsonHelper.getAsFloat(json,"experience",0),net.minecraft.util.GsonHelper.getAsInt(json,"cookingtime",200));
  }
  // The server sends the resolved item, so clients need no tag lookup of their own.
  public SmeltingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf){
   return RecipeSerializer.SMELTING_RECIPE.fromNetwork(id,buf);
  }
  public void toNetwork(FriendlyByteBuf buf,SmeltingRecipe recipe){
   buf.writeUtf(recipe.getGroup());buf.writeEnum(recipe.category());recipe.getIngredients().get(0).toNetwork(buf);
   buf.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));buf.writeFloat(recipe.getExperience());buf.writeVarInt(recipe.getCookingTime());
  }
 });
 /** Smelting recipe with a tag result; see {@link #TAG_SMELTING}. */
 public static final class TagSmelting extends SmeltingRecipe {
  private final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag;
  private final int count;
  TagSmelting(ResourceLocation id,String group,Ingredient input,net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag,int count,float xp,int time){
   super(id,group,CookingBookCategory.MISC,input,ItemStack.EMPTY,xp,time);this.tag=tag;this.count=count;
  }
  /** First item of the tag, or empty while tags are unbound or the tag is empty. */
  public ItemStack resolve(){
   var items=net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tag);
   if(items.isEmpty() || items.get().size()==0)return ItemStack.EMPTY;
   return new ItemStack(items.get().get(0).value(),count);
  }
  @Override public ItemStack getResultItem(RegistryAccess access){return resolve();}
  @Override public ItemStack assemble(net.minecraft.world.Container container,RegistryAccess access){return resolve();}
  @Override public RecipeSerializer<?> getSerializer(){return TAG_SMELTING.get();}
 }
 /** Retains commissioned item identity, names, enchantments, qualities and proportional wear. */
 private static final class Upgrade extends ShapelessRecipe {
  Upgrade(ShapelessRecipe base){super(base.getId(),base.getGroup(),base.category(),base.getResultItem(RegistryAccess.EMPTY),base.getIngredients());}
  @Override public RecipeSerializer<?> getSerializer(){return UPGRADE.get();}
  @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess access){
   ItemStack result=super.assemble(grid,access);
   for(int i=0;i<grid.getContainerSize();i++){
    ItemStack old=grid.getItem(i);
    if(!(old.getItem() instanceof ArsenalWeaponItem))continue;
    carryOver(old,result);
    break;
   }
   return result;
  }
 }
 /** Keeps a weapon's name, enchantments, data and proportional wear when it becomes another tier. */
 public static void carryOver(ItemStack old,ItemStack result){
  if(old.hasTag())result.setTag(old.getTag().copy());
  result.getOrCreateTag().remove("ArsenalActive");result.getOrCreateTag().remove("ArsenalBallThrown");
  if(result.getTag().isEmpty())result.setTag(null);
  result.setDamageValue(Math.min(result.getMaxDamage()-1,Math.round((float)old.getDamageValue()/Math.max(1,old.getMaxDamage())*result.getMaxDamage())));
 }
}
