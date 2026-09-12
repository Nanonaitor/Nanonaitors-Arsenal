package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.client.ArsenalTooltip;
import com.nanonaitor.arsenal.combat.DoubleBladedScimitarCombat;
import com.nanonaitor.arsenal.compat.ReskillableCompat;
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
import net.minecraftforge.fml.common.Loader;

/** Blade Staff. Legacy class and registry IDs are retained for save compatibility. */
public final class ItemDoubleBladedScimitar extends ItemArsenalWeapon {
    public static final ResourceLocation REFLECTING = new ResourceLocation(
        "nanonaitors_arsenal", "reflecting");

    public ItemDoubleBladedScimitar(WeaponTier tier) {
        // Vanilla swords deal material damage + 4. This family deals one more.
        super(tier, "double_bladed_scimitar",
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
        return Loader.isModLoaded("somanyenchantments") && id != null
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
                "Blade Staff with continuous attacks.")) return;
        tooltip.add(TextFormatting.AQUA + "Empty off-hand: hold attack to auto-attack.");
        tooltip.add(TextFormatting.GOLD + "Melee hits damage other enemies within "
            + (getTier() == WeaponTier.SENTIENT ? "3" : "2") + " blocks of the target.");
        tooltip.add(TextFormatting.BLUE + "Right click: reflect attacks for 1 sec.");
        tooltip.add(TextFormatting.DARK_PURPLE
            + "Reflection returns incoming damage before armor reduction.");
        tooltip.add(TextFormatting.DARK_PURPLE
            + "Reflection also returns harmful effects delivered by the attack.");
        tooltip.add(TextFormatting.DARK_PURPLE
            + "Reflected non-melee attacks Stun their attacker for 1 sec.");
        tooltip.add(TextFormatting.GRAY
            + "Failed guard cooldown: 3 secs; successful reflection: 0.5 secs.");
        tooltip.add(TextFormatting.RED
            + "Occupied off-hand: half attack speed; special abilities disabled.");
        if (getTier() == WeaponTier.LIVING || getTier() == WeaponTier.SENTIENT) {
            tooltip.add(TextFormatting.DARK_GREEN + "Held: cleanses Call of the Hive from entities within "
                + (getTier() == WeaponTier.SENTIENT ? "10" : "5") + " blocks.");
        }
    }
}
