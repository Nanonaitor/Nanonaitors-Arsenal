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
        return hand == EnumHand.MAIN_HAND && ArsenalCompatManager.canUseTwoHanded(player);
    }

    public boolean isTwoHandedReady(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() == this
            && ArsenalCompatManager.canUseTwoHanded(player);
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

    @Override protected void appendShieldTooltip(List<String> tooltip) {
        line(tooltip, TextFormatting.GOLD, "Extremely durable two-handed fortress shield");
        line(tooltip, TextFormatting.AQUA, "15% passive damage reduction when ready");
        line(tooltip, TextFormatting.BLUE, "Can shield all directed attacks from any direction");
        line(tooltip, TextFormatting.RED, "Damage: 1 + total armor points");
        line(tooltip, TextFormatting.DARK_GRAY, "Guard and attack for a 4-block area bash");
        line(tooltip, TextFormatting.GRAY, "40% slower while carried; 75% slower while guarding");
        line(tooltip, TextFormatting.DARK_RED,
            "Requires an empty offhand for every ability (XAT Titans exempt)");
    }
}
