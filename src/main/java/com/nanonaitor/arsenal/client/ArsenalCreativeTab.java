package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.config.ContentSwitches;
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
        net.minecraft.item.Item[] icons = {
            ModContent.MORNING_STARS.get(WeaponTier.DIAMOND),
            ModContent.SCIMITARS.get(WeaponTier.DIAMOND),
            ModContent.DOUBLE_BLADED_SCIMITARS.get(WeaponTier.DIAMOND),
            ModContent.CLAWS.get(WeaponTier.DIAMOND),
            ModContent.FLAILS.get(WeaponTier.DIAMOND),
            ModContent.BATTERING_RAMS.get(WeaponTier.DIAMOND),
            ModContent.BALLS_AND_CHAINS.get(WeaponTier.DIAMOND),
            ModContent.TARTSY_SHIELD,
            ModContent.SUN_WAR_BULWARK
        };
        for (net.minecraft.item.Item icon : icons) {
            if (icon != null && ContentSwitches.enabled(icon)) return new ItemStack(icon);
        }
        return new ItemStack(Items.DIAMOND_SWORD);
    }

    @Override
    public void displayAllRelevantItems(NonNullList<ItemStack> items) {
        add(items, ModContent.SUN_WAR_BULWARK);
        add(items, ModContent.TARTSY_SHIELD);
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.MORNING_STARS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.SCIMITARS.get(tier));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) add(items, ModContent.DOUBLE_BLADED_SCIMITARS.get(tier));
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
        if (ContentSwitches.enchantmentEnabled(ModContent.LONG_CHAIN)) {
            items.add(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(
                ModContent.LONG_CHAIN, ModContent.LONG_CHAIN.getMaxLevel())));
        }
        if (ContentSwitches.enchantmentEnabled(ModContent.ROTATION_FORCE)) {
            items.add(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(
                ModContent.ROTATION_FORCE, ModContent.ROTATION_FORCE.getMaxLevel())));
        }
        if (ContentSwitches.enchantmentEnabled(ModContent.RECOVERY)) {
            items.add(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(
                ModContent.RECOVERY, ModContent.RECOVERY.getMaxLevel())));
        }
        if (ContentSwitches.enchantmentEnabled(ModContent.BREECHED)) {
            items.add(ItemEnchantedBook.getEnchantedItemStack(new EnchantmentData(
                ModContent.BREECHED, ModContent.BREECHED.getMaxLevel())));
        }
    }

    private static void add(NonNullList<ItemStack> items, net.minecraft.item.Item item) {
        if (item != null && ContentSwitches.enabled(item)) {
            items.add(new ItemStack(item));
        }
    }
}
