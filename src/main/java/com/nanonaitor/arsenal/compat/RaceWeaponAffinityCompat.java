package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.ItemBatteringRam;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import com.nanonaitor.arsenal.item.ItemFlail;
import com.nanonaitor.arsenal.item.ItemMorningStar;
import com.nanonaitor.arsenal.item.ItemScimitar;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Optional native equivalent of Dregora's raceWeaponAffinity.zs rules. */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class RaceWeaponAffinityCompat {
    private static final ThreadLocal<ItemStack> FORCED_ATTACK_STACK = new ThreadLocal<>();

    private RaceWeaponAffinityCompat() {}

    public static boolean isEnabled() {
        return ArsenalConfig.raceWeaponAffinity.enabled && Loader.isModLoaded("xat");
    }

    /** Lets an off-hand/custom attack identify its actual weapon during the synchronous hurt event. */
    public static void beginAttack(ItemStack stack) {
        FORCED_ATTACK_STACK.set(stack);
    }

    public static void endAttack() {
        FORCED_ATTACK_STACK.remove();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void applyAffinityDamage(LivingHurtEvent event) {
        if (!isEnabled()) return;
        EntityPlayer player = directPlayer(event.getSource());
        if (player == null) return;
        ItemStack stack = FORCED_ATTACK_STACK.get();
        if (stack == null || stack.isEmpty()) stack = player.getHeldItemMainhand();
        String affinity = affinityRace(stack);
        if (affinity == null || !ArsenalCompatManager.isXatRace(player, affinity)) return;

        float original = event.getAmount();
        float multiplied = (float) (original * ArsenalConfig.raceWeaponAffinity.damageMultiplier);
        float minimum = (float) (original + ArsenalConfig.raceWeaponAffinity.minimumBonusDamage);
        event.setAmount(Math.max(multiplied, minimum));
    }

    public static String affinityRace(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : affinityRace(stack.getItem());
    }

    public static String affinityRace(Item item) {
        if (item instanceof ItemScimitar) return "Fairy";
        if (item instanceof ItemFlail) return "Goblin";
        if (item instanceof ItemClaws) return "Faelis";
        if (item instanceof ItemMorningStar) return "Dwarf";
        if (item instanceof ItemSunWarBulwark) return "Dragon";
        if (item instanceof ItemBallAndChain) return "Titan";
        if (item instanceof ItemBatteringRam) return "Taurus";
        if (item instanceof ItemDoubleBladedScimitar) return "Elf";
        return null;
    }

    public static int configuredPercent() {
        return (int) Math.round((ArsenalConfig.raceWeaponAffinity.damageMultiplier - 1.0D) * 100.0D);
    }

    private static EntityPlayer directPlayer(DamageSource source) {
        Entity trueSource = source.getTrueSource();
        Entity immediate = source.getImmediateSource();
        return trueSource instanceof EntityPlayer && trueSource == immediate
            ? (EntityPlayer) trueSource : null;
    }
}
