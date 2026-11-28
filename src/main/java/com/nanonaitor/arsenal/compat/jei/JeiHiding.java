package com.nanonaitor.arsenal.compat.jei;

import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.enchantment.ModEnchantments;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * Which Arsenal stacks JEI should hide. Kept apart from the plugin class so it needs no
 * JEI classes and GameTests can check it without JEI installed.
 */
public final class JeiHiding {
    /** Weapon family config ids: the Linked Claws share the Claws switch. */
    private static String switchId(WeaponKind kind){return kind==WeaponKind.LINKED_CLAWS?WeaponKind.CLAWS.id:kind.id;}

    public static List<ItemStack> hiddenStacks(){
        List<ItemStack> hidden=new ArrayList<>();
        for(var entry:ModItems.ITEMS.getEntries()){
            var item=entry.get();
            if(item instanceof ArsenalWeaponItem weapon){
                var family=ArsenalConfig.WEAPONS.get(switchId(weapon.kind()));
                // Linked Claws are the internal off-hand half of a claw pair, never an item to look up.
                if(weapon.kind()==WeaponKind.LINKED_CLAWS || (family!=null && !family.get()) || !weapon.tier().available())
                    hidden.add(new ItemStack(item));
                continue;
            }
            // Shields and other items with their own switch (sun_war_bulwark, tartsy_shield).
            var own=ArsenalConfig.WEAPONS.get(BuiltInRegistries.ITEM.getKey(item).getPath());
            if(own!=null && !own.get())hidden.add(new ItemStack(item));
        }
        for(var entry:ModEnchantments.ENCHANTMENTS.getEntries()){
            if(ModEnchantments.enabled(entry.getId().getPath()))continue;
            var enchantment=entry.get();
            for(int level=enchantment.getMinLevel();level<=enchantment.getMaxLevel();level++)
                hidden.add(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment,level)));
        }
        return hidden;
    }
    private JeiHiding(){}
}
