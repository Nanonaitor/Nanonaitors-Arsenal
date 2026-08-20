package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.compat.ReskillableCompat;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.network.BallAndChainReleaseAnimationMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModContent;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.Potion;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class BallAndChainCombat {
    public static final int SWING_INTERVAL_TICKS = 25;
    public static final int RELEASE_ANIMATION_TICKS = 16;
    public static final double WINDUP_REACH = 3.0D;
    public static final double THROW_REACH_PER_CHARGE = 4.0D;
    private static final double LINE_RADIUS = 0.55D;
    private static final float[] RELEASE_DAMAGE_MULTIPLIER = {0.0F, 1.25F, 1.75F, 2.25F};
    private static final Map<EntityPlayer, SwingState> SWINGS = new WeakHashMap<>();
    private static final Map<EntityPlayer, ThrowState> THROWS = new WeakHashMap<>();

    private BallAndChainCombat() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void cancelVanillaAttack(AttackEntityEvent event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventBlockBreaking(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntityPlayer().getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
            event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        }
    }

    public static void updateSwinging(EntityPlayerMP player, boolean swinging) {
        if (!swinging) {
            release(player);
            return;
        }
        if (THROWS.containsKey(player)) {
            return;
        }
        ItemStack stack = player.getHeldItemMainhand();
        if (!isValidWielder(player, stack)) {
            SWINGS.remove(player);
            return;
        }
        long now = player.world.getTotalWorldTime();
        SwingState state = SWINGS.get(player);
        if (state == null) {
            state = new SwingState(now);
            SWINGS.put(player, state);
        }
        state.lastHeartbeatTick = now;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote
            || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (updateThrow(player)) {
            return;
        }
        SwingState state = SWINGS.get(player);
        if (state == null) {
            return;
        }
        ItemStack weapon = player.getHeldItemMainhand();
        if (!isValidWielder(player, weapon)) {
            SWINGS.remove(player);
            player.resetActiveHand();
            return;
        }
        long now = player.world.getTotalWorldTime();
        if (now - state.lastHeartbeatTick > 3L) {
            release(player);
            return;
        }
        if (!player.isHandActive()) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        if (now >= state.nextSwingTick) {
            state.nextSwingTick = now
                + ChainWeaponStats.swingIntervalTicks(player, weapon);
            int maxCharges = maxCharges(((ItemBallAndChain) weapon.getItem()).getTier());
            int previousCharge = state.charge;
            state.charge = Math.min(maxCharges, state.charge + 1);
            performWindupSweep(player, weapon, state.charge);
            if (previousCharge < maxCharges && state.charge == maxCharges) {
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(),
                    0.55F, 1.55F);
            }
        }
    }

    private static boolean updateThrow(EntityPlayerMP player) {
        ThrowState state = THROWS.get(player);
        if (state == null) {
            return false;
        }
        ItemStack current = player.getHeldItemMainhand();
        if (current != state.weapon || !(current.getItem() instanceof ItemBallAndChain)
            || ((ItemBallAndChain) current.getItem()).getTier() != state.tier
            || !ArsenalCompatManager.canUseTwoHanded(player)) {
            THROWS.remove(player);
            if (player.isHandActive()) player.resetActiveHand();
            return false;
        }
        long age = player.world.getTotalWorldTime() - state.startTick;
        if (!state.returnHitDone && age >= state.durationTicks / 2) {
            state.returnHitDone = true;
            performThrowPass(player, state, true);
        }
        if (age >= state.durationTicks || player.isDead) {
            THROWS.remove(player);
            if (player.isHandActive()) {
                player.resetActiveHand();
            }
            return false;
        }
        if (!player.isHandActive() && player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        return true;
    }

    private static boolean isValidWielder(EntityPlayerMP player, ItemStack stack) {
        return stack.getItem() instanceof ItemBallAndChain
            && ArsenalCompatManager.canUseTwoHanded(player) && player.isEntityAlive()
            && ReskillableCompat.canUse(player, stack)
            && !player.isSpectator();
    }

    private static void performWindupSweep(EntityPlayerMP player, ItemStack weapon,
                                           int charge) {
        Vec3d start = new Vec3d(player.posX, player.posY + player.height * 0.5D,
            player.posZ);
        Vec3d forward = horizontalLook(player);
        double reach = ChainWeaponStats.ballWindupReach(player, weapon);
        Vec3d end = stopAtSolidBlock(player, start,
            start.add(forward.scale(reach)));
        AxisAlignedBB search = sweptBox(start, end, 0.7D, 1.0D);
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class, search,
            target -> isValidTarget(player, target));
        targets.sort(Comparator.comparingDouble(target -> target.getDistanceSq(start.x,
            start.y, start.z)));
        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        boolean hit = false;
        for (EntityLivingBase target : targets) {
            if (intersectsLine(target, start, end, 0.7D, 1.0D)) {
                float damage = baseDamage * 0.5F + EnchantmentHelper.getModifierForCreature(
                    weapon, target.getCreatureAttribute());
                hit |= applyHit(player, target, weapon, damage, 0.3F,
                    forward, false);
            }
        }
        playAttackSound(player, hit, 0.82F + charge * 0.07F);
    }

    private static void release(EntityPlayerMP player) {
        SwingState swing = SWINGS.remove(player);
        ItemStack weapon = player.getHeldItemMainhand();
        if (swing == null || THROWS.containsKey(player)) {
            return;
        }
        if (!isValidWielder(player, weapon) || swing.charge <= 0) {
            player.resetActiveHand();
            return;
        }
        ItemBallAndChain item = (ItemBallAndChain) weapon.getItem();
        int charge = Math.min(maxCharges(item.getTier()), swing.charge);
        int effectiveCharge = item.getTier() == WeaponTier.GOLD && charge >= 2 ? 3 : charge;
        Vec3d start = new Vec3d(player.posX, player.posY + player.getEyeHeight() * 0.75D,
            player.posZ);
        Vec3d direction = player.getLookVec().normalize();
        double throwReach = ChainWeaponStats.ballThrowReach(player, weapon,
            effectiveCharge);
        Vec3d intendedEnd = start.add(direction.scale(throwReach));
        Vec3d end = stopAtSolidBlock(player, start, intendedEnd);
        float multiplier = RELEASE_DAMAGE_MULTIPLIER[effectiveCharge];
        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * multiplier;
        int durationTicks = ChainWeaponStats.ballReleaseAnimationTicks(player, weapon);
        ThrowState state = new ThrowState(player.world.getTotalWorldTime(),
            durationTicks, weapon, item.getTier(), effectiveCharge, start, end,
            direction, baseDamage);
        THROWS.put(player, state);
        player.setActiveHand(EnumHand.MAIN_HAND);
        sendReleaseAnimation(player, effectiveCharge, (float) start.distanceTo(end),
            durationTicks);
        performThrowPass(player, state, false);
        if (end.distanceTo(intendedEnd) > 0.05D) {
            player.world.playSound(null, end.x, end.y, end.z,
                SoundEvents.ENTITY_IRONGOLEM_ATTACK, player.getSoundCategory(),
                0.8F, 0.75F + effectiveCharge * 0.08F);
        }
    }

    private static void performThrowPass(EntityPlayerMP player, ThrowState state,
                                         boolean returning) {
        Vec3d start = returning ? state.end : state.start;
        Vec3d end = returning ? state.start : state.end;
        Vec3d direction = returning ? state.direction.scale(-1.0D) : state.direction;
        AxisAlignedBB search = sweptBox(start, end, LINE_RADIUS, LINE_RADIUS);
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(
            EntityLivingBase.class, search,
            target -> isValidTarget(player, target));
        targets.sort(Comparator.comparingDouble(target -> target.getDistanceSq(start.x,
            start.y, start.z)));
        boolean hit = false;
        for (EntityLivingBase target : targets) {
            if (!intersectsLine(target, start, end, LINE_RADIUS, LINE_RADIUS)) {
                continue;
            }
            float damage = state.baseDamage + EnchantmentHelper.getModifierForCreature(
                state.weapon, target.getCreatureAttribute());
            if (isFullCharge(state)) {
                damage = compensateForArmor(target, damage,
                    getArmorPiercing(state.tier));
            }
            float knockback = 0.75F + state.charge * 0.55F;
            if (applyHit(player, target, state.weapon, damage, knockback,
                direction, true)) {
                hit = true;
                if (!returning && isFullCharge(state)) {
                    fractureArmor(target, state.tier);
                }
            }
        }
        playAttackSound(player, hit,
            (returning ? 0.66F : 0.72F) + state.charge * 0.10F);
    }

    private static boolean applyHit(EntityPlayerMP player, EntityLivingBase target,
                                    ItemStack weapon, float damage, float knockback,
                                    Vec3d direction, boolean ignoreCurrentIFrames) {
        int previousResistance = target.hurtResistantTime;
        if (ignoreCurrentIFrames) {
            target.hurtResistantTime = 0;
        }
        boolean bypassAdaptation = weapon.getItem() instanceof ItemBallAndChain
            && ((((ItemBallAndChain) weapon.getItem()).getTier() == WeaponTier.LIVING)
                || (((ItemBallAndChain) weapon.getItem()).getTier() == WeaponTier.SENTIENT))
            && ArsenalCompatManager.isSrpParasite(target);
        Potion antimall = bypassAdaptation ? ArsenalCompatManager.potion("antimall") : null;
        PotionEffect previousAntimall = antimall == null ? null
            : target.getActivePotionEffect(antimall);
        if (antimall != null && previousAntimall == null) {
            target.addPotionEffect(new PotionEffect(antimall, 2, 0, false, false));
        }
        boolean damaged = target.attackEntityFrom(
            DamageSource.causePlayerDamage(player), damage);
        if (antimall != null && previousAntimall == null) {
            target.removePotionEffect(antimall);
        }
        if (!damaged) {
            if (ignoreCurrentIFrames) {
                target.hurtResistantTime = previousResistance;
            }
            return false;
        }
        int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
        if (fireAspect > 0) {
            target.setFire(fireAspect * 4);
        }
        float totalKnockback = knockback
            + EnchantmentHelper.getKnockbackModifier(player) * 0.35F;
        if (totalKnockback > 0.0F) {
            target.knockBack(player, totalKnockback, -direction.x, -direction.z);
            target.motionY += direction.y * totalKnockback * 0.35D;
            target.velocityChanged = true;
        }
        EnchantmentHelper.applyThornEnchantments(target, player);
        EnchantmentHelper.applyArthropodEnchantments(player, target);
        weapon.damageItem(1, player);
        player.addExhaustion(0.1F);
        return true;
    }

    private static boolean isValidTarget(EntityPlayerMP player,
                                         EntityLivingBase target) {
        return target != player && !target.isDead && !player.isOnSameTeam(target);
    }

    private static Vec3d horizontalLook(EntityPlayer player) {
        double yaw = Math.toRadians(player.rotationYaw);
        return new Vec3d(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private static Vec3d stopAtSolidBlock(EntityPlayerMP player, Vec3d start,
                                          Vec3d intendedEnd) {
        Vec3d travel = intendedEnd.subtract(start);
        double length = travel.lengthVector();
        if (length <= 0.0001D) return intendedEnd;
        Vec3d direction = travel.scale(1.0D / length);
        Vec3d side = new Vec3d(-direction.z, 0.0D, direction.x);
        if (side.lengthSquared() > 0.0001D) side = side.normalize().scale(0.35D);
        Vec3d[] offsets = {
            new Vec3d(0.0D, 0.0D, 0.0D),
            new Vec3d(0.0D, -0.45D, 0.0D),
            new Vec3d(0.0D, 0.35D, 0.0D),
            side,
            side.scale(-1.0D)
        };
        double closest = length;
        for (Vec3d offset : offsets) {
            Vec3d rayStart = start.add(offset);
            RayTraceResult hit = player.world.rayTraceBlocks(rayStart,
                intendedEnd.add(offset), false, true, false);
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                closest = Math.min(closest, rayStart.distanceTo(hit.hitVec));
            }
        }
        return closest < length ? start.add(direction.scale(closest)) : intendedEnd;
    }

    private static AxisAlignedBB sweptBox(Vec3d start, Vec3d end,
                                          double horizontal, double vertical) {
        return new AxisAlignedBB(start.x, start.y, start.z, end.x, end.y, end.z)
            .grow(horizontal, vertical, horizontal);
    }

    private static boolean intersectsLine(EntityLivingBase target, Vec3d start,
                                          Vec3d end, double horizontal,
                                          double vertical) {
        AxisAlignedBB box = target.getEntityBoundingBox().grow(
            horizontal, vertical, horizontal);
        return box.contains(start) || box.contains(end)
            || box.calculateIntercept(start, end) != null;
    }

    private static void sendReleaseAnimation(EntityPlayerMP player, int charge,
                                             float distance, int durationTicks) {
        BallAndChainReleaseAnimationMessage message =
            new BallAndChainReleaseAnimationMessage(player.getEntityId(), charge,
                distance, player.rotationYaw, player.rotationPitch, durationTicks);
        ModNetwork.CHANNEL.sendToAllAround(message, new NetworkRegistry.TargetPoint(
            player.dimension, player.posX, player.posY, player.posZ, 64.0D));
    }

    private static void playAttackSound(EntityPlayerMP player, boolean hit, float pitch) {
        SoundEvent sound = hit ? SoundEvents.ENTITY_PLAYER_ATTACK_STRONG
            : SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE;
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            sound, player.getSoundCategory(), hit ? 1.0F : 0.65F, pitch);
    }

    private static float getArmorPiercing(WeaponTier tier) {
        return tier.getArmorPiercePercent() / 100.0F;
    }

    private static int maxCharges(WeaponTier tier) {
        return tier == WeaponTier.GOLD ? 2 : 3;
    }

    private static boolean isFullCharge(ThrowState state) {
        return state.charge >= 3;
    }

    private static float compensateForArmor(EntityLivingBase target, float damage,
                                             float piercing) {
        float armor = target.getTotalArmorValue();
        float toughness = (float) target.getEntityAttribute(
            SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
        float armoredDamage = CombatRules.getDamageAfterAbsorb(damage, armor, toughness);
        float desiredDamage = armoredDamage + (damage - armoredDamage) * piercing;
        if (desiredDamage <= armoredDamage + 0.001F) {
            return damage;
        }
        float low = damage;
        float high = Math.max(damage * 10.0F, damage + armor * 5.0F + 10.0F);
        for (int iteration = 0; iteration < 16; iteration++) {
            float middle = (low + high) * 0.5F;
            if (CombatRules.getDamageAfterAbsorb(middle, armor, toughness)
                < desiredDamage) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return high;
    }

    private static void fractureArmor(EntityLivingBase target, WeaponTier tier) {
        if (target instanceof EntityPlayer) {
            if (ModContent.ARMOR_FRACTURE == null) {
                return;
            }
            int piercing = tier.getArmorPiercePercent();
            int amplifier = piercing >= 100 ? 4 : piercing >= 75 ? 2
                : piercing >= 50 ? 1 : 0;
            target.addPotionEffect(new PotionEffect(ModContent.ARMOR_FRACTURE,
                10 * 20, amplifier, false, true));
            return;
        }
        IAttributeInstance armor = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        if (armor == null) {
            return;
        }
        double currentArmor = Math.max(0.0D, armor.getAttributeValue());
        double reduction = Math.min(currentArmor,
            Math.max(2.0D, currentArmor * 0.10D));
        if (reduction > 0.0D) {
            armor.applyModifier(new AttributeModifier(UUID.randomUUID(),
                "Ball and Chain permanent armor fracture", -reduction, 0)
                .setSaved(true));
        }
    }

    private static final class SwingState {
        private long lastHeartbeatTick;
        private long nextSwingTick;
        private int charge;

        private SwingState(long startTick) {
            this.lastHeartbeatTick = startTick;
            this.nextSwingTick = startTick;
        }
    }

    private static final class ThrowState {
        private final long startTick;
        private final int durationTicks;
        private final ItemStack weapon;
        private final WeaponTier tier;
        private final int charge;
        private final Vec3d start;
        private final Vec3d end;
        private final Vec3d direction;
        private final float baseDamage;
        private boolean returnHitDone;

        private ThrowState(long startTick, int durationTicks, ItemStack weapon, WeaponTier tier,
                           int charge, Vec3d start, Vec3d end, Vec3d direction,
                           float baseDamage) {
            this.startTick = startTick;
            this.durationTicks = durationTicks;
            this.weapon = weapon;
            this.tier = tier;
            this.charge = charge;
            this.start = start;
            this.end = end;
            this.direction = direction;
            this.baseDamage = baseDamage;
        }
    }
}
