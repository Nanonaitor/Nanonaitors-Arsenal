package com.nanonaitor.arsenal.recipe;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public final class ArsenalUpgradeRecipe extends ShapelessOreRecipe {
    private final ItemArsenalWeapon source, result;
    public ArsenalUpgradeRecipe(ItemArsenalWeapon source, ItemArsenalWeapon result, String catalyst) {
        super(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "compat"), new ItemStack(result),
            new ItemStack(source), ArsenalCompatManager.itemStack(catalyst));
        this.source = source; this.result = result;
    }
    @Override public ItemStack getCraftingResult(InventoryCrafting inv) {
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack original = inv.getStackInSlot(i);
            if (!original.isEmpty() && original.getItem() == source) {
                ItemStack upgraded = new ItemStack(result);
                if (original.hasTagCompound()) upgraded.setTagCompound(original.getTagCompound().copy());
                float used = original.getMaxDamage() <= 0 ? 0F
                    : (float) original.getItemDamage() / original.getMaxDamage();
                upgraded.setItemDamage(Math.min(upgraded.getMaxDamage() - 1,
                    Math.round(used * upgraded.getMaxDamage())));
                return upgraded;
            }
        }
        return ItemStack.EMPTY;
    }
}
