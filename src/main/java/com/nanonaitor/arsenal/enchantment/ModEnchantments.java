package com.nanonaitor.arsenal.enchantment;

import com.nanonaitor.arsenal.ArsenalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class ModEnchantments {
    public static final ResourceKey<Enchantment> LONG_CHAIN = key("long_chain");
    public static final ResourceKey<Enchantment> ROTATION_FORCE = key("rotation_force");

    public static int level(LivingEntity owner, ItemStack stack, ResourceKey<Enchantment> enchantment) {
        var registry = owner.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(registry.getOrThrow(enchantment), stack);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(ArsenalMod.MOD_ID, path));
    }

    private ModEnchantments() {}
}
