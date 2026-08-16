package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.registry.ModContent;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.util.NonNullList;

public final class ArsenalCreativeTab extends CreativeTabs {
    public ArsenalCreativeTab() {
        super("nanonaitors_arsenal");
    }

    @Override
    public ItemStack getTabIconItem() {
        if (ModContent.MORNING_STARS.get(WeaponTier.DIAMOND) != null) {
            return new ItemStack(ModContent.MORNING_STARS.get(WeaponTier.DIAMOND));
        }
        return new ItemStack(Items.DIAMOND_SWORD);
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        add(items, ModContent.SUN_WAR_BULWARK);
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
        if (ModContent.LONG_CHAIN != null) {
            items.add(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(
                ModContent.LONG_CHAIN, ModContent.LONG_CHAIN.getMaxLevel())));
        }
        if (ModContent.ROTATION_FORCE != null) {
            items.add(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(
                ModContent.ROTATION_FORCE, ModContent.ROTATION_FORCE.getMaxLevel())));
        }
    }

    private static void add(NonNullList<ItemStack> items, net.minecraft.item.Item item) {
        if (item != null) {
            items.add(new ItemStack(item));
        }
    }
}
