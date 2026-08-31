package com.nanonaitor.arsenal.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import java.util.List;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;

public final class ItemSunWarBulwark extends ItemArsenalShield {
    public static final double ATTACK_SPEED = 0.25D;

    public ItemSunWarBulwark() { super("sun_war_bulwark", 4096); }

    @Override
    public boolean canBeginGuard(EntityPlayer player, EnumHand hand) {
        boolean held = player.getHeldItem(hand).getItem() == this;
        boolean free = hand == EnumHand.MAIN_HAND ? player.getHeldItemOffhand().isEmpty()
            : player.getHeldItemMainhand().isEmpty();
        if (held && !free && !player.world.isRemote) {
            player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                "I need both hands to shield with the bulwark!"), true);
        }
        return held && free;
    }

    public boolean isTwoHandedReady(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() == this && player.getHeldItemOffhand().isEmpty()
            || player.getHeldItemOffhand().getItem() == this && player.getHeldItemMainhand().isEmpty();
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 0.0D, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", ATTACK_SPEED - 4.0D, 0));
        }
        return modifiers;
    }

    @Override protected String shieldSummary() {
        return "Extremely durable two-handed fortress shield.";
    }

    @Override protected void appendShieldDetails(List<String> tooltip) {
        line(tooltip, TextFormatting.AQUA, "15% passive damage reduction when ready");
        line(tooltip, TextFormatting.BLUE, "Can shield all directed attacks from any direction");
        line(tooltip, TextFormatting.RED, "Damage: 1 + total armor points");
        line(tooltip, TextFormatting.DARK_GRAY, "Guard and attack for a 4-block area bash");
        line(tooltip, TextFormatting.GRAY, "40% slower while carried; 75% slower while guarding");
        line(tooltip, TextFormatting.DARK_RED, "Needs both hands free to guard; passive always works");
    }
}
