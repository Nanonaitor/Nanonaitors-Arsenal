package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.registry.ModContent;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public final class ArsenalCreativeTab extends CreativeTabs {
    public ArsenalCreativeTab() {
        super("nanonaitors_arsenal");
    }

    @Override
    public ItemStack getTabIconItem() {
        if (ModContent.SCIMITARS.get(WeaponTier.DIAMOND) != null) {
            return new ItemStack(ModContent.SCIMITARS.get(WeaponTier.DIAMOND));
        }
        return new ItemStack(Items.DIAMOND_SWORD);
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.MORNING_STARS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.SCIMITARS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.CLAWS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.FLAILS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.BATTERING_RAMS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.BALLS_AND_CHAINS.get(tier));
        }
    }

    private static void add(NonNullList<ItemStack> items, net.minecraft.item.Item item) {
        if (item != null) {
            items.add(new ItemStack(item));
        }
    }
}
