package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Enchantments;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

public abstract class ItemArsenalShield extends ItemShield {
    protected ItemArsenalShield(String id, int durability) {
        setRegistryName(NanonaitorsArsenal.MOD_ID, id);
        setUnlocalizedName(NanonaitorsArsenal.MOD_ID + "." + id);
        setCreativeTab(NanonaitorsArsenal.CREATIVE_TAB);
        setMaxStackSize(1);
        if (durability > 0) setMaxDamage(durability);
        addPropertyOverride(new ResourceLocation("blocking"), (stack, world, entity) ->
            entity != null && (entity.isHandActive() && entity.getActiveItemStack() == stack
                || this instanceof ItemSunWarBulwark
                    && entity.getEntityData().getBoolean("ArsenalBulwarkMenuGuard")
                    && (entity.getHeldItemMainhand() == stack
                        || entity.getHeldItemOffhand() == stack)) ? 1.0F : 0.0F);
    }

    public abstract boolean canBeginGuard(EntityPlayer player, EnumHand hand);

    @Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BLOCK; }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        // ItemShield hardcodes vanilla's "Shield" display name. Arsenal shields
        // must resolve their own registered translation keys instead.
        return I18n.translateToLocal(getUnlocalizedName(stack) + ".name").trim();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (!canBeginGuard(player, hand)) return new ActionResult<>(EnumActionResult.FAIL, stack);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    public boolean isGuarding(EntityPlayer player) {
        return player.isHandActive() && player.getActiveItemStack().getItem() == this;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (ArsenalTooltip.begin(tooltip, shieldSummaryColor(), shieldSummary())) {
            appendShieldDetails(tooltip);
        }
    }

    protected abstract String shieldSummary();
    protected TextFormatting shieldSummaryColor() { return TextFormatting.GOLD; }
    protected abstract void appendShieldDetails(List<String> tooltip);

    protected static void line(List<String> tooltip, TextFormatting color, String text) {
        tooltip.add(color + text);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        // Do not delegate back to Item here. Some 1.12.2 enchantment/JEI
        // compatibility paths ask the enchantment to ask the item again,
        // causing unbounded recursion and a StackOverflowError during startup.
        return enchantment == Enchantments.UNBREAKING
            || enchantment == Enchantments.MENDING
            || com.nanonaitor.arsenal.config.ArsenalConfig.shields.allowShieldEnchantments
                && (enchantment == ModContent.RECOVERY
                    || enchantment.type != null
                        && enchantment.type != net.minecraft.enchantment.EnumEnchantmentType.BREAKABLE
                        && enchantment.type != net.minecraft.enchantment.EnumEnchantmentType.ALL
                        && enchantment.type.canEnchantItem(net.minecraft.init.Items.SHIELD));
    }

    @Override public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (com.nanonaitor.arsenal.config.ArsenalConfig.shields.allowShieldEnchantments) return true;
        for (Enchantment enchantment : net.minecraft.enchantment.EnchantmentHelper.getEnchantments(book).keySet())
            if (enchantment != Enchantments.UNBREAKING && enchantment != Enchantments.MENDING) return false;
        return true;
    }

    @Override public int getItemEnchantability() { return 15; }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }
}
