package com.nanonaitor.arsenal;

import com.nanonaitor.arsenal.client.ArsenalCreativeTab;
import com.nanonaitor.arsenal.compat.SilverSetBonusCompat;
import com.nanonaitor.arsenal.compat.DragonForgeCompat;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.recipe.ModSmeltingRecipes;
import net.minecraft.creativetab.CreativeTabs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;

@Mod(
    modid = NanonaitorsArsenal.MOD_ID,
    name = NanonaitorsArsenal.NAME,
    version = NanonaitorsArsenal.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    guiFactory = "com.nanonaitor.arsenal.client.ArsenalGuiFactory",
    dependencies = "after:setbonus;after:iceandfire;after:quark;after:spartanweaponry;after:spartanfire;after:srparasites;after:mobends"
)
public final class NanonaitorsArsenal {
    public static final String MOD_ID = "nanonaitors_arsenal";
    public static final String NAME = "Nanonaitor's Arsenal";
    public static final String VERSION = "2.0.22";
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static final CreativeTabs CREATIVE_TAB = new ArsenalCreativeTab();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModNetwork.init();
        com.nanonaitor.arsenal.data.TransientData.register();
        LOGGER.info("Loading {} {} for Minecraft 1.12.2", NAME, VERSION);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        // Both read the ore dictionary, which some mods (HBM's steel) only
        // fill during their own init. By post-init every mod's init has run.
        com.nanonaitor.arsenal.recipe.ModRecipes.registerLateOreRecipes();
        ModSmeltingRecipes.register();
        com.nanonaitor.arsenal.compat.ParasiteDurabilityCompat.register();
        DragonForgeCompat.register();
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        SilverSetBonusCompat.registerRlcraftEquipmentSets();
    }
}
