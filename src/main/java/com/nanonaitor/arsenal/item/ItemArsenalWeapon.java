package com.nanonaitor.arsenal.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.compat.DistinctDamageCompat;
import com.nanonaitor.arsenal.enchantment.EnchantmentLongChain;
import com.nanonaitor.arsenal.enchantment.EnchantmentRotationForce;
import java.util.Collections;
import java.util.Set;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.init.Enchantments;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;

public abstract class ItemArsenalWeapon extends ItemSword {
    private static final ThreadLocal<Set<Enchantment>> CHECKING_ENCHANTMENTS =
        ThreadLocal.withInitial(() -> Collections.newSetFromMap(new java.util.IdentityHashMap<Enchantment, Boolean>()));
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

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.onUpdate(stack, world, entity, slot, selected);
        DistinctDamageCompat.apply(stack, this);
    }

    /**
     * Keep all Arsenal weapon families compatible with vanilla sword enchants
     * and with mods that identify melee weapons through Forge's "sword" class.
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (enchantment.getRegistryName() != null
            && "somanyenchantments:desolator".equals(enchantment.getRegistryName().toString())) {
            if (!(this instanceof ItemMorningStar)) return false;
            try { return (Boolean)enchantment.getClass().getMethod("isEnabled").invoke(enchantment); }
            catch (ReflectiveOperationException exception) { return false; }
        }
        if (com.nanonaitor.arsenal.compat.BladeStaffComboCompat.isCombo(enchantment)) {
            return this instanceof ItemDoubleBladedScimitar && enchantment.getMaxLevel() > 0;
        }
        if (enchantment instanceof EnchantmentLongChain
            || enchantment instanceof EnchantmentRotationForce) {
            return this instanceof ItemFlail || this instanceof ItemBallAndChain;
        }
        if (enchantment == Enchantments.SWEEPING && !(this instanceof ItemScimitar)) {
            return false;
        }
        // Forge's default Enchantment method calls back into Item. On re-entry,
        // use the normal category predicate instead of calling Enchantment again.
        // Mod overrides (including SME's NONE category) keep their own settings.
        Set<Enchantment> checking = CHECKING_ENCHANTMENTS.get();
        if (!checking.add(enchantment)) {
            return enchantment.type != null && enchantment.type.canEnchantItem(this);
        }
        try {
            return enchantment.canApplyAtEnchantingTable(stack);
        } finally {
            checking.remove(enchantment);
            if (checking.isEmpty()) CHECKING_ENCHANTMENTS.remove();
        }
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
