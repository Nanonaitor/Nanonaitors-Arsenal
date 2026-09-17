package com.nanonaitor.arsenal.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Arsenal text layout; leaves vanilla attributes and pack-owned tooltip frames intact. */
public final class WeaponTooltipLayout {
    public static void heading(List<Component> lines, boolean expanded, String traits) {
        lines.add(Component.literal("Traits: ").withStyle(ChatFormatting.GOLD)
            .append(expanded ? Component.literal("Details").withStyle(ChatFormatting.DARK_GRAY)
                : Component.literal("Hold ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("SHIFT").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" for details").withStyle(ChatFormatting.DARK_GRAY))));
        lines.add(Component.literal(traits).withStyle(ChatFormatting.YELLOW));
    }

    public static void details(List<Component> lines, int from) {
        List<Component> original = new java.util.ArrayList<>(lines.subList(from,lines.size()));
        lines.subList(from,lines.size()).clear();
        for (Component line : original) {
            ChatFormatting color = line.getStyle().getColor()!=null
                && line.getStyle().getColor().getValue()==ChatFormatting.DARK_RED.getColor()
                ? ChatFormatting.RED : ChatFormatting.GRAY;
            StringBuilder part = new StringBuilder();
            for (String word : line.getString().split("\\s+")) {
                if (part.length()>0 && part.length()+word.length()+1>52) {
                    lines.add(Component.literal("  "+part).withStyle(color));
                    part.setLength(0);
                }
                if(part.length()>0)part.append(' ');
                part.append(word);
            }
            if(part.length()>0)lines.add(Component.literal("  "+part).withStyle(color));
        }
    }
    private WeaponTooltipLayout() {}
}
