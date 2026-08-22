package com.nanonaitor.arsenal.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.TooltipDisplay;

public class ArsenalWeaponItem extends Item {
    private final WeaponTier tier;
    private final WeaponKind kind;

    public ArsenalWeaponItem(WeaponTier tier, WeaponKind kind, Properties properties) {
        super(properties);
        this.tier = tier;
        this.kind = kind;
    }

    public WeaponTier tier() { return tier; }
    public WeaponKind kind() { return kind; }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        if (kind == WeaponKind.BATTERING_RAM) {
            consumer.accept(new com.nanonaitor.arsenal.client.BatteringRamClientExtensions());
        }
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) {
        return kind == WeaponKind.BALL_AND_CHAIN || kind == WeaponKind.BATTERING_RAM
            || kind == WeaponKind.FLAIL ? 72000 : 0;
    }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) {
        // The ram has its own stable two-handed carry/charge poses. BLOCK would
        // layer Minecraft's one-handed shield transform over those poses and make
        // the first-person model fight or snap while the charge is active.
        return ItemUseAnimation.NONE;
    }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        switch (kind) {
            case MORNING_STAR -> {
                lines.accept(Component.literal("Fully charged hits fracture 20% armor per stack.").withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Caps at " + tier.fractureCap + " stacks; 30 secs on mobs, 10 secs on players.").withStyle(ChatFormatting.GRAY));
            }
            case SCIMITAR -> lines.accept(Component.literal("Hits inflict Weakness "
                + (tier.ramBreakLevel >= 3 ? "II" : "I") + " for 10 secs.")
                .withStyle(ChatFormatting.DARK_PURPLE));
            case CLAWS -> {
                lines.accept(Component.literal("Hold left/right click to auto-attack.")
                    .withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Fully charged paired hits pierce invulnerability!")
                    .withStyle(ChatFormatting.GRAY));
            }
            case LINKED_CLAWS -> lines.accept(Component.literal("Linked to the matching main-hand claws.").withStyle(ChatFormatting.DARK_GRAY));
            case FLAIL -> {
                lines.accept(Component.literal("Hold attack to strike every target within 4 blocks.").withStyle(ChatFormatting.GOLD));
            }
            case BATTERING_RAM -> {
                lines.accept(Component.literal("2-Handed").withStyle(ChatFormatting.RED));
                lines.accept(Component.literal("Entity damage uses the attack charge present when the ram begins.").withStyle(ChatFormatting.YELLOW));
                lines.accept(Component.literal(switch (tier) {
                    case WOOD -> "Breaks soft terrain, foliage, wool and glass.";
                    case STONE, COPPER, GOLD -> "Also breaks planks and constructed wooden blocks.";
                    case IRON -> "Also breaks logs, cobble, clay, concrete and brickwork.";
                    case DIAMOND -> "Also breaks common stone, deepslate and masonry.";
                    case NETHERITE -> "Breaks every Battering Ram material tier.";
                }).withStyle(ChatFormatting.GRAY));
            }
            case BALL_AND_CHAIN -> {
                int charges = tier == WeaponTier.GOLD ? 2 : 3;
                lines.accept(Component.literal(
                    "2-Handed: Hold Attack to Swing, let go to release.")
                    .withStyle(ChatFormatting.RED));
                lines.accept(Component.literal("Full charge with " + charges
                    + " swings to pierce all armor.")
                    .withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
