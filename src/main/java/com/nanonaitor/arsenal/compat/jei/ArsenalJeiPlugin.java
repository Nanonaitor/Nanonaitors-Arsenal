package com.nanonaitor.arsenal.compat.jei;

import com.nanonaitor.arsenal.ArsenalMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Optional JEI integration, as in 1.12.2: hides what a player cannot get. JEI only
 * finds this class through its annotation scan, so nothing here loads without JEI.
 */
@JeiPlugin
public final class ArsenalJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID=new ResourceLocation(ArsenalMod.MOD_ID,"jei");

    @Override public ResourceLocation getPluginUid(){return UID;}

    // Runs once the world's tags and the server's synced config are known, so a tier whose
    // material no mod provides and enchantments the server turned off are hidden correctly.
    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime){
        var hidden=JeiHiding.hiddenStacks();
        if(!hidden.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,hidden);
    }
}
