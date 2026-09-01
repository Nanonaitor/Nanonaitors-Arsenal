package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemDoubleBladedScimitar;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class DoubleBladedScimitarCombat {
    public static final int REFLECTION_TICKS = 20;
    public static final int NORMAL_COOLDOWN_TICKS = 60;
    public static final int SUCCESS_COOLDOWN_TICKS = 10;
    private static final String ACTIVE = "ArsenalDoubleBladeReflecting";
    private static final UUID OCCUPIED_OFFHAND_SPEED = UUID.fromString(
        "a14d7f20-5d8d-4eef-9b81-c24ce5d48c77");
    private static final AttributeModifier HALF_SPEED = new AttributeModifier(
        OCCUPIED_OFFHAND_SPEED, "Double blade occupied offhand penalty", -0.5D, 2)
        .setSaved(false);
    private static final Map<EntityPlayer, Long> REFLECTION_END = new WeakHashMap<>();
    private static final Map<EntityPlayer, Map<Potion, PotionEffect>> REFLECTION_EFFECT_BASELINE =
        new WeakHashMap<>();
    private static final Map<EntityPlayer, PendingEffectReflection> PENDING_EFFECT_REFLECTION =
        new WeakHashMap<>();
    private static final ThreadLocal<Boolean> RETURNING = new ThreadLocal<Boolean>() {
        @Override protected Boolean initialValue() { return Boolean.FALSE; }
    };
    private static final ThreadLocal<Boolean> AOE_DAMAGE = new ThreadLocal<Boolean>() {
        @Override protected Boolean initialValue() { return Boolean.FALSE; }
    };

    private DoubleBladedScimitarCombat() {}

    public static boolean beginReflection(EntityPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemDoubleBladedScimitar)
            || stack != player.getHeldItemMainhand()
            || !player.getHeldItemOffhand().isEmpty()
            || player.getCooldownTracker().hasCooldown(stack.getItem())) return false;
        long end = player.world.getTotalWorldTime() + REFLECTION_TICKS;
        REFLECTION_END.put(player, end);
        REFLECTION_EFFECT_BASELINE.put(player, harmfulEffects(player));
        player.getEntityData().setBoolean(ACTIVE, true);
        player.setActiveHand(EnumHand.MAIN_HAND);
        player.getCooldownTracker().setCooldown(stack.getItem(), NORMAL_COOLDOWN_TICKS);
        player.world.playSound(player.world.isRemote ? player : null,
            player.posX, player.posY, player.posZ, SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            SoundCategory.PLAYERS, 0.65F, 1.35F);
        return true;
    }

    public static boolean isReflecting(EntityPlayer player) {
        if (player == null || !(player.getHeldItemMainhand().getItem()
            instanceof ItemDoubleBladedScimitar) || !player.getHeldItemOffhand().isEmpty()) {
            return false;
        }
        Long end = REFLECTION_END.get(player);
        if (end != null) return player.world.getTotalWorldTime() < end;
        // Active-hand state is synchronized by vanilla and covers remote clients.
        return player.isHandActive() && player.getActiveHand() == EnumHand.MAIN_HAND
            && player.getActiveItemStack().getItem() instanceof ItemDoubleBladedScimitar;
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (!player.world.isRemote) updateReflectedEffects(player);
        ItemStack main = player.getHeldItemMainhand();
        boolean doubleBlade = main.getItem() instanceof ItemDoubleBladedScimitar;
        updateOffhandPenalty(player, doubleBlade && !player.getHeldItemOffhand().isEmpty());
        Long end = REFLECTION_END.get(player);
        if (end == null) return;
        boolean expired = player.world.getTotalWorldTime() >= end || player.isDead
            || !doubleBlade || !player.getHeldItemOffhand().isEmpty();
        if (expired) {
            REFLECTION_END.remove(player);
            REFLECTION_EFFECT_BASELINE.remove(player);
            player.getEntityData().removeTag(ACTIVE);
            if (player.isHandActive() && player.getActiveItemStack() == main) {
                player.resetActiveHand();
            }
        } else {
            player.getEntityData().setBoolean(ACTIVE, true);
            if (!player.isHandActive()) player.setActiveHand(EnumHand.MAIN_HAND);
            if (!PENDING_EFFECT_REFLECTION.containsKey(player)) {
                REFLECTION_EFFECT_BASELINE.put(player, harmfulEffects(player));
            }
        }
    }

    private static void updateOffhandPenalty(EntityPlayer player, boolean apply) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        AttributeModifier present = speed.getModifier(OCCUPIED_OFFHAND_SPEED);
        if (apply && present == null) speed.applyModifier(HALF_SPEED);
        else if (!apply && present != null) speed.removeModifier(present);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void reflect(LivingAttackEvent event) {
        if (RETURNING.get() || !(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer defender = (EntityPlayer) event.getEntityLiving();
        if (!isReflecting(defender)) return;
        event.setCanceled(true);
        Entity source = event.getSource().getTrueSource();
        if (source == null || source == defender || event.getAmount() <= 0.0F) return;
        if (!defender.world.isRemote && source instanceof EntityLivingBase) {
            PendingEffectReflection pending = new PendingEffectReflection(
                (EntityLivingBase) source,
                REFLECTION_EFFECT_BASELINE.containsKey(defender)
                    ? REFLECTION_EFFECT_BASELINE.get(defender) : harmfulEffects(defender),
                defender.world.getTotalWorldTime() + 2L);
            PENDING_EFFECT_REFLECTION.put(defender, pending);
            transferReflectedEffects(defender, pending);
        }
        boolean hit;
        RETURNING.set(Boolean.TRUE);
        try {
            hit = source.attackEntityFrom(new EntityDamageSource(
                "double_bladed_scimitar_reflect", defender), event.getAmount());
        } finally {
            RETURNING.set(Boolean.FALSE);
        }
        if (hit) {
            if (!isDirectMelee(event.getSource()) && source instanceof EntityLivingBase
                && ModContent.STUNNED != null) {
                ((EntityLivingBase) source).addPotionEffect(
                    new PotionEffect(ModContent.STUNNED, 20, 0, false, true));
            }
            ItemStack weapon = defender.getHeldItemMainhand();
            defender.getCooldownTracker().removeCooldown(weapon.getItem());
            defender.getCooldownTracker().setCooldown(weapon.getItem(), SUCCESS_COOLDOWN_TICKS);
            REFLECTION_EFFECT_BASELINE.remove(defender);
            defender.world.playSound(null, defender.posX, defender.posY, defender.posZ,
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS,
                0.8F, 1.25F);
        }
    }

    /** Normal Blade Staff melee hits splash their final melee damage around the struck target. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void bladeStaffMeleeAoe(LivingHurtEvent event) {
        if (event.isCanceled() || AOE_DAMAGE.get() || event.getAmount() <= 0.0F) return;
        DamageSource damageSource = event.getSource();
        if (!(damageSource.getTrueSource() instanceof EntityPlayer)
            || !isDirectMelee(damageSource)) return;
        EntityPlayer attacker = (EntityPlayer) damageSource.getTrueSource();
        if (!(attacker.getHeldItemMainhand().getItem() instanceof ItemDoubleBladedScimitar)
            || attacker.world.isRemote) return;
        EntityLivingBase primary = event.getEntityLiving();
        ItemDoubleBladedScimitar bladeStaff =
            (ItemDoubleBladedScimitar) attacker.getHeldItemMainhand().getItem();
        double radius = bladeStaff.getTier() == WeaponTier.SENTIENT ? 3.0D : 2.0D;
        AOE_DAMAGE.set(Boolean.TRUE);
        try {
            for (EntityLivingBase nearby : attacker.world.getEntitiesWithinAABB(
                    EntityLivingBase.class, primary.getEntityBoundingBox().grow(radius),
                    entity -> entity != attacker && entity != primary && entity.isEntityAlive()
                        && entity.getDistanceSq(primary) <= radius * radius
                        && (!(entity instanceof EntityPlayer)
                            || attacker.canAttackPlayer((EntityPlayer) entity)))) {
                nearby.attackEntityFrom(DamageSource.causePlayerDamage(attacker), event.getAmount());
            }
        } finally {
            AOE_DAMAGE.set(Boolean.FALSE);
        }
    }

    private static boolean isDirectMelee(DamageSource source) {
        Entity direct = source.getImmediateSource();
        return direct instanceof EntityLivingBase && direct == source.getTrueSource()
            && !source.isProjectile() && !source.isMagicDamage() && !source.isExplosion();
    }

    private static Map<Potion, PotionEffect> harmfulEffects(EntityLivingBase entity) {
        Map<Potion, PotionEffect> effects = new HashMap<>();
        for (PotionEffect effect : entity.getActivePotionEffects()) {
            if (effect.getPotion().isBadEffect()) {
                effects.put(effect.getPotion(), new PotionEffect(effect));
            }
        }
        return effects;
    }

    private static void updateReflectedEffects(EntityPlayer defender) {
        PendingEffectReflection pending = PENDING_EFFECT_REFLECTION.get(defender);
        if (pending == null) return;
        transferReflectedEffects(defender, pending);
        if (defender.world.getTotalWorldTime() >= pending.expiresAt
            || !pending.attacker.isEntityAlive()) {
            PENDING_EFFECT_REFLECTION.remove(defender);
        }
    }

    private static void transferReflectedEffects(EntityPlayer defender,
                                                   PendingEffectReflection pending) {
        Map<Potion, PotionEffect> current = harmfulEffects(defender);
        for (Map.Entry<Potion, PotionEffect> entry : current.entrySet()) {
            PotionEffect before = pending.baseline.get(entry.getKey());
            PotionEffect after = entry.getValue();
            boolean introduced = before == null || after.getAmplifier() > before.getAmplifier()
                || after.getDuration() > before.getDuration() + 1;
            if (!introduced) continue;
            defender.removePotionEffect(entry.getKey());
            if (before != null) defender.addPotionEffect(new PotionEffect(before));
            pending.attacker.addPotionEffect(new PotionEffect(after));
        }
    }

    private static final class PendingEffectReflection {
        private final EntityLivingBase attacker;
        private final Map<Potion, PotionEffect> baseline;
        private final long expiresAt;

        private PendingEffectReflection(EntityLivingBase attacker,
                                        Map<Potion, PotionEffect> baseline, long expiresAt) {
            this.attacker = attacker;
            this.baseline = new HashMap<>(baseline);
            this.expiresAt = expiresAt;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void stopEntityAttack(AttackEntityEvent event) {
        if (isReflecting(event.getEntityPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void stopBlockAttack(PlayerInteractEvent.LeftClickBlock event) {
        if (isReflecting(event.getEntityPlayer())) event.setCanceled(true);
    }
}
