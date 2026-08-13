package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public abstract class ItemArsenalShield extends Item {
    protected ItemArsenalShield(String id, int durability) {
        setRegistryName(NanonaitorsArsenal.MOD_ID, id);
        setUnlocalizedName(NanonaitorsArsenal.MOD_ID + "." + id);
        setCreativeTab(NanonaitorsArsenal.CREATIVE_TAB);
        setMaxStackSize(1);
        if (durability > 0) setMaxDamage(durability);
        addPropertyOverride(new ResourceLocation("blocking"), (stack, world, entity) ->
            entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack ? 1.0F : 0.0F);
    }

    public abstract boolean canBeginGuard(EntityPlayer player, EnumHand hand);

    @Override public int getMaxItemUseDuration(ItemStack stack) { return 72000; }
    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BLOCK; }

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
        appendShieldTooltip(tooltip);
    }

    protected abstract void appendShieldTooltip(List<String> tooltip);

    protected static void line(List<String> tooltip, TextFormatting color, String text) {
        tooltip.add(color + text);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.type == EnumEnchantmentType.BREAKABLE
            || super.canApplyAtEnchantingTable(stack, enchantment);
    }

    @Override public int getItemEnchantability() { return 15; }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }
}
