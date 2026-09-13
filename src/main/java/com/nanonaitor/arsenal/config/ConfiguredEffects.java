package com.nanonaitor.arsenal.config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

public final class ConfiguredEffects {
 private static final java.util.Set<String> WARNED=java.util.concurrent.ConcurrentHashMap.newKeySet();
 public static void apply(LivingEntity target,String key,int ticks,int amplifier){
  var option=ArsenalConfig.EFFECTS.get(key);if(option==null)return;
  for(String entry:option.get()){
   try {
    EffectSpec spec=EffectSpec.parse(entry,ticks,amplifier);if(spec==null)continue;
    var effect=ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(spec.id));
    if(effect!=null) target.addEffect(new MobEffectInstance(effect,spec.ticks,spec.amplifier));
   } catch(IllegalArgumentException ex){if(WARNED.add(entry)) com.mojang.logging.LogUtils.getLogger().warn("Ignoring invalid Arsenal effect setting {}: {}",entry,ex.getMessage());}
  }
 }
}
