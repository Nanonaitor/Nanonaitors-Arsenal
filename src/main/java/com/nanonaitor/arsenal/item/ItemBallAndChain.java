package com.nanonaitor.arsenal.item;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemBallAndChain extends ItemArsenalWeapon {
    public ItemBallAndChain(WeaponTier tier) {
        super(tier, "ball_and_chain", 3.0D + tier.getMaterial().getAttackDamage(), -3.5D);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "swinging"),
            (stack, world, entity) -> isActivelySwinging(stack, entity) ? 1.0F : 0.0F);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID,"guarding"),
            (stack,world,entity) -> entity instanceof EntityPlayer
                && entity.getHeldItemMainhand().getItem()==this && entity.getHeldItemOffhand().isEmpty()
                && entity.isHandActive() && entity.getActiveHand()==EnumHand.MAIN_HAND
                && !entity.getEntityData().getBoolean("ArsenalBallAndChainActive") ? 1.0F:0.0F);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "animation_part"),
            (stack, world, entity) -> stack.hasTagCompound()
                ? stack.getTagCompound().getInteger("ArsenalAnimationPart") : 0.0F);
    }

    private static boolean isActivelySwinging(ItemStack stack, EntityLivingBase entity) {
        return entity != null
            && entity.getHeldItemMainhand().getItem() == stack.getItem()
            && entity.getEntityData().getBoolean("ArsenalBallAndChainActive");
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BLOCK; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if(player.getCooldownTracker().hasCooldown(this))return new ActionResult<>(EnumActionResult.PASS,held);
        if (hand != EnumHand.MAIN_HAND || !player.getHeldItemOffhand().isEmpty()
            || player.getEntityData().getBoolean("ArsenalBallAndChainActive"))
            return new ActionResult<>(EnumActionResult.PASS, held);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, held);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Hold attack to swing; release to throw.")) return;
        tooltip.add(TextFormatting.GRAY + "Higher rotations increase throw reach and damage.");
        tooltip.add(TextFormatting.GRAY + "An empty offhand allows guarding.");
        tooltip.add(TextFormatting.RED + "Two-Handed");
    }

}
