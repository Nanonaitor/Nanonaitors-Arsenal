package com.nanonaitor.arsenal.registry;
import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.enchantment.ModEnchantments;
import com.nanonaitor.arsenal.item.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.registries.*;
public final class ModTabs {
 public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ArsenalMod.MOD_ID);
 public static final RegistryObject<CreativeModeTab> MAIN=TABS.register("arsenal",()->CreativeModeTab.builder()
  .withTabsBefore(CreativeModeTabs.COMBAT).title(Component.translatable("itemGroup.nanonaitors_arsenal"))
  .icon(()->ModItems.get(WeaponKind.MORNING_STAR,WeaponTier.DIAMOND).get().getDefaultInstance())
  .displayItems((parameters,output)->{
   ModItems.VISIBLE.values().forEach(item->{if(!(item.get() instanceof ArsenalWeaponItem weapon) || weapon.tier().available()) output.accept(item.get());});
   ModEnchantments.ENCHANTMENTS.getEntries().forEach(e->{if(ModEnchantments.enabled(e.getId().getPath())) output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(e.get(),e.get().getMaxLevel())));});
  }).build());
 private ModTabs(){}
}
