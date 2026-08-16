package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalWeapon;
import com.nanonaitor.arsenal.item.WeaponTier;
import java.util.UUID;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Bridges Arsenal Silver weapons into RLCraft's Set Bonus Silver rules. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class SilverSetBonusCompat {
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString(
        "563b78c5-187b-42ec-8698-28d8e906d70a");
    private static final double SILVER_ATTACK_SPEED_BONUS = 0.50D;

    private SilverSetBonusCompat() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        IAttributeInstance speed = player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_SPEED);
        if (speed == null) return;

        AttributeModifier old = speed.getModifier(ATTACK_SPEED_UUID);
        if (old != null) speed.removeModifier(old);
        if (Loader.isModLoaded("setbonus") && holdsArsenalSilverWeapon(player)
            && wearsFullSilverArmor(player)) {
            // SetBonus's RLCraft rule is generic.attackSpeed=0.5 @ 1:
            // +50% of the base attack-speed attribute.
            speed.applyModifier(new AttributeModifier(ATTACK_SPEED_UUID,
                "Arsenal Silver Set Bonus compatibility",
                SILVER_ATTACK_SPEED_BONUS, 1).setSaved(false));
        }
    }

    private static boolean holdsArsenalSilverWeapon(EntityPlayer player) {
        ItemStack held = player.getHeldItemMainhand();
        return held.getItem() instanceof ItemArsenalWeapon
            && ((ItemArsenalWeapon) held.getItem()).getTier() == WeaponTier.SILVER;
    }

    private static boolean wearsFullSilverArmor(EntityPlayer player) {
        return hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD),
                "iceandfire:armor_silver_metal_helmet")
            && hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.CHEST),
                "iceandfire:armor_silver_metal_chestplate")
            && hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.LEGS),
                "iceandfire:armor_silver_metal_leggings")
            && hasRegistryName(player.getItemStackFromSlot(EntityEquipmentSlot.FEET),
                "iceandfire:armor_silver_metal_boots");
    }

    private static boolean hasRegistryName(ItemStack stack, String expected) {
        ResourceLocation name = stack.isEmpty() ? null : stack.getItem().getRegistryName();
        return name != null && expected.equals(name.toString());
    }
}
