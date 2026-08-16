package com.nanonaitor.arsenal.compat.jei;

import com.nanonaitor.arsenal.item.ItemLinkedClaw;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientBlacklist;
import net.minecraft.item.ItemStack;

/**
 * Optional JEI integration. Linked Claws are internal synchronized offhand
 * items, so exposing them in the ingredient list is misleading.
 */
@JEIPlugin
public final class ArsenalJeiPlugin implements IModPlugin {
    @Override
    public void register(IModRegistry registry) {
        IIngredientBlacklist blacklist = registry.getJeiHelpers()
            .getIngredientBlacklist();
        for (ItemLinkedClaw linkedClaw : ModContent.LINKED_CLAWS.values()) {
            blacklist.addIngredientToBlacklist(new ItemStack(linkedClaw));
        }
        if (ArsenalCompatManager.hasItem("quark:chain")) {
            // Prefer Quark's chain when it is available.
            // Keep Arsenal's standalone fallback registered for old worlds,
            // but hide the duplicate ingredient when Quark is present.
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.IRON_CHAIN_ITEM));
        }
        for (WeaponTier tier : WeaponTier.values()) {
            if (ArsenalCompatManager.isTierAvailable(tier)) continue;
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.MORNING_STARS.get(tier)));
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.SCIMITARS.get(tier)));
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.CLAWS.get(tier)));
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.FLAILS.get(tier)));
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.BATTERING_RAMS.get(tier)));
            blacklist.addIngredientToBlacklist(new ItemStack(ModContent.BALLS_AND_CHAINS.get(tier)));
        }
    }
}
