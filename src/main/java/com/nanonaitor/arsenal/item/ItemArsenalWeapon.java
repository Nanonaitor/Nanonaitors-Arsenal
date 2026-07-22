package com.nanonaitor.arsenal.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import java.util.Collections;
import java.util.Set;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;

public abstract class ItemArsenalWeapon extends ItemSword {
    private final WeaponTier tier;
    private final double attackDamageModifier;
    private final double attackSpeedModifier;

    protected ItemArsenalWeapon(WeaponTier tier, String family,
                                double attackDamageModifier, double attackSpeedModifier) {
        super(tier.getMaterial());
        this.tier = tier;
        this.attackDamageModifier = attackDamageModifier;
        this.attackSpeedModifier = attackSpeedModifier;
        setRegistryName(NanonaitorsArsenal.MOD_ID, family + "_" + tier.getId());
        setUnlocalizedName(NanonaitorsArsenal.MOD_ID + "." + family + "_" + tier.getId());
        setCreativeTab(NanonaitorsArsenal.CREATIVE_TAB);
    }

    public final WeaponTier getTier() {
        return tier;
    }

    public final double getDisplayedAttackDamage() {
        return 1.0D + attackDamageModifier;
    }

    public final double getDisplayedAttackSpeed() {
        return 4.0D + attackSpeedModifier;
    }

    /**
     * Keep all Arsenal weapon families compatible with vanilla sword enchants
     * and with mods that identify melee weapons through Forge's "sword" class.
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.type == EnumEnchantmentType.WEAPON
            || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public Set<String> getToolClasses(ItemStack stack) {
        return Collections.singleton("sword");
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return ArsenalCompatManager.matchesIngredient(tier.getRepairIngredient(), repair)
            || super.getIsRepairable(toRepair, repair);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", attackDamageModifier, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", attackSpeedModifier, 0));
        }
        return modifiers;
    }
}
