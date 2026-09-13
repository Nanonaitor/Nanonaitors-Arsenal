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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;

public final class ArsenalShieldItem extends ShieldItem {
    public enum Type { SUN_WAR, TARTSY }
    private final Type type;
    public ArsenalShieldItem(Type type, Properties properties) { super(properties); this.type = type; }
    public Type shieldType() { return type; }
    @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot){
        if(type!=Type.SUN_WAR || slot!=net.minecraft.world.entity.EquipmentSlot.MAINHAND)return super.getDefaultAttributeModifiers(slot);
        return com.google.common.collect.ImmutableMultimap.of(
            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID,"Bulwark cooldown",-3.75D,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,net.minecraft.world.item.enchantment.Enchantment enchantment){
        if(!com.nanonaitor.arsenal.config.ArsenalConfig.SHIELD_ENCHANTMENTS.get())
            return enchantment==net.minecraft.world.item.enchantment.Enchantments.UNBREAKING || enchantment==net.minecraft.world.item.enchantment.Enchantments.MENDING;
        return super.canApplyAtEnchantingTable(stack,enchantment);
    }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.nanonaitor.arsenal.client.ShieldClientExtensions());
    }

    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (type == Type.TARTSY && player.getCooldowns().isOnCooldown(stackFor(player, hand).getItem())) {
            return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        ItemStack opposite = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
        if (type == Type.SUN_WAR && !opposite.isEmpty()) {
            if (player instanceof ServerPlayer server) server.connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal(
                    "I need both hands to shield with the bulwark!")
                    .withStyle(ChatFormatting.RED)));
            return net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    // NONE lets the custom client extension supply separate carry and overhead guard poses.
    // Blocking is handled by CombatEvents while this stack is actively in use.
    @Override public UseAnim getUseAnimation(ItemStack stack) {
        return type == Type.TARTSY ? UseAnim.BLOCK : UseAnim.NONE;
    }
    @Override public int getUseDuration(ItemStack stack) { return 72000; }

    @Override public void appendHoverText(ItemStack stack, Level context,
            List<Component> lines, TooltipFlag flag) {
        if (type == Type.TARTSY) {
            lines.add(Component.literal("One-handed spiked assault shield.").withStyle(ChatFormatting.GOLD));
            if (!com.nanonaitor.arsenal.client.ClientTooltip.expanded()) {
                lines.add(Component.literal("Hold SHIFT for details").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }
            lines.add(Component.literal("Negates any one hit, then disables for 4 secs.").withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal("Attack while guarding to charge forward.").withStyle(ChatFormatting.BLUE));
            lines.add(Component.literal("Dash: 1 sec immunity, 2 damage and Stunned for 1 sec.").withStyle(ChatFormatting.DARK_PURPLE));
            lines.add(Component.literal("A confirmed dash hit primes one guaranteed critical.").withStyle(ChatFormatting.RED));
            return;
        }
        lines.add(Component.literal("Extremely durable, two-handed bulwark; 15% passive damage reduction.").withStyle(ChatFormatting.GOLD));
        if (!com.nanonaitor.arsenal.client.ClientTooltip.expanded()) {
            lines.add(Component.literal("Hold SHIFT for details").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(Component.literal("Can shield all directed attacks from any direction.").withStyle(ChatFormatting.AQUA));
        lines.add(Component.literal("Damage: 1 + armor points, scaled by attack charge. Guard and attack for a 4-block bash.").withStyle(ChatFormatting.RED));
        lines.add(Component.literal("Wait about 4 secs between attacks for full damage.").withStyle(ChatFormatting.YELLOW));
        lines.add(Component.literal("40% slower while carried; 75% slower while guarding.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Guarding requires the opposite hand to be empty.").withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.literal("An occupied opposite hand halves attack speed.").withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.literal("Does not stop environmental hazards.").withStyle(ChatFormatting.DARK_GRAY));
    }
    private static ItemStack stackFor(Player player, InteractionHand hand) { return player.getItemInHand(hand); }
}
