package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.client.ArsenalTooltip;
import com.nanonaitor.arsenal.enchantment.EnchantmentLongChain;
import com.nanonaitor.arsenal.enchantment.EnchantmentRotationForce;
import com.nanonaitor.arsenal.enchantment.EnchantmentRecovery;
import com.nanonaitor.arsenal.enchantment.EnchantmentBreeched;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.Map;
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
        if (tier==WeaponTier.LIVING) {
            int points=stack.hasTagCompound()?stack.getTagCompound().getInteger("srpkills"):0;
            event.getToolTip().add(TextFormatting.GRAY+"Evolution progress: "+points+" / "+ArsenalCompatManager.getSrpEvolutionThreshold()+".");
        }
    }

    private static final String[] ARSENAL_ENCHANTMENTS = {
        "long_chain", "rotation_force", "recovery", "breeched",
        "reverso", "luna", "magic_log", "endure"
    };

    /**
     * Hovering an item or book shows what each Arsenal enchantment does, right
     * under the enchantment's name. Knockback on a Ball and Chain or Battering
     * Ram also explains its extra use there.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void explainEnchantments(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        if (enchantments.isEmpty()) return;
        java.util.List<String> tip = event.getToolTip();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null || enchantment.getRegistryName() == null) continue;
            String explanation = null;
            if (NanonaitorsArsenal.MOD_ID.equals(enchantment.getRegistryName().getResourceDomain())) {
                String key = "enchantment.nanonaitors_arsenal." + enchantment.getRegistryName().getResourcePath() + ".desc";
                if (I18n.hasKey(key)) explanation = I18n.format(key);
            } else if (enchantment == net.minecraft.init.Enchantments.KNOCKBACK) {
                if (stack.getItem() instanceof com.nanonaitor.arsenal.item.ItemBallAndChain) {
                    explanation = "Also shoves back melee attackers you guard against.";
                } else if (stack.getItem() instanceof com.nanonaitor.arsenal.item.ItemBatteringRam) {
                    explanation = "Adds to the ram's own push, on normal and charge hits.";
                }
            }
            if (explanation == null) continue;
            String name = TextFormatting.getTextWithoutFormattingCodes(enchantment.getTranslatedName(entry.getValue()));
            int at = -1;
            for (int i = 0; i < tip.size(); i++) {
                if (name != null && name.equals(TextFormatting.getTextWithoutFormattingCodes(tip.get(i)))) { at = i; break; }
            }
            String line = (enchantment.isCurse() ? TextFormatting.RED : TextFormatting.DARK_GRAY) + explanation;
            if (at >= 0) tip.add(at + 1, line);
        }
    }

    /** Keeps one copy when another tooltip provider adds the same explanation. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void deduplicateEnchantmentDescriptions(ItemTooltipEvent event) {
        for (String key : ARSENAL_ENCHANTMENTS) {
            String expected = I18n.format(
                "enchantment.nanonaitors_arsenal." + key + ".desc");
            boolean found = false;
            java.util.Iterator<String> lines = event.getToolTip().iterator();
            while (lines.hasNext()) {
                String plain = TextFormatting.getTextWithoutFormattingCodes(lines.next());
                if (!expected.equals(plain)) continue;
                if (found) lines.remove();
                else found = true;
            }
        }
    }

}
