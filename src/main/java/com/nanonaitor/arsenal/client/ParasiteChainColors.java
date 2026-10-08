package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.WeaponTier;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Tint only animated tether links; leave the head and inventory artwork intact. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class ParasiteChainColors {
    private ParasiteChainColors() {}

    public static int colorForPart(int part) {
        return part == WeaponPartRenderer.CHAIN_LINK_FLAT
            || part == WeaponPartRenderer.CHAIN_LINK_UPRIGHT ? 0xFFCC3030 : -1;
    }

    @SubscribeEvent
    public static void register(ColorHandlerEvent.Item event) {
        for (Item item : ForgeRegistries.ITEMS.getValuesCollection()) {
            if (!(item instanceof ItemFlail) && !(item instanceof ItemBallAndChain)) continue;
            WeaponTier tier = ((ItemArsenalWeapon) item).getTier();
            if (tier != WeaponTier.LIVING && tier != WeaponTier.SENTIENT) continue;
            event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
                int part = stack.hasTagCompound()
                    ? stack.getTagCompound().getInteger(WeaponPartRenderer.PART_TAG) : 0;
                return colorForPart(part);
            }, item);
        }
    }
}
