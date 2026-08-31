package com.nanonaitor.arsenal.item;

import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;

/** A light, one-hit shield built around an aggressive forward counter. */
public final class ItemTartsyShield extends ItemArsenalShield {
    public ItemTartsyShield() { super("tartsy_shield", 768); }

    @Override
    public boolean canBeginGuard(EntityPlayer player, EnumHand hand) {
        return player.getHeldItem(hand).getItem() == this
            && !player.getCooldownTracker().hasCooldown(this);
    }

    @Override protected String shieldSummary() {
        return "One-handed spiked assault shield.";
    }

    @Override protected void appendShieldDetails(List<String> tooltip) {
        line(tooltip, TextFormatting.AQUA, "Negates any one hit, then disables for 4 secs");
        line(tooltip, TextFormatting.BLUE, "Attack while guarding to charge forward");
        line(tooltip, TextFormatting.DARK_PURPLE, "Dash: 1 sec immunity, 2 damage and Stunned for 1 sec");
        line(tooltip, TextFormatting.RED, "A confirmed dash hit primes one guaranteed critical");
    }
}
