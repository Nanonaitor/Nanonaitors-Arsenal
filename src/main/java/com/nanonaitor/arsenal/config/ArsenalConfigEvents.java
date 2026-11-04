package com.nanonaitor.arsenal.config;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ArsenalConfigEvents {
    private ArsenalConfigEvents() {}

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!NanonaitorsArsenal.MOD_ID.equals(event.getModID())) return;
        ConfigManager.sync(NanonaitorsArsenal.MOD_ID, Config.Type.INSTANCE);
    }
}
