package com.nanonaitor.arsenal.compat;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Formatting matches the profile's skill requirement section, not weapon traits. */
public final class RequirementTooltip {
    public static void append(List<Component> lines,int required,int current) {
        if(required<=0)return;
        lines.add(Component.empty());
        lines.add(Component.literal("Requirements:").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Component.literal(" - Strength: ").withStyle(ChatFormatting.WHITE)
            .append(Component.literal(Integer.toString(required)).withStyle(
                LevelRequirements.meets(current,required)?ChatFormatting.GREEN:ChatFormatting.RED)));
    }
    private RequirementTooltip() {}
}
