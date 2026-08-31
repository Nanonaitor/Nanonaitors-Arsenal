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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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
        } else if (kind == WeaponKind.MORNING_STAR) {
            consumer.accept(new com.nanonaitor.arsenal.client.MorningStarClientExtensions());
        } else if (kind == WeaponKind.BALL_AND_CHAIN) {
            consumer.accept(new com.nanonaitor.arsenal.client.BallChainClientExtensions());
        } else if (kind == WeaponKind.SCIMITAR) {
            consumer.accept(new com.nanonaitor.arsenal.client.ScimitarClientExtensions());
        } else if (kind == WeaponKind.BLADE_STAFF) {
            consumer.accept(new com.nanonaitor.arsenal.client.BladeStaffClientExtensions());
        }
    }

    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (kind == WeaponKind.BALL_AND_CHAIN && hand == InteractionHand.MAIN_HAND
            && player.getOffhandItem().isEmpty()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (kind == WeaponKind.SCIMITAR
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && player.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
            && main.kind() == WeaponKind.SCIMITAR && off.kind() == WeaponKind.SCIMITAR) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity user) {
        return kind == WeaponKind.BALL_AND_CHAIN || kind == WeaponKind.BATTERING_RAM
            || kind == WeaponKind.FLAIL || kind == WeaponKind.MORNING_STAR
            || kind == WeaponKind.SCIMITAR || kind == WeaponKind.BLADE_STAFF ? 72000 : 0;
    }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) {
        // The ram has its own stable two-handed carry/charge poses. BLOCK would
        // layer Minecraft's one-handed shield transform over those poses and make
        // the first-person model fight or snap while the charge is active.
        return kind == WeaponKind.SCIMITAR ? ItemUseAnimation.BLOCK : ItemUseAnimation.NONE;
    }

    @Override public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.literal(summary()).withStyle(ChatFormatting.GOLD));
        if (!com.nanonaitor.arsenal.client.ClientTooltip.expanded()) {
            lines.accept(Component.literal("Hold SHIFT for details").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        switch (kind) {
            case MORNING_STAR -> {
                lines.accept(Component.literal("Each charge quarter adds +10% damage.").withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Full charge: wide strike, Armor Fracture and 20% Stun chance.").withStyle(ChatFormatting.YELLOW));
                lines.accept(Component.literal("Caps at " + tier.fractureCap + " stacks; 30 secs on mobs, 10 secs on players.").withStyle(ChatFormatting.GRAY));
            }
            case SCIMITAR -> {
                lines.accept(Component.literal("Hits inflict Weakness "
                    + (tier.ramBreakLevel >= 3 ? "II" : "I") + " for 10 secs.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
                lines.accept(Component.literal("Can attack from either hand; dual wield to auto-alternate.")
                    .withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Dual wield and use to cross-guard.")
                    .withStyle(ChatFormatting.GRAY));
            }
            case CLAWS -> {
                lines.accept(Component.literal("Fully charged paired hits pierce invulnerability!")
                    .withStyle(ChatFormatting.GRAY));
            }
            case LINKED_CLAWS -> lines.accept(Component.literal("Linked to the matching main-hand claws.").withStyle(ChatFormatting.DARK_GRAY));
            case FLAIL -> {
            }
            case BATTERING_RAM -> {
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
                lines.accept(Component.literal("Full charge with " + charges
                    + " swings to pierce all armor.")
                    .withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("An occupied offhand halves rotation speed.")
                    .withStyle(ChatFormatting.DARK_RED));
            }
            case BLADE_STAFF -> {
                lines.accept(Component.literal("Empty offhand: hold attack to auto-strike at full charge.").withStyle(ChatFormatting.YELLOW));
                lines.accept(Component.literal("Melee hits damage other enemies within 2 blocks of the target.").withStyle(ChatFormatting.GOLD));
                lines.accept(Component.literal("Use to spin for 1 sec and reflect incoming damage before armor.").withStyle(ChatFormatting.AQUA));
                lines.accept(Component.literal("Reflected non-melee attacks Stun their attacker for 1 sec.").withStyle(ChatFormatting.DARK_PURPLE));
                lines.accept(Component.literal("Missed reflection: 3 sec cooldown; successful reflection: 0.5 sec.").withStyle(ChatFormatting.GRAY));
                lines.accept(Component.literal("An occupied offhand disables reflection and halves attack speed.").withStyle(ChatFormatting.DARK_RED));
            }
        }
    }
    private String summary() {
        return switch (kind) {
            case MORNING_STAR -> "Hold attack to charge a sweeping strike.";
            case SCIMITAR -> "Fast blade that inflicts Weakness.";
            case CLAWS -> "Hold left/right click to auto-attack.";
            case LINKED_CLAWS -> "Linked to the matching main-hand claws.";
            case FLAIL -> "Hold attack to strike every target within 4 blocks.";
            case BATTERING_RAM -> "2-Handed siege weapon.";
            case BALL_AND_CHAIN -> "Hold Attack to Swing, let go to release.";
            case BLADE_STAFF -> "Two-ended blade with auto-attacks and timed reflection.";
        };
    }
}
