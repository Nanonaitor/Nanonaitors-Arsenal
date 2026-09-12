package com.nanonaitor.arsenal.config;

import net.minecraft.item.Item;
public final class ContentSwitches {
    private ContentSwitches() {}
    public static boolean enabled(Item item) {
        return item != null && item.getRegistryName() != null && enabled(item.getRegistryName().getResourcePath());
    }
    public static boolean enabled(String id) {
        ArsenalConfig.Weapons w = ArsenalConfig.weapons;
        if (id.startsWith("morning_star")) return w.morningStar;
        if (id.startsWith("double_bladed_scimitar")) return w.bladeStaff;
        if (id.startsWith("scimitar")) return w.scimitar;
        if (id.startsWith("claws") || id.startsWith("linked_claw")) return w.claws;
        if (id.startsWith("flail")) return w.flail;
        if (id.startsWith("battering_ram")) return w.batteringRam;
        if (id.startsWith("ball_and_chain")) return w.ballAndChain;
        if (id.startsWith("tartsy_shield")) return w.tartsyShield;
        if (id.startsWith("sun_war_bulwark")) return w.sunWarBulwark;
        return true;
    }
}
