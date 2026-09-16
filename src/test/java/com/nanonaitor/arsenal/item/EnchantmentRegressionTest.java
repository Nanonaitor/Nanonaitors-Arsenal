package com.nanonaitor.arsenal.item;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

/** Exercises Forge's real Item -> Enchantment -> Item eligibility path. */
public final class EnchantmentRegressionTest {
    private static int checks;
    private static void check(boolean result) {
        checks++;
        if (!result) throw new AssertionError("Enchantment check " + checks);
    }
    private static Enchantment category(EnumEnchantmentType type) {
        return new Enchantment(Enchantment.Rarity.COMMON, type,
            new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}) {};
    }
    public static void main(String[] args) {
        net.minecraft.init.Bootstrap.register();
        for (WeaponTier tier : WeaponTier.values()) for (ItemArsenalWeapon weapon : new ItemArsenalWeapon[]{
            new ItemMorningStar(tier), new ItemScimitar(tier), new ItemDoubleBladedScimitar(tier),
            new ItemClaws(tier), new ItemLinkedClaw(tier), new ItemFlail(tier),
            new ItemBatteringRam(tier), new ItemBallAndChain(tier)}) {
            ItemStack stack = new ItemStack(weapon);
            check(weapon.getItemEnchantability() > 0);
            check(weapon.canApplyAtEnchantingTable(stack, category(EnumEnchantmentType.WEAPON)));
            check(weapon.canApplyAtEnchantingTable(stack, category(EnumEnchantmentType.BREAKABLE)));
            check(!weapon.canApplyAtEnchantingTable(stack, category(EnumEnchantmentType.ARMOR)));
            Enchantment custom = new Enchantment(Enchantment.Rarity.COMMON, EnumEnchantmentType.ARMOR,
                new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}) {
                @Override public boolean canApplyAtEnchantingTable(ItemStack s) { return s.getItem() instanceof ItemArsenalWeapon; }
            };
            check(weapon.canApplyAtEnchantingTable(stack, custom));
            Enchantment disabled = new Enchantment(Enchantment.Rarity.COMMON, EnumEnchantmentType.WEAPON,
                new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND}) {
                @Override public boolean canApplyAtEnchantingTable(ItemStack s) { return false; }
            };
            check(!weapon.canApplyAtEnchantingTable(stack, disabled));
            check(weapon.canApplyAtEnchantingTable(stack, net.minecraft.init.Enchantments.SHARPNESS));
            check(weapon.canApplyAtEnchantingTable(stack, net.minecraft.init.Enchantments.UNBREAKING));
            check(weapon.canApplyAtEnchantingTable(stack, net.minecraft.init.Enchantments.MENDING));
            check(weapon.canApplyAtEnchantingTable(stack, net.minecraft.init.Enchantments.SWEEPING)
                == (weapon instanceof ItemScimitar));
            Enchantment combo = category(EnumEnchantmentType.WEAPON)
                .setRegistryName("mujmajnkraftsbettersurvival", "combo");
            check(weapon.canApplyAtEnchantingTable(stack, combo) == (weapon instanceof ItemDoubleBladedScimitar));
            check(weapon.canApplyAtEnchantingTable(stack, new Desolator()) == (weapon instanceof ItemMorningStar));
        }
        System.out.println("Passed " + checks + " enchantment eligibility checks");
    }
    public static final class Desolator extends Enchantment {
        public Desolator() {
            super(Rarity.COMMON, EnumEnchantmentType.WEAPON, new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND});
            setRegistryName("somanyenchantments", "desolator");
        }
        public boolean isEnabled() { return true; }
    }
}
