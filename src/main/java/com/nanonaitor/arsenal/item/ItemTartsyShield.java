package com.nanonaitor.arsenal.item;

import java.util.List;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.config.ConfiguredEffects;
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
        line(tooltip, TextFormatting.GRAY, "Blocks one hit; attack while guarding to dash.");
        String effects = ConfiguredEffects.displayNames(ArsenalConfig.tartsyShield.dashEffects);
        if (!effects.isEmpty()) line(tooltip, TextFormatting.GRAY, "Dash hits apply: " + effects + ".");
    }
}
