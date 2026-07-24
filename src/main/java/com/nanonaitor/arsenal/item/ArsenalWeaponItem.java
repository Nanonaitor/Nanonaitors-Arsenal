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
        return kind == WeaponKind.BATTERING_RAM ? ItemUseAnimation.BLOCK : ItemUseAnimation.NONE;
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
            case SCIMITAR -> lines.accept(Component.literal("10% chance on a fully charged hit to inflict Weakness II for 2 secs.").withStyle(ChatFormatting.GOLD));
            case CLAWS -> {
                lines.accept(Component.literal("Left click: main claw. Right click: linked claw.").withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Alternate fully charged hits to pierce invulnerability frames.").withStyle(ChatFormatting.GRAY));
                lines.accept(Component.literal("Requires an empty offhand; both claws share durability and enchantments.").withStyle(ChatFormatting.DARK_GRAY));
            }
            case LINKED_CLAWS -> lines.accept(Component.literal("Linked to the matching main-hand claws.").withStyle(ChatFormatting.DARK_GRAY));
            case FLAIL -> {
                lines.accept(Component.literal("Hold attack to strike every target within 4 blocks.").withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Cannot mine blocks while spinning.").withStyle(ChatFormatting.DARK_GRAY));
            }
            case BATTERING_RAM -> {
                lines.accept(Component.literal("Two-Handed: hold attack to charge and smash a forward 3x3 path.").withStyle(ChatFormatting.RED));
                lines.accept(Component.literal("Cannot charge at 3 hunger icons or less; costs durability per hit.").withStyle(ChatFormatting.DARK_RED));
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
                int reach = 4;
                lines.accept(Component.literal("Two-Handed: hold attack for up to " + charges + " charges, then release.").withStyle(ChatFormatting.RED));
                lines.accept(Component.literal("Throws " + reach + " blocks per charge and hits both outward and returning.").withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Full charge pierces " + tier.armorPiercePercent() + "% armor.").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
}
