package com.nanonaitor.arsenal.item;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public final class ItemFlail extends ItemArsenalWeapon {
    public ItemFlail(WeaponTier tier) {
        super(tier, "flail", 2.0D + tier.getMaterial().getAttackDamage(), -3.2D);
        addPropertyOverride(new ResourceLocation("nanonaitors_arsenal", "swinging"),
            (stack, world, entity) -> entity != null
                && entity.getEntityData().getBoolean("ArsenalFlailActive")
                && entity.getHeldItemMainhand().getItem() == this ? 1.0F : 0.0F);
        addPropertyOverride(new ResourceLocation("nanonaitors_arsenal", "animation_part"),
            (stack, world, entity) -> stack.hasTagCompound()
                ? stack.getTagCompound().getInteger("ArsenalAnimationPart") : 0.0F);
    }

    @Override
    public void addInformation(ItemStack stack, World world, List<String> tooltip,
                               ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "Hold left-click to swing continuously.");
        tooltip.add(TextFormatting.GRAY + "Hits all visible enemies within 4 blocks.");
    }
}
