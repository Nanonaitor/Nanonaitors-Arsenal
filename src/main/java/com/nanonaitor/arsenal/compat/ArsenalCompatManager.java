package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.item.WeaponTier;
import java.lang.reflect.Field;
import java.util.Locale;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

public final class ArsenalCompatManager {
    private ArsenalCompatManager() {}

    public static boolean isTierAvailable(WeaponTier tier) {
        switch (tier.getFamily()) {
            case VANILLA: return true;
            case SPARTAN: return hasOre(tier.getRepairIngredient().substring(4));
            case DEFILED: return hasItem("defiledlands:umbrium_ingot");
            case ICE_AND_FIRE: return hasItem("iceandfire:dragonbone");
            case FIRE_DRAGONBONE: return hasItem("iceandfire:fire_dragon_blood");
            case ICE_DRAGONBONE: return hasItem("iceandfire:ice_dragon_blood");
            case LIGHTNING_DRAGONBONE: return hasItem("iceandfire:lightning_dragon_blood");
            case DESERT_MYRMEX: return hasItem("iceandfire:myrmex_desert_chitin");
            case JUNGLE_MYRMEX: return hasItem("iceandfire:myrmex_jungle_chitin");
            case SRP: return Loader.isModLoaded("srparasites") && hasItem("srparasites:living_core");
            default: return false;
        }
    }

    public static boolean hasItem(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item != null && item != Items.AIR;
    }
    public static ItemStack itemStack(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
    public static boolean hasOre(String name) {
        return OreDictionary.doesOreNameExist(name) && !OreDictionary.getOres(name, false).isEmpty();
    }
    public static boolean matchesIngredient(String descriptor, ItemStack candidate) {
        if (candidate.isEmpty()) return false;
        if (descriptor.startsWith("ore:")) {
            for (ItemStack ore : OreDictionary.getOres(descriptor.substring(4), false))
                if (OreDictionary.itemMatches(ore, candidate, false)) return true;
            return false;
        }
        ItemStack expected = itemStack(descriptor);
        return !expected.isEmpty() && OreDictionary.itemMatches(expected, candidate, false);
    }
    public static boolean hasDragonForge() {
        if (!Loader.isModLoaded("iceandfire")) return false;
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys())
            if ("iceandfire".equals(id.getResourceDomain())
                && id.getResourcePath().replace("_", "").toLowerCase(Locale.ROOT).contains("dragonforge")) return true;
        return false;
    }
    public static int getSrpEvolutionThreshold() {
        if (Loader.isModLoaded("srparasites")) try {
            Class<?> config = Class.forName("com.dhanantry.scapeandrunparasites.util.config.SRPConfig");
            Field field = config.getField("weapon_livingSentient_HP_needed");
            return Math.max(1, field.getInt(null));
        } catch (ReflectiveOperationException | LinkageError ignored) {}
        return 1000;
    }
    public static boolean isSrpParasite(EntityLivingBase entity) {
        if (entity == null || !Loader.isModLoaded("srparasites")) return false;
        try {
            return Class.forName("com.dhanantry.scapeandrunparasites.entity.ai.misc.EntityParasiteBase").isInstance(entity);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            ResourceLocation id = EntityList.getKey(entity);
            return id != null && "srparasites".equals(id.getResourceDomain());
        }
    }

    public static Potion potion(String path) {
        return ForgeRegistries.POTIONS.getValue(new ResourceLocation("srparasites", path));
    }

    /** Mirrors SRP's own optional scent toggle without linking against SRP. */
    public static boolean isSrpScentEnabled() {
        if (!Loader.isModLoaded("srparasites")) return false;
        try {
            Class<?> config = Class.forName(
                "com.dhanantry.scapeandrunparasites.util.config.SRPConfigSystems");
            return config.getField("useScent").getBoolean(null);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }
}
