package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Better Survival's enchantment overrides Item's eligibility; bridge books explicitly. */
@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID)
public final class BladeStaffComboCompat {
    private BladeStaffComboCompat() {}
    public static boolean isCombo(Enchantment enchantment) {
        return enchantment.getRegistryName()!=null
            && "mujmajnkraftsbettersurvival:combo".equals(enchantment.getRegistryName().toString());
    }
    @SubscribeEvent
    public static void anvil(AnvilUpdateEvent event) {
        ItemStack left=event.getLeft(), right=event.getRight();
        if (!(left.getItem() instanceof ItemDoubleBladedScimitar) || right.getItem()!=Items.ENCHANTED_BOOK
            || !event.getOutput().isEmpty()) return;
        Map<Enchantment,Integer> additions=EnchantmentHelper.getEnchantments(right);
        if (additions.keySet().stream().noneMatch(e -> isCombo(e) && e.getMaxLevel()>0)) return;
        ItemStack output=left.copy();
        Map<Enchantment,Integer> result=EnchantmentHelper.getEnchantments(output);
        int cost=left.getRepairCost()+right.getRepairCost(), applied=0;
        for (Map.Entry<Enchantment,Integer> entry:additions.entrySet()) {
            Enchantment enchantment=entry.getKey();
            if (enchantment.getMaxLevel()<=0 || !(isCombo(enchantment) || enchantment.canApply(left))) continue;
            boolean compatible=true;
            for(Enchantment existing:result.keySet())
                if(existing!=enchantment && !enchantment.isCompatibleWith(existing)) compatible=false;
            if(!compatible)continue;
            int old=result.getOrDefault(enchantment,0);
            int level=Math.min(enchantment.getMaxLevel(),old==entry.getValue()?old+1:Math.max(old,entry.getValue()));
            result.put(enchantment,level);
            int rarity=enchantment.getRarity()==Enchantment.Rarity.VERY_RARE?4:
                enchantment.getRarity()==Enchantment.Rarity.RARE?2:1;
            cost+=rarity*level;
            applied++;
        }
        if(applied==0)return;
        String name=event.getName();
        if(name==null || name.trim().isEmpty()) {
            if(output.hasDisplayName()){output.clearCustomName();cost++;}
        } else if(!name.equals(output.getDisplayName())) {output.setStackDisplayName(name);cost++;}
        EnchantmentHelper.setEnchantments(result,output);
        output.setRepairCost(Math.max(left.getRepairCost(),right.getRepairCost())*2+1);
        event.setOutput(output);
        event.setMaterialCost(1);
        event.setCost(Math.max(1,cost));
    }
}
