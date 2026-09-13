package com.nanonaitor.arsenal.config;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.*;
public record WeaponRecipeCondition(String weapon) implements ICondition {
 public static final ResourceLocation ID=new ResourceLocation("nanonaitors_arsenal","weapon_enabled");
 public ResourceLocation getID(){return ID;}
 public boolean test(IContext context){var value=ArsenalConfig.WEAPONS.get(weapon);return value!=null && value.get();}
 public static final IConditionSerializer<WeaponRecipeCondition> SERIALIZER=new IConditionSerializer<>(){
  public void write(JsonObject json,WeaponRecipeCondition value){json.addProperty("weapon",value.weapon);}
  public WeaponRecipeCondition read(JsonObject json){return new WeaponRecipeCondition(json.get("weapon").getAsString());}
  public ResourceLocation getID(){return ID;}
 };
}
