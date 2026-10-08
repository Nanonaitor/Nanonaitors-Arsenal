package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Copy registered SRP melee durability, including the pack's SRP configuration. */
public final class ParasiteDurabilityCompat {
    private ParasiteDurabilityCompat() {}

    public static void register() {
        copy(WeaponTier.LIVING, "srparasites:weapon_sword");
        copy(WeaponTier.SENTIENT, "srparasites:weapon_sword_sentient");
    }

    private static void copy(WeaponTier tier, String referenceId) {
        Item reference = ForgeRegistries.ITEMS.getValue(new ResourceLocation(referenceId));
        if (reference == null) return;
        int durability = reference.getMaxDamage();
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (item instanceof ItemArsenalWeapon
                && ((ItemArsenalWeapon) item).getTier() == tier) item.setMaxDamage(durability);
        }
        NanonaitorsArsenal.LOGGER.info("Matched Arsenal {} weapon durability to {}: {}",
            tier.getId(), referenceId, durability);
    }
}
