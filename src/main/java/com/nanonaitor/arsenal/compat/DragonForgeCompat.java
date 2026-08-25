package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

/** Optional native Ice and Fire Dragonforge recipes for Dragonbone weapons. */
public final class DragonForgeCompat {
    private DragonForgeCompat() {}

    public static void register() {
        if (!Loader.isModLoaded("iceandfire") || !ArsenalCompatManager.hasDragonForge()) {
            return;
        }
        try {
            Class<?> registryClass = Class.forName(
                "com.github.alexthe666.iceandfire.item.IafDragonForgeRecipeRegistry");
            Class<?> recipeClass = Class.forName(
                "com.github.alexthe666.iceandfire.recipe.DragonForgeRecipe");
            Constructor<?> constructor = recipeClass.getConstructor(ItemStack.class,
                ItemStack.class, ItemStack.class, boolean.class);

            int fire = registerTier(registryClass, recipeClass, constructor,
                "FIRE_FORGE_RECIPES", WeaponTier.FLAMED_DRAGONBONE,
                "iceandfire:fire_dragon_blood");
            int ice = registerTier(registryClass, recipeClass, constructor,
                "ICE_FORGE_RECIPES", WeaponTier.ICED_DRAGONBONE,
                "iceandfire:ice_dragon_blood");
            int lightning = registerTier(registryClass, recipeClass, constructor,
                "LIGHTNING_FORGE_RECIPES", WeaponTier.ELECTRIC_DRAGONBONE,
                "iceandfire:lightning_dragon_blood");
            NanonaitorsArsenal.LOGGER.info(
                "Registered Arsenal Dragonforge recipes: fire={}, ice={}, lightning={}",
                fire, ice, lightning);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            NanonaitorsArsenal.LOGGER.warn(
                "Could not register optional Ice and Fire Dragonforge recipes",
                exception);
        }
    }

    private static int registerTier(Class<?> registryClass, Class<?> recipeClass,
                                    Constructor<?> constructor, String listField,
                                    WeaponTier target, String bloodId)
        throws ReflectiveOperationException {
        ItemStack blood = ArsenalCompatManager.itemStack(bloodId);
        if (blood.isEmpty()) return 0;

        Field field = registryClass.getField(listField);
        @SuppressWarnings("unchecked")
        List<Object> recipes = (List<Object>) field.get(null);
        int added = 0;
        for (Map<WeaponTier, ? extends ItemArsenalWeapon> family : families()) {
            ItemArsenalWeapon input = family.get(WeaponTier.DRAGONBONE);
            ItemArsenalWeapon output = family.get(target);
            if (input == null || output == null
                || hasRecipe(recipes, recipeClass, input, output)) continue;
            // Preserve durability, name, enchantments, qualities and other NBT.
            recipes.add(constructor.newInstance(new ItemStack(input), blood.copy(),
                new ItemStack(output), true));
            added++;
        }
        return added;
    }

    private static boolean hasRecipe(List<Object> recipes, Class<?> recipeClass,
                                     ItemArsenalWeapon input,
                                     ItemArsenalWeapon output)
        throws ReflectiveOperationException {
        Method getInput = recipeClass.getMethod("getInput");
        Method getOutput = recipeClass.getMethod("getOutput");
        for (Object recipe : recipes) {
            ItemStack existingInput = (ItemStack) getInput.invoke(recipe);
            ItemStack existingOutput = (ItemStack) getOutput.invoke(recipe);
            if (existingInput.getItem() == input && existingOutput.getItem() == output) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<WeaponTier, ? extends ItemArsenalWeapon>> families() {
        return Arrays.asList(ModContent.MORNING_STARS, ModContent.SCIMITARS,
            ModContent.CLAWS, ModContent.FLAILS, ModContent.BATTERING_RAMS,
            ModContent.BALLS_AND_CHAINS);
    }
}
