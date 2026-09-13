package com.nanonaitor.arsenal.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ArsenalWeaponItem extends net.minecraft.world.item.SwordItem {
    private final WeaponTier tier;
    private final WeaponKind kind;
    private final com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> combatAttributes;

    public ArsenalWeaponItem(WeaponTier tier, WeaponKind kind, Properties properties) {
        super(tier.material, (int)(kind == WeaponKind.CLAWS || kind == WeaponKind.LINKED_CLAWS
            ? tier.clawDamage() - 1.0F - tier.material.getAttackDamageBonus() : kind.damageBaseline),
            kind == WeaponKind.BATTERING_RAM && tier == WeaponTier.GOLD ? -3.0F : kind.speedModifier, properties);
        this.tier = tier;
        this.kind = kind;
        float damage=kind==WeaponKind.CLAWS || kind==WeaponKind.LINKED_CLAWS ? tier.clawDamage()-1
            : kind==WeaponKind.SCIMITAR ? com.nanonaitor.arsenal.registry.ModItems.roundedScimitarDamage(tier)-1
            : tier.material.getAttackDamageBonus()+kind.damageBaseline;
        float speed=kind==WeaponKind.BATTERING_RAM && tier==WeaponTier.GOLD?-3:kind.speedModifier;
        combatAttributes=com.google.common.collect.ImmutableMultimap.of(
            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID,"Weapon modifier",damage,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION),
            net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID,"Weapon modifier",speed,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }

    @Override public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute,net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot){
        return slot==EquipmentSlot.MAINHAND ? combatAttributes : super.getDefaultAttributeModifiers(slot);
    }

    public WeaponTier tier() { return tier; }
    public WeaponKind kind() { return kind; }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.nanonaitor.arsenal.client.WeaponClientExtensions());
    }

    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == WeaponKind.BALL_AND_CHAIN && hand == InteractionHand.MAIN_HAND
            && player.getOffhandItem().isEmpty()) {
            player.startUsingItem(hand);
            return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        if (kind == WeaponKind.SCIMITAR
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && player.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
            && main.kind() == WeaponKind.SCIMITAR && off.kind() == WeaponKind.SCIMITAR) {
            player.startUsingItem(hand);
            return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
        }
        return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override public int getUseDuration(ItemStack stack) {
        return kind == WeaponKind.BALL_AND_CHAIN || kind == WeaponKind.BATTERING_RAM
            || kind == WeaponKind.FLAIL || kind == WeaponKind.MORNING_STAR
            || kind == WeaponKind.SCIMITAR || kind == WeaponKind.BLADE_STAFF ? 72000 : 0;
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) {
        // The ram has its own stable two-handed carry/charge poses. BLOCK would
        // layer Minecraft's one-handed shield transform over those poses and make
        // the first-person model fight or snap while the charge is active.
        return kind == WeaponKind.SCIMITAR ? UseAnim.BLOCK : UseAnim.NONE;
    }

    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override public void appendHoverText(ItemStack stack, Level context,
            List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal(summary()).withStyle(ChatFormatting.GOLD));
        if (!com.nanonaitor.arsenal.client.ClientTooltip.expanded()) {
            lines.add(Component.literal("Hold SHIFT for details").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        switch (kind) {
            case MORNING_STAR -> {
                lines.add(Component.literal("Each charge quarter adds +10% damage.").withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("Full charge: wide strike, Armor Fracture and 20% Stun chance.").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal("Caps at " + tier.fractureCap + " stacks; 30 secs on mobs, 10 secs on players.").withStyle(ChatFormatting.GRAY));
            }
            case SCIMITAR -> {
                lines.add(Component.literal("Hits inflict Weakness "
                    + tier.weaknessLevel() + " for 10 secs by default.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
                lines.add(Component.literal("Can attack from either hand; dual wield to auto-alternate.")
                    .withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("Dual wield and use to cross-guard.")
                    .withStyle(ChatFormatting.GRAY));
            }
            case CLAWS -> {
                lines.add(Component.literal("Fully charged paired hits pierce invulnerability!")
                    .withStyle(ChatFormatting.GRAY));
            }
            case LINKED_CLAWS -> lines.add(Component.literal("Linked to the matching main-hand claws.").withStyle(ChatFormatting.DARK_GRAY));
            case FLAIL -> {
            }
            case BATTERING_RAM -> {
                lines.add(Component.literal("Entity damage uses the attack charge present when the ram begins.").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal(switch (tier) {
                    case WOOD -> "Breaks soft terrain, foliage, wool and glass.";
                    case STONE, COPPER, GOLD -> "Also breaks planks and constructed wooden blocks.";
                    case IRON -> "Also breaks logs, cobble, clay, concrete and brickwork.";
                    case DIAMOND -> "Also breaks common stone, deepslate and masonry.";
                    default -> "Breaks terrain through material tier " + tier.ramBreakLevel + ".";
                }).withStyle(ChatFormatting.GRAY));
            }
            case BALL_AND_CHAIN -> {
                int charges = tier == WeaponTier.GOLD ? 2 : 3;
                lines.add(Component.literal("Full charge with " + charges
                    + " swings: " + tier.armorPiercePercent() + "% armor piercing.")
                    .withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("An occupied offhand halves rotation speed.")
                    .withStyle(ChatFormatting.DARK_RED));
            }
            case BLADE_STAFF -> {
                lines.add(Component.literal("Empty offhand: hold attack to auto-strike at full charge.").withStyle(ChatFormatting.YELLOW));
                lines.add(Component.literal("Melee hits damage other enemies within "
                    + ("sentient".equals(tier.id) ? "3" : "2")
                    + " blocks of the target.").withStyle(ChatFormatting.GOLD));
                lines.add(Component.literal("Use to spin for 1 sec and reflect incoming damage before armor.").withStyle(ChatFormatting.AQUA));
                lines.add(Component.literal("Reflection also returns harmful effects delivered by the attack.").withStyle(ChatFormatting.DARK_PURPLE));
                lines.add(Component.literal("Reflected non-melee attacks Stun their attacker for 1 sec.").withStyle(ChatFormatting.DARK_PURPLE));
                lines.add(Component.literal("Missed reflection: 3 sec cooldown; successful reflection: 0.5 sec.").withStyle(ChatFormatting.GRAY));
                lines.add(Component.literal("An occupied offhand disables reflection and halves attack speed.").withStyle(ChatFormatting.DARK_RED));
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
