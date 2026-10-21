package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Keeps optional parasite tiers aligned with the durability configured by their source mod. */
public final class ParasiteWeaponCompat {
    public static int maxDamage(WeaponTier tier, int fallback) {
        String path = switch (tier) {
            case LIVING -> "living_v2_longsword";
            case SENTIENT -> "sentient_v3_longsword";
            default -> null;
        };
        if (path == null) return fallback;
        Item source = BuiltInRegistries.ITEM.get(new ResourceLocation("srp_spartans", path));
        if (source == null || source == net.minecraft.world.item.Items.AIR) return fallback;
        int configured = new ItemStack(source).getMaxDamage();
        return configured > 0 ? configured : fallback;
    }
    private ParasiteWeaponCompat() {}
}
