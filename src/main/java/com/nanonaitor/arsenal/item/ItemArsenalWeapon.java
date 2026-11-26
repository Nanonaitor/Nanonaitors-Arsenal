package com.nanonaitor.arsenal.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.enchantment.EnchantmentLongChain;
import com.nanonaitor.arsenal.enchantment.EnchantmentRotationForce;
import java.util.Collections;
import java.util.Set;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ResourceLocation;

public abstract class ItemArsenalWeapon extends ItemSword {
    private static final ThreadLocal<Set<Enchantment>> CHECKING_ENCHANTMENTS =
        ThreadLocal.withInitial(() -> Collections.newSetFromMap(new java.util.IdentityHashMap<Enchantment, Boolean>()));
    private final WeaponTier tier;
    private final double attackDamageModifier;
    private final double attackSpeedModifier;
    private final float durabilityScale;
    /** Battering Rams and Balls and Chains cost a block of material; they last 50% longer. */
    protected static final float HEAVY_DURABILITY_SCALE = 1.5F;

    protected ItemArsenalWeapon(WeaponTier tier, String family,
                                double attackDamageModifier, double attackSpeedModifier) {
        this(tier, family, attackDamageModifier, attackSpeedModifier, 1.0F);
    }

    /**
     * @param durabilityScale multiplier on the tier's durability, for families
     *                        whose recipes cost a full block of material
     */
    protected ItemArsenalWeapon(WeaponTier tier, String family,
                                double attackDamageModifier, double attackSpeedModifier,
                                float durabilityScale) {
        super(tier.getMaterial());
        this.tier = tier;
        this.attackDamageModifier = attackDamageModifier;
        this.attackSpeedModifier = attackSpeedModifier;
        this.durabilityScale = durabilityScale;
        setBaseDurability(tier.getMaterial().getMaxUses());
        setRegistryName(NanonaitorsArsenal.MOD_ID, family + "_" + tier.getId());
        setUnlocalizedName(NanonaitorsArsenal.MOD_ID + "." + family + "_" + tier.getId());
        setCreativeTab(NanonaitorsArsenal.CREATIVE_TAB);
    }

    /** Custom materials have no lang entry: take the iron name and swap the material. */
    @Override
    @SuppressWarnings("deprecation")
    public String getItemStackDisplayName(ItemStack stack) {
        if (!tier.isCustom()) return super.getItemStackDisplayName(stack);
        String key = getUnlocalizedName().substring(0, getUnlocalizedName().length() - tier.getId().length()) + "iron.name";
        String iron = net.minecraft.util.text.translation.I18n.translateToLocal(key);
        return iron.equals(key) ? tier.getDisplayName() + " " + getUnlocalizedName()
            : iron.replace("Iron", tier.getDisplayName());
    }

    public final WeaponTier getTier() {
        return tier;
    }

    /** Sets durability from a tier-level value, keeping this family's scale. */
    public final void setBaseDurability(int uses) {
        setMaxDamage(Math.max(1, Math.round(uses * durabilityScale)));
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
        if (isSwifterSlashes(enchantment)
            && (this instanceof ItemFlail || this instanceof ItemBallAndChain
                || this instanceof ItemClaws)) {
            return false;
        }
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
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if ((this instanceof ItemFlail || this instanceof ItemBallAndChain
                || this instanceof ItemClaws)
            && net.minecraft.enchantment.EnchantmentHelper.getEnchantments(book).keySet().stream()
                .anyMatch(ItemArsenalWeapon::isSwifterSlashes)) {
            return false;
        }
        return super.isBookEnchantable(stack, book);
    }

    private static boolean isSwifterSlashes(Enchantment enchantment) {
        ResourceLocation id = enchantment.getRegistryName();
        return id != null && "somanyenchantments".equals(id.getResourceDomain())
            && ("swifter_slashes".equals(id.getResourcePath()) || "swifterslashes".equals(id.getResourcePath()));
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
