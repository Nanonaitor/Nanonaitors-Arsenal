package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * Optional RLCombat support (mod id "rlcombat", the 1.20.1 dual-wield combat mod).
 * RLCombat makes right-click attack with whatever weapon is in the off hand. A lone
 * off-hand Scimitar also strikes on right-click in Arsenal, so with RLCombat installed
 * every right-click hit twice: Arsenal's strike, then RLCombat's. As in 1.12.2, where
 * Arsenal sent these strikes through RLCombat's attack, the strike is left to RLCombat:
 * it then uses RLCombat's off-hand cooldown, critical hits, sweeps and damage types, and
 * Arsenal's weapon effects still apply because RLCombat attacks with the scimitar itself.
 * Arsenal keeps the strike whenever it already claims the right-click (an Arsenal weapon
 * in the main hand, or a held charge such as a Morning Star), since RLCombat never sees
 * those clicks.
 */
public final class RLCombatCompat {
    private static Boolean loaded;

    public static boolean loaded(){
        if(loaded==null)loaded=ModList.get().isLoaded("rlcombat");
        return loaded;
    }

    /** True when RLCombat, not Arsenal, performs this player's lone off-hand Scimitar strike. */
    public static boolean ownsOffhandStrike(Player player){
        return loaded() && !player.isUsingItem() && !(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem);
    }
    private RLCombatCompat(){}
}
