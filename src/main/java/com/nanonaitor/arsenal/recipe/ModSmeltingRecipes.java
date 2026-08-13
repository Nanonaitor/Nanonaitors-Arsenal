package com.nanonaitor.arsenal.recipe;

import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

/** Furnace recycling modeled after RLCraft Dregora's roughly one-third returns. */
public final class ModSmeltingRecipes {
    private static final float EXPERIENCE = 0.1F;

    private ModSmeltingRecipes() {}

    public static void register() {
        registerTier(WeaponTier.IRON, new ItemStack(Items.IRON_INGOT),
            new ItemStack(Items.IRON_NUGGET));
        registerTier(WeaponTier.GOLD, new ItemStack(Items.GOLD_INGOT),
            new ItemStack(Items.GOLD_NUGGET));
        registerTier(WeaponTier.DIAMOND, new ItemStack(Items.DIAMOND),
            firstAvailable("nuggetDiamond", "variedcommodities:coin_diamond"));
        registerTier(WeaponTier.SILVER, firstOre("ingotSilver"),
            firstAvailable("nuggetSilver", "iceandfire:silver_nugget"));
        registerTier(WeaponTier.BRONZE, firstOre("ingotBronze"),
            firstAvailable("nuggetBronze", "variedcommodities:coin_bronze"));
        registerTier(WeaponTier.STEEL, firstOre("ingotSteel"),
            firstAvailable("nuggetSteel", "contenttweaker:steel_nugget"));
        registerTier(WeaponTier.UMBRIUM, ArsenalCompatManager.itemStack("defiledlands:umbrium_ingot"),
            firstAvailable("nuggetUmbrium", "defiledlands:umbrium_nugget"));

        // Four gold blocks are the only recoverable component counted from the Bulwark.
        add(ModContent.SUN_WAR_BULWARK, counted(new ItemStack(Items.GOLD_INGOT), 12));
    }

    private static void registerTier(WeaponTier tier, ItemStack whole, ItemStack nugget) {
        if (!ArsenalCompatManager.isTierAvailable(tier) || whole.isEmpty()) return;

        // floor(primary material / 3), matching the upper end of the requested 25-33% band.
        add(ModContent.MORNING_STARS.get(tier), counted(whole, 2)); // 7 primary materials
        add(ModContent.SCIMITARS.get(tier), counted(whole, 1));    // 4 primary materials
        add(ModContent.CLAWS.get(tier), counted(whole, 1));        // 4 primary materials
        add(ModContent.FLAILS.get(tier), counted(nugget, 3));      // 1 primary material

        boolean usesBlock = usesMaterialBlock(tier);
        ItemStack heavyReturn = usesBlock ? counted(whole, 3) : counted(nugget, 3);
        add(ModContent.BATTERING_RAMS.get(tier), heavyReturn);
        add(ModContent.BALLS_AND_CHAINS.get(tier), heavyReturn);
    }

    private static boolean usesMaterialBlock(WeaponTier tier) {
        switch (tier) {
            case IRON:
            case GOLD:
            case DIAMOND:
                return true;
            default:
                String id = tier.getId();
                String ore = "block" + Character.toUpperCase(id.charAt(0)) + id.substring(1);
                return ArsenalCompatManager.hasOre(ore);
        }
    }

    private static void add(Item input, ItemStack output) {
        if (input == null || output.isEmpty() || output.getCount() <= 0) return;
        GameRegistry.addSmelting(
            new ItemStack(input, 1, OreDictionary.WILDCARD_VALUE), output.copy(), EXPERIENCE);
    }

    private static ItemStack counted(ItemStack stack, int count) {
        if (stack == null || stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack result = stack.copy();
        result.setCount(count);
        return result;
    }

    private static ItemStack firstAvailable(String oreName, String fallbackId) {
        ItemStack ore = firstOre(oreName);
        return ore.isEmpty() ? ArsenalCompatManager.itemStack(fallbackId) : ore;
    }

    private static ItemStack firstOre(String oreName) {
        List<ItemStack> ores = OreDictionary.getOres(oreName, false);
        if (ores.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = ores.get(0).copy();
        result.setCount(1);
        return result;
    }
}
