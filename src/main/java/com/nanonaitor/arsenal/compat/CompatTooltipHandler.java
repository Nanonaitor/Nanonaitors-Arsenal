package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import com.nanonaitor.arsenal.enchantment.EnchantmentLongChain;
import com.nanonaitor.arsenal.enchantment.EnchantmentRotationForce;
import com.nanonaitor.arsenal.enchantment.EnchantmentRecovery;
import com.nanonaitor.arsenal.enchantment.EnchantmentBreeched;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class CompatTooltipHandler {
    private CompatTooltipHandler() {}
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack=event.getItemStack();
        if (stack.getItem() instanceof ItemArsenalWeapon) {
            // DDD normally receives configured distributions during capability
            // attachment. Arsenal supplies optional distributions dynamically,
            // so apply them before DDD's normal-priority tooltip renderer runs.
            ItemArsenalWeapon weapon = (ItemArsenalWeapon) stack.getItem();
            if (!DistinctDamageCompat.apply(stack, weapon) && DistinctDamageCompat.isLoaded()) {
                DistinctDamageCompat.addFallbackTooltip(event.getToolTip(), weapon);
            }
        }
        if (stack.getItem() == ModContent.IRON_CHAIN_ITEM) {
            if (ArsenalTooltip.begin(event.getToolTip(), TextFormatting.GOLD,
                    "Placeable chain and chain-weapon crafting component.")) {
                event.getToolTip().add(TextFormatting.GRAY
                    + "Connects along the axis of the face it is placed against.");
                event.getToolTip().add(TextFormatting.DARK_GRAY
                    + "Used to craft Flails and Balls & Chains.");
            }
            return;
        }
        addRaceAffinityTooltip(event, stack);
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        boolean arsenalEnchantment = enchantments.keySet().stream().anyMatch(enchantment ->
            enchantment instanceof EnchantmentLongChain
                || enchantment instanceof EnchantmentRotationForce);
        boolean recoveryEnchantment = enchantments.keySet().stream().anyMatch(enchantment ->
            enchantment instanceof EnchantmentRecovery);
        boolean breechedEnchantment = enchantments.keySet().stream().anyMatch(enchantment ->
            enchantment instanceof EnchantmentBreeched);
        if (arsenalEnchantment || recoveryEnchantment || breechedEnchantment) {
            if (ArsenalTooltip.begin(event.getToolTip(), TextFormatting.GOLD,
                    arsenalEnchantment ? "Arsenal chain-weapon enchantment."
                        : breechedEnchantment ? "Arsenal shield curse."
                        : "Arsenal shield enchantment.")) {
                for (Enchantment enchantment : enchantments.keySet()) {
                    if (enchantment instanceof EnchantmentLongChain) {
                        event.getToolTip().add(TextFormatting.GRAY
                            + I18n.format("enchantment.nanonaitors_arsenal.long_chain.desc"));
                    } else if (enchantment instanceof EnchantmentRotationForce) {
                        event.getToolTip().add(TextFormatting.GRAY
                            + I18n.format("enchantment.nanonaitors_arsenal.rotation_force.desc"));
                    } else if (enchantment instanceof EnchantmentRecovery) {
                        event.getToolTip().add(TextFormatting.GRAY
                            + I18n.format("enchantment.nanonaitors_arsenal.recovery.desc"));
                    } else if (enchantment instanceof EnchantmentBreeched) {
                        event.getToolTip().add(TextFormatting.RED
                            + I18n.format("enchantment.nanonaitors_arsenal.breeched.desc"));
                    }
                }
            }
            return;
        }
        if (!(stack.getItem() instanceof ItemArsenalWeapon)) return;
        if (!GuiScreen.isShiftKeyDown()) return;
        WeaponTier tier=((ItemArsenalWeapon)stack.getItem()).getTier();
        if (tier==WeaponTier.SILVER) event.getToolTip().add(TextFormatting.GRAY+"+2 damage vs undead.");
        if (tier.isMyrmex()) event.getToolTip().add(TextFormatting.GRAY+"+4 damage vs non-arthropods and Death Worms.");
        if (tier.isVenom()) event.getToolTip().add(TextFormatting.DARK_GREEN+"Poison III for 10 secs on hit.");
        if (tier==WeaponTier.FLAMED_DRAGONBONE) {
            event.getToolTip().add(TextFormatting.RED+"Ignites and knocks back.");
            event.getToolTip().add(TextFormatting.RED+"+13.5 damage vs Ice Dragons.");
        }
        if (tier==WeaponTier.ICED_DRAGONBONE) {
            event.getToolTip().add(TextFormatting.AQUA+"Freezes, slows, and knocks back.");
            event.getToolTip().add(TextFormatting.AQUA+"+13.5 damage vs Fire Dragons.");
        }
        if (tier==WeaponTier.ELECTRIC_DRAGONBONE) {
            event.getToolTip().add(TextFormatting.LIGHT_PURPLE+"Chains lightning and knocks back.");
            event.getToolTip().add(TextFormatting.LIGHT_PURPLE
                +"+6.75 damage vs Fire and Ice Dragons.");
        }
        if (tier==WeaponTier.LIVING) {
            int points=stack.hasTagCompound()?stack.getTagCompound().getInteger("srpkills"):0;
            event.getToolTip().add(TextFormatting.DARK_RED+"Parasite evolution: "+points+" / "+ArsenalCompatManager.getSrpEvolutionThreshold());
        }
        if (tier==WeaponTier.LIVING || tier==WeaponTier.SENTIENT) {
            boolean sentient=tier==WeaponTier.SENTIENT;
            if (stack.getItem() instanceof ItemMorningStar)
                event.getToolTip().add(TextFormatting.WHITE+"Renders armour "
                    +TextFormatting.RED+"useless"+TextFormatting.WHITE+" over time.");
            else if (stack.getItem() instanceof ItemClaws)
                event.getToolTip().add(TextFormatting.WHITE+"Inflicts "
                    +TextFormatting.RED+"life-threatening"+TextFormatting.WHITE+" injuries.");
            else if (stack.getItem() instanceof ItemFlail)
                event.getToolTip().add(TextFormatting.RED+(sentient?"Decimates":"Wipes out")
                    +TextFormatting.WHITE+" a large area.");
            else if (stack.getItem() instanceof ItemBallAndChain)
                event.getToolTip().add(TextFormatting.RED+"Kill one"
                    +TextFormatting.WHITE+" kill many.");
            else if (stack.getItem() instanceof ItemBatteringRam)
                event.getToolTip().add(TextFormatting.WHITE+"Lashes out "
                    +TextFormatting.GREEN+"virulent"+TextFormatting.WHITE+" strikes.");
            else if (stack.getItem() instanceof ItemScimitar)
                event.getToolTip().add(TextFormatting.WHITE+"Leaves its victims "
                    +TextFormatting.DARK_GRAY+"too weak to fight"+TextFormatting.WHITE+".");
            if (sentient) event.getToolTip().add(TextFormatting.WHITE
                +"You can't hide anymore...");
        }
    }

    /** Removes a second identical Rotation Force explanation added by tooltip mods. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void deduplicateRotationForceTooltip(ItemTooltipEvent event) {
        String expected = I18n.format("enchantment.nanonaitors_arsenal.rotation_force.desc");
        boolean found = false;
        java.util.Iterator<String> lines = event.getToolTip().iterator();
        while (lines.hasNext()) {
            String plain = TextFormatting.getTextWithoutFormattingCodes(lines.next());
            if (!expected.equals(plain)) continue;
            if (found) lines.remove();
            else found = true;
        }
    }

    private static void addRaceAffinityTooltip(ItemTooltipEvent event, ItemStack stack) {
        if (!RaceWeaponAffinityCompat.isEnabled()) return;
        String affinity = RaceWeaponAffinityCompat.affinityRace(stack);
        if (affinity == null) return;
        int percent = RaceWeaponAffinityCompat.configuredPercent();
        boolean matching = Minecraft.getMinecraft().player != null
            && ArsenalCompatManager.isXatRace(Minecraft.getMinecraft().player, affinity);
        String line = matching
            ? TextFormatting.LIGHT_PURPLE + "+" + percent + "% Damage!"
            : TextFormatting.ITALIC + "+" + percent + "% Damage as " + affinity;
        event.getToolTip().add(Math.min(1, event.getToolTip().size()), line);
    }
}
