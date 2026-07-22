package com.nanonaitor.arsenal;

import com.nanonaitor.arsenal.client.ArsenalCreativeTab;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.creativetab.CreativeTabs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = NanonaitorsArsenal.MOD_ID,
    name = NanonaitorsArsenal.NAME,
    version = NanonaitorsArsenal.VERSION,
    acceptedMinecraftVersions = "[1.12.2]"
)
public final class NanonaitorsArsenal {
    public static final String MOD_ID = "nanonaitors_arsenal";
    public static final String NAME = "Nanonaitor's Arsenal";
    public static final String VERSION = "0.1.0-dev";
    public static final Logger LOGGER = LogManager.getLogger(NAME);
    public static final CreativeTabs CREATIVE_TAB = new ArsenalCreativeTab();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModNetwork.init();
        LOGGER.info("Loading {} {} for Minecraft 1.12.2", NAME, VERSION);
    }
}
