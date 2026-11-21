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
    /** The off-hand holds something that guards: any shield (isShield or a shield-class item). */
    public static boolean offhandGuard(EntityPlayer p){
        ItemStack off=p.getHeldItemOffhand();
        return shield(p,off)||(!off.isEmpty()&&off.getItem() instanceof net.minecraft.item.ItemShield);
    }
    public static boolean activeShield(EntityPlayer p){return p.isHandActive()&&shield(p,p.getActiveItemStack());}
    /** Defenders (the Defenders mod) are built to attack while guarding, so they never cancel weapon abilities. */
    public static boolean defender(ItemStack s){
        net.minecraft.util.ResourceLocation name=s.isEmpty()?null:s.getItem().getRegistryName();
        return name!=null&&"defenders".equals(name.getResourceDomain());
    }
    /** Guarding with a shield that stops weapon abilities: any shield except a Defender. */
    public static boolean activeConventionalShield(EntityPlayer p){return activeShield(p)&&!defender(p.getActiveItemStack());}
    /** weaponSuppressed, except while guarding with a Defender (built to attack while guarding). */
    public static boolean suppressedUnlessDefending(EntityPlayer p){
        if(activeShield(p))return !defender(p.getActiveItemStack());
        return weaponSuppressed(p);
    }
    private static final String ATTACK_LOCK = "ArsenalShieldAttackLock";
    public static boolean weaponSuppressed(EntityPlayer p) { return activeShield(p)
        || com.nanonaitor.arsenal.data.TransientData.of(p).getLong(ATTACK_LOCK) > p.world.getTotalWorldTime(); }
    /** Blocks Arsenal weapon abilities for the given ticks after a shield action takes over. */
    public static void lockWeaponAttacks(EntityPlayer p, int ticks) {
        com.nanonaitor.arsenal.data.TransientData.of(p).setLong(ATTACK_LOCK, p.world.getTotalWorldTime() + ticks);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void use(PlayerInteractEvent.RightClickItem e){
        EntityPlayer p=e.getEntityPlayer();ItemStack s=p.getHeldItem(e.getHand());
        // Any shield-type item in the off-hand (vanilla, Arsenal's Tartsy/Bulwark, modded)
        // gets the right-click. Cancelling here also stops the server from resyncing
        // the inventory over a declined weapon use, which used to drop the shield again.
        if(s.getItem() instanceof ItemArsenalWeapon && (cooling(p,s)
            || e.getHand()==EnumHand.MAIN_HAND&&offhandGuard(p))){
            e.setCanceled(true);e.setCancellationResult(EnumActionResult.PASS);
        }
        if(shield(p,s)){
            DoubleBladedScimitarCombat.cancelReflection(p);
            // Raising a Defender keeps a Ball and Chain wind-up or throw going.
            if(!defender(s))BallAndChainCombat.cancelAbility(p);
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
