package com.nanonaitor.arsenal.client;

import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Spartan-style compact mechanic tooltips for Arsenal equipment. */
@SideOnly(Side.CLIENT)
public final class ArsenalTooltip {
    private ArsenalTooltip() {}

    /** Adds the always-visible summary and returns whether details should show. */
    public static boolean begin(List<String> tooltip, TextFormatting color,
                                String summary) {
        tooltip.add(color + summary);
        boolean expanded = GuiScreen.isShiftKeyDown();
        if (expanded) {
            tooltip.add(TextFormatting.DARK_GRAY + "Showing details");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + "Hold " + TextFormatting.AQUA
                + "SHIFT" + TextFormatting.DARK_GRAY + " for details");
        }
        return expanded;
    }
}
