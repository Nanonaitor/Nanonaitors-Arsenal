package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Item-use fallback only: never preempts entity or block interaction. */
@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID)
public final class AbilityUseRules {
    public static boolean shield(EntityPlayer p,ItemStack s){return !s.isEmpty()&&s.getItem().isShield(s,p);}
    public static boolean cooling(EntityPlayer p,ItemStack s){return !s.isEmpty()&&p.getCooldownTracker().hasCooldown(s.getItem());}
    public static boolean activeShield(EntityPlayer p){return p.isHandActive()&&shield(p,p.getActiveItemStack());}
    public static boolean weaponSuppressed(EntityPlayer p) { return activeShield(p)
        || p.getEntityData().getLong("ArsenalShieldAttackLock") > p.world.getTotalWorldTime(); }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void use(PlayerInteractEvent.RightClickItem e){
        EntityPlayer p=e.getEntityPlayer();ItemStack s=p.getHeldItem(e.getHand());
        if(s.getItem() instanceof ItemArsenalWeapon && (cooling(p,s)
            || e.getHand()==EnumHand.MAIN_HAND&&shield(p,p.getHeldItemOffhand()))){
            e.setCanceled(true);e.setCancellationResult(EnumActionResult.PASS);
        }
        if(shield(p,s)){
            DoubleBladedScimitarCombat.cancelReflection(p);
            BallAndChainCombat.cancelAbility(p);
            if(p.isHandActive()&&p.getActiveItemStack().getItem() instanceof ItemArsenalWeapon)p.resetActiveHand();
        }
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void tick(TickEvent.PlayerTickEvent e){
        EntityPlayer p=e.player;
        if(e.phase!=TickEvent.Phase.START||!p.isHandActive())return;
        ItemStack s=p.getActiveItemStack();
        // Reflection intentionally starts its own cooldown while active.
        if((s.getItem() instanceof ItemArsenalShield||s.getItem() instanceof ItemArsenalWeapon)
            && !(s.getItem() instanceof ItemDoubleBladedScimitar)&&cooling(p,s))p.resetActiveHand();
    }
}
