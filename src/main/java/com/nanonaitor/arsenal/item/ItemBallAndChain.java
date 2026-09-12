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
        super(tier, "ball_and_chain", 3.0D + tier.getMaterial().getAttackDamage(), -3.4D);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "swinging"),
            (stack, world, entity) -> isActivelySwinging(stack, entity) ? 1.0F : 0.0F);
        addPropertyOverride(new ResourceLocation(NanonaitorsArsenal.MOD_ID, "animation_part"),
            (stack, world, entity) -> stack.hasTagCompound()
                ? stack.getTagCompound().getInteger("ArsenalAnimationPart") : 0.0F);
    }

    private static boolean isActivelySwinging(ItemStack stack, EntityLivingBase entity) {
        return entity != null
            && entity.getHeldItemMainhand().getItem() == stack.getItem()
            && (entity.getEntityData().getBoolean("ArsenalBallAndChainActive")
                || entity.isHandActive());
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BLOCK; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
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
        int charges = getTier() == WeaponTier.GOLD ? 2 : 3;
        if (!ArsenalTooltip.begin(tooltip, TextFormatting.GOLD,
                "Hold attack to swing; release to throw.")) return;
        if (getTier() == WeaponTier.GOLD) {
            tooltip.add(TextFormatting.GOLD + "Gold reaches full charge in 2 rotations.");
            tooltip.add(TextFormatting.YELLOW
                + "Its second charge jumps to full reach and throw damage.");
        } else {
            tooltip.add(TextFormatting.GOLD
                + "Hold attack to build up to " + charges + " charges.");
        }
        tooltip.add(TextFormatting.GRAY + "Wind-up: 0.5x damage, "
            + com.nanonaitor.arsenal.config.ArsenalConfig.reach.ballWindupReach + " base blocks.");
        tooltip.add(TextFormatting.GRAY + "Throw: "
            + com.nanonaitor.arsenal.config.ArsenalConfig.reach.ballThrowReachPerCharge + " base blocks per charge.");
        if (getTier() == WeaponTier.SILVER && net.minecraftforge.fml.common.Loader.isModLoaded("setbonus"))
            tooltip.add(TextFormatting.YELLOW + "Matching RLCraft Silver set: second rotation reaches full charge.");
        tooltip.add(TextFormatting.DARK_GRAY + "Reach and attack-speed modifiers apply.");
        if (getTier() == WeaponTier.GOLD) {
            tooltip.add(TextFormatting.GRAY
                + "Throws deal 1.25x / 2.25x damage at charges 1 / 2.");
        } else {
            tooltip.add(TextFormatting.GRAY
                + "Throws deal 1.25x / 1.75x / 2.25x damage.");
        }
        tooltip.add(TextFormatting.GRAY + "Hits in both directions; stops at solid blocks.");
        tooltip.add(TextFormatting.DARK_RED + "Full-charge throw pierces "
            + getTier().getArmorPiercePercent() + "% armor.");
        tooltip.add(TextFormatting.DARK_GRAY + "Throw hits fracture armor.");
        tooltip.add(TextFormatting.DARK_GRAY + "Empty offhand allows guarding; occupied offhand halves speed.");
    }

}
