package com.nanonaitor.arsenal.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public final class ArsenalShieldItem extends ShieldItem {
    public enum Type { SUN_WAR }
    private final Type type;
    public ArsenalShieldItem(Type type, Properties properties) { super(properties); this.type = type; }
    public Type shieldType() { return type; }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.nanonaitor.arsenal.client.BulwarkClientExtensions());
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (type == Type.SUN_WAR && (hand != InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty())) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }
    // NONE lets the custom client extension supply separate carry and overhead guard poses.
    // Blocking is handled by CombatEvents while this stack is actively in use.
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal("Extremely durable, two-handed bulwark; 15% passive damage reduction.").withStyle(ChatFormatting.GOLD));
        lines.accept(Component.literal("Can shield all directed attacks from any direction.").withStyle(ChatFormatting.AQUA));
        lines.accept(Component.literal("Damage: 1 + total armor points. Guard and attack for a 4-block area bash.").withStyle(ChatFormatting.RED));
        lines.accept(Component.literal("40% slower while carried; 75% slower while guarding.").withStyle(ChatFormatting.GRAY));
        lines.accept(Component.literal("Requires an empty offhand for every ability.").withStyle(ChatFormatting.DARK_RED));
        lines.accept(Component.literal("Does not stop environmental hazards.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
