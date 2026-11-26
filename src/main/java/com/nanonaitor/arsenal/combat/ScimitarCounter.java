package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;

/**
 * Scimitar counter: blocking an attack while holding a scimitar (the
 * dual-scimitar guard, or any shield) readies the next scimitar hit or
 * scimitar shield bash to deal extra damage (x1.3 by default). The bash
 * multiplies its combined dual-wield damage. The counter fades after a few
 * seconds unused. This replaces the old Weakness-on-hit ability.
 */
public final class ScimitarCounter {
    static final String CHARGED_UNTIL = "ArsenalScimitarCounterUntil";

    public static void register() {
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(ScimitarCounter::onShieldBlock);
        bus.addListener(net.minecraftforge.eventbus.api.EventPriority.LOW, ScimitarCounter::onHurt);
    }

    public static boolean holdsScimitar(Player player) {
        return scimitar(player.getMainHandItem()) || scimitar(player.getOffhandItem());
    }

    private static boolean scimitar(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == WeaponKind.SCIMITAR;
    }

    /** Server: an attack on this player was blocked. */
    public static void blocked(Player player) {
        if (player.level().isClientSide || !holdsScimitar(player)) return;
        long now = player.level().getGameTime();
        boolean fresh = !isCharged(player);
        player.getPersistentData().putLong(CHARGED_UNTIL, now + com.nanonaitor.arsenal.config.SyncedConfig.get("counterSeconds", ArsenalConfig.SCIMITAR_COUNTER_SECONDS) * 20L);
        // A bright ring tells the player the counter is ready.
        if (fresh) player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON,
            SoundSource.PLAYERS, 0.8F, 1.7F);
    }

    public static boolean isCharged(Player player) {
        return player.getPersistentData().getLong(CHARGED_UNTIL) >= player.level().getGameTime();
    }

    /** The empowered damage. */
    public static float counterDamage(float amount, double multiplier) {
        return amount * (float)multiplier;
    }

    /** Vanilla and modded shields that block through vanilla's shield check. */
    private static void onShieldBlock(ShieldBlockEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof Player player && event.getBlockedDamage() > 0.0F)
            blocked(player);
    }

    /**
     * The next melee hit with a scimitar (either hand) or the scimitar shield bash:
     * both reach here with the scimitar as the attacking stack.
     */
    private static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)
            || event.getSource().getDirectEntity() != player || player.level().isClientSide
            || !scimitar(TierEffects.attackingStack(player)) || !isCharged(player)
            || event.getAmount() <= 0.0F) return;
        player.getPersistentData().remove(CHARGED_UNTIL);
        event.setAmount(counterDamage(event.getAmount(), com.nanonaitor.arsenal.config.SyncedConfig.get("counterMultiplier", ArsenalConfig.SCIMITAR_COUNTER_MULTIPLIER)));
        if (player.level() instanceof ServerLevel level) {
            var target = event.getEntity();
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY(0.6D), target.getZ(),
                14, 0.3D, 0.3D, 0.3D, 0.2D);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    private ScimitarCounter() {}
}
