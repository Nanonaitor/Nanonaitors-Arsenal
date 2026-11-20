package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.data.TransientData;
import com.nanonaitor.arsenal.item.ItemScimitar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Scimitar counter: blocking an attack while holding a scimitar (with the
 * dual-scimitar guard or any shield) charges the next scimitar strike. That
 * strike, or the shield bash with both blades' damage combined, deals
 * x1.3 (configurable). The charge fades after a few seconds unused.
 */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ScimitarCounter {
    private static final String CHARGED_UNTIL = "ArsenalScimitarCounterUntil";

    private ScimitarCounter() {}

    static boolean holdsScimitar(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() instanceof ItemScimitar
            || player.getHeldItemOffhand().getItem() instanceof ItemScimitar;
    }

    /** Server: an attack on this player was blocked. */
    public static void blocked(EntityPlayer player) {
        if (player.world.isRemote || !holdsScimitar(player)) return;
        long until = player.world.getTotalWorldTime() + ArsenalConfig.scimitars.counterWindowSeconds * 20L;
        boolean fresh = TransientData.of(player).getLong(CHARGED_UNTIL) < player.world.getTotalWorldTime();
        TransientData.of(player).setLong(CHARGED_UNTIL, until);
        if (fresh) {
            // A bright ring tells the player the counter is ready.
            player.world.playSound(null, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_ARMOR_EQUIP_IRON, player.getSoundCategory(), 0.8F, 1.7F);
        }
    }

    public static boolean isCharged(EntityPlayer player) {
        return TransientData.of(player).getLong(CHARGED_UNTIL) >= player.world.getTotalWorldTime();
    }

    /**
     * Blocks by anything other than the dual-scimitar guard (which reports
     * itself): vanilla and modded shields, and Arsenal's own guards (which cancel
     * the attack, hence receiveCanceled). Mirrors vanilla's shield test: an
     * active blocking item, a blockable source, from the front.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getAmount() <= 0.0F) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote || !holdsScimitar(player) || !player.isHandActive()) return;
        ItemStack active = player.getActiveItemStack();
        if (active.getItem() instanceof ItemScimitar) return; // the dual guard reports its own blocks
        boolean shield = active.getItem().isShield(active, player) || player.isActiveItemStackBlocking();
        DamageSource source = event.getSource();
        if (!shield || source.isUnblockable()) return;
        if (event.isCanceled() || fromFront(player, source)) blocked(player);
    }

    /** The next scimitar hit (main or off-hand) or scimitar shield bash after a block. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof EntityPlayer) || source.getImmediateSource() != source.getTrueSource()) return;
        EntityPlayer player = (EntityPlayer) source.getTrueSource();
        if (player.world.isRemote || !holdsScimitar(player) || !isCharged(player)) return;
        if (!countersDamageType(source.getDamageType())) return;
        TransientData.of(player).setLong(CHARGED_UNTIL, 0L);
        event.setAmount(counterDamage(event.getAmount(), ArsenalConfig.scimitars.counterMultiplier));
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).getServerWorld().spawnParticle(EnumParticleTypes.CRIT_MAGIC,
                event.getEntityLiving().posX, event.getEntityLiving().posY + event.getEntityLiving().height * 0.6D,
                event.getEntityLiving().posZ, 14, 0.3D, 0.3D, 0.3D, 0.2D);
        }
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, player.getSoundCategory(), 1.0F, 1.2F);
    }

    /** The counter empowers melee scimitar hits and the scimitar shield bash, nothing else. */
    public static boolean countersDamageType(String type) {
        return "player".equals(type) || "arsenal_scimitar_bash".equals(type);
    }

    /** The empowered damage. */
    public static float counterDamage(float amount, double multiplier) {
        return amount * (float) multiplier;
    }

    private static boolean fromFront(EntityPlayer player, DamageSource source) {
        Vec3d at = source.getDamageLocation();
        if (at == null) return false;
        Vec3d look = player.getLook(1.0F);
        Vec3d to = at.subtract(player.getPositionVector()).normalize();
        to = new Vec3d(to.x, 0.0D, to.z).normalize();
        return to.dotProduct(new Vec3d(look.x, 0.0D, look.z).normalize()) > 0.0D;
    }
}
