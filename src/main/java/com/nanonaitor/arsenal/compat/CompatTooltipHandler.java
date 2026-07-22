package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class CompatTooltipHandler {
    private CompatTooltipHandler() {}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack=event.getItemStack();
        if (!(stack.getItem() instanceof ItemArsenalWeapon)) return;
        WeaponTier tier=((ItemArsenalWeapon)stack.getItem()).getTier();
        if (tier==WeaponTier.SILVER) event.getToolTip().add(TextFormatting.GRAY+"+2 damage vs undead.");
        if (tier.isMyrmex()) event.getToolTip().add(TextFormatting.GRAY+"+4 damage vs non-arthropods and Death Worms.");
        if (tier.isVenom()) event.getToolTip().add(TextFormatting.DARK_GREEN+"Poison III for 10 secs on hit.");
        if (tier==WeaponTier.FLAMED_DRAGONBONE) event.getToolTip().add(TextFormatting.RED+"Ignites and knocks back; bonus damage vs Ice Dragons.");
        if (tier==WeaponTier.ICED_DRAGONBONE) event.getToolTip().add(TextFormatting.AQUA+"Freezes, slows, and knocks back; bonus damage vs Fire Dragons.");
        if (tier==WeaponTier.ELECTRIC_DRAGONBONE) event.getToolTip().add(TextFormatting.LIGHT_PURPLE+"Chains lightning and knocks back; bonus damage vs dragons.");
        if (tier==WeaponTier.LIVING) {
            int points=stack.hasTagCompound()?stack.getTagCompound().getInteger("srpkills"):0;
            event.getToolTip().add(TextFormatting.DARK_RED+"Parasite evolution: "+points+" / "+ArsenalCompatManager.getSrpEvolutionThreshold());
        }
        if (tier==WeaponTier.LIVING || tier==WeaponTier.SENTIENT) {
            boolean sentient=tier==WeaponTier.SENTIENT;
            String level=sentient?" II":" I";
            if (stack.getItem() instanceof ItemMorningStar)
                event.getToolTip().add(TextFormatting.DARK_GREEN+"Hits inflict Corrosion"+level+" for 5 secs.");
            else if (stack.getItem() instanceof ItemClaws)
                event.getToolTip().add(TextFormatting.DARK_RED+"Hits inflict Bleeding"+level+" for 5 secs.");
            else if (stack.getItem() instanceof ItemFlail)
                event.getToolTip().add(TextFormatting.DARK_PURPLE+"Hits inflict Immalleable"+level+" for 5 secs.");
            else if (stack.getItem() instanceof ItemBallAndChain)
                event.getToolTip().add(TextFormatting.DARK_PURPLE+"Ignores parasite adaptation; inflicts Debar"+level+".");
            else if (stack.getItem() instanceof ItemBatteringRam)
                event.getToolTip().add(TextFormatting.RED+"While held: Rage"+level+"; cleansed when unequipped.");
            else if (stack.getItem() instanceof ItemScimitar)
                event.getToolTip().add(TextFormatting.DARK_PURPLE+"Hits inflict Weakness "+(sentient?"III":"II")+" for 5 secs.");
            if (sentient) event.getToolTip().add(TextFormatting.DARK_GRAY
                +"Retains SRP's normal chance to mark its wielder as Prey.");
        }
    }
}
