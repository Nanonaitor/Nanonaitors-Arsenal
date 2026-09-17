package com.nanonaitor.arsenal.test;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.*;

@GameTestHolder(ArsenalMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EnchantmentGameTests {
    @GameTest(template="empty",templateNamespace="forge")
    public static void ordinaryWeaponEnchantments(GameTestHelper h) {
        for (WeaponTier tier : WeaponTier.values()) for (WeaponKind kind : WeaponKind.values()) {
            ItemStack stack = new ItemStack(ModItems.get(kind,tier).get());
            for (var enchantment : new net.minecraft.world.item.enchantment.Enchantment[]{
                Enchantments.SHARPNESS, Enchantments.UNBREAKING, Enchantments.MENDING,
                Enchantments.FIRE_ASPECT, Enchantments.MOB_LOOTING}) {
                h.assertTrue(stack.getItem().canApplyAtEnchantingTable(stack,enchantment),
                    kind + "/" + tier + " permits " + enchantment);
            }
            h.assertTrue(!stack.getItem().canApplyAtEnchantingTable(stack,Enchantments.ALL_DAMAGE_PROTECTION),
                "Armor enchantments remain ineligible");
        }
        h.succeed();
    }
}
