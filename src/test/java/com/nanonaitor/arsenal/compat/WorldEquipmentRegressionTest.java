package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.*;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.event.LootTableLoadEvent;

public final class WorldEquipmentRegressionTest {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("World equipment check "+checks);}
    public static void main(String[] args){
        Bootstrap.register();
        check(WorldEquipmentCompat.tierFor("minecraft","iron_sword")==WeaponTier.IRON);
        check(WorldEquipmentCompat.tierFor("minecraft","golden_sword")==WeaponTier.GOLD);
        check(WorldEquipmentCompat.tierFor("minecraft","wooden_axe")==WeaponTier.WOOD);
        check(WorldEquipmentCompat.tierFor("spartanweaponry","rapier_diamond")==WeaponTier.DIAMOND);
        check(WorldEquipmentCompat.tierFor("spartanfire","saber_lightning_dragonbone")==WeaponTier.ELECTRIC_DRAGONBONE);
        check(WorldEquipmentCompat.tierFor("spartanweaponry","crossbow_iron")==null);
        check(WorldEquipmentCompat.tierFor("spartanweaponry","throwing_axe_iron")==null);
        check(WorldEquipmentCompat.tierFor("spartanweaponry","saber_lead")==null);
        check(WorldEquipmentCompat.tierFor("other","saber_iron")==null);
        check(WorldEquipmentCompat.tierFor("nanonaitors_arsenal","scimitar_iron")==null);
        ItemScimitar scimitar=new ItemScimitar(WeaponTier.IRON);
        ModContent.SCIMITARS.put(WeaponTier.IRON,scimitar);
        LootFunction function=new LootFunction(new LootCondition[0]){
            @Override public ItemStack apply(ItemStack stack,Random rng,LootContext context){stack.setStackDisplayName("Retained function");return stack;}
        };
        LootEntryItem original=new LootEntryItem(Items.IRON_SWORD,7,3,new LootFunction[]{function},new LootCondition[0],"weapon");
        LootEntryItem bow=new LootEntryItem(Items.BOW,2,0,new LootFunction[0],new LootCondition[0],"bow");
        LootPool pool=new LootPool(new LootEntry[]{original,bow},new LootCondition[0],new RandomValueRange(1),new RandomValueRange(0),"main");
        LootTable table=new LootTable(new LootPool[]{pool});
        WorldEquipmentCompat.loot(new LootTableLoadEvent(new ResourceLocation("test:chest"),table,null));
        LootEntry replaced=pool.getEntry("weapon");check(replaced!=original);check(pool.getEntry("bow")==bow);
        for(float luck:new float[]{0,1,3,-1})check(replaced.getEffectiveWeight(luck)==original.getEffectiveWeight(luck));
        LootContext context=new LootContext(0,null,null,null,null,null);
        List<ItemStack> output=new ArrayList<>();
        ArsenalConfig.worldEquipment.lootReplacementChance=1;
        replaced.addLoot(output,new Random(1),context);check(output.size()==1);check(output.get(0).getItem()==scimitar);check(output.get(0).getDisplayName().equals("Retained function"));
        output.clear();ArsenalConfig.worldEquipment.lootReplacementChance=0;
        replaced.addLoot(output,new Random(1),context);check(output.get(0).getItem()==Items.IRON_SWORD);
        WorldEquipmentCompat.loot(new LootTableLoadEvent(new ResourceLocation("test:chest"),table,null));check(pool.getEntry("weapon")==replaced);
        System.out.println("World equipment regression checks: "+checks);
    }
}
