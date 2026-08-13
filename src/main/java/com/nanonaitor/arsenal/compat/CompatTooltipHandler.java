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
}
