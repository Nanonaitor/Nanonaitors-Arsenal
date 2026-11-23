package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.client.ArsenalTooltip;
import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import com.nanonaitor.arsenal.compat.ReskillableCompat;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.config.ConfiguredEffects;
import java.util.List;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

/** Blade Staff. Legacy class and registry IDs are retained for save compatibility. */
public final class ItemDoubleBladedScimitar extends ItemArsenalWeapon {
    public static final ResourceLocation REFLECTING = new ResourceLocation(
        "nanonaitors_arsenal", "reflecting");

    public ItemDoubleBladedScimitar(WeaponTier tier) {
        // Vanilla swords deal material damage + 4. This family deals one more.
        super(tier, "blade_staff",
            4.0D + tier.getMaterial().getAttackDamage(), -2.0D);
        addPropertyOverride(REFLECTING, new IItemPropertyGetter() {
            @Override
            public float apply(ItemStack stack, World world, EntityLivingBase entity) {
                return entity instanceof EntityPlayer
                    && DoubleBladedScimitarCombat.isReflecting((EntityPlayer) entity)
                    ? 1.0F : 0.0F;
            }
        });
    }

    @Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.NONE; }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        if (com.nanonaitor.arsenal.compat.BladeStaffComboCompat.isCombo(enchantment)) return enchantment.getMaxLevel()>0;
        return !isUnsupportedSweep(enchantment)
            && super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, net.minecraft.entity.Entity entity,
                         int slot, boolean selected) {
        super.onUpdate(stack, world, entity, slot, selected);
        if (!world.isRemote) removeUnsupportedSweeps(stack);
    }

    private static boolean isUnsupportedSweep(Enchantment enchantment) {
        if (enchantment == Enchantments.SWEEPING) return true;
        ResourceLocation id = enchantment == null ? null : enchantment.getRegistryName();
        return id != null
            && "somanyenchantments".equals(id.getResourceDomain())
            && "arcslash".equals(id.getResourcePath());
    }

    private static void removeUnsupportedSweeps(ItemStack stack) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (!enchantments.keySet().removeIf(ItemDoubleBladedScimitar::isUnsupportedSweep)) return;
        EnchantmentHelper.setEnchantments(enchantments, stack);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
                                                     EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (hand != EnumHand.MAIN_HAND || !player.getHeldItemOffhand().isEmpty()
            || player.getCooldownTracker().hasCooldown(this)
            || !ReskillableCompat.canUse(player, held)) {
            return new ActionResult<>(EnumActionResult.PASS, held);
        }
        if (!DoubleBladedScimitarCombat.beginReflection(player, held)) {
            return new ActionResult<>(EnumActionResult.PASS, held);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Sweeps nearby enemies; right-click to reflect damage.")) return;
        tooltip.add(TextFormatting.GRAY + reflectionDescription());
        String effects = ConfiguredEffects.displayNames(ArsenalConfig.bladeStaff.reflectEffects);
        if (!effects.isEmpty()) tooltip.add(TextFormatting.GRAY + "Reflected projectiles apply: " + effects + ".");
        tooltip.add(TextFormatting.RED + "Two-Handed");
    }

    private String reflectionDescription() {
        if (getTier() == WeaponTier.WOOD)
            return "Reflects a flat 1 damage per incoming hit.";
        if (getTier() == WeaponTier.SILVER)
            return "Reflects 50% damage, or 200% against undead enemies.";
        if (getTier() == WeaponTier.GOLD)
            return "Reflects 50% damage, or 100% with matching gold armor.";
        return "Reflects " + Math.round(getTier().getReflectionMultiplier() * 100.0F)
            + "% of incoming combat damage.";
    }
}
