package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.config.ArsenalConfig;
import com.nanonaitor.arsenal.registry.ModEffects;
import com.nanonaitor.arsenal.registry.ModItems;
import com.nanonaitor.arsenal.enchantment.ModEnchantments;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public final class CombatEvents {
    private static void replaceTransient(net.minecraft.world.entity.ai.attributes.AttributeInstance a, AttributeModifier m) { a.removeModifier(m.getId()); a.addTransientModifier(m); }
    private static void replacePermanent(net.minecraft.world.entity.ai.attributes.AttributeInstance a, AttributeModifier m) { a.removeModifier(m.getId()); a.addPermanentModifier(m); }
    private static final float BULWARK_BASE_DAMAGE = 1.0F;
    private static final float BULWARK_ARMOR_DAMAGE = 1.0F;
    private static final java.util.UUID BULWARK_SLOW = java.util.UUID.nameUUIDFromBytes("nanonaitors_arsenal:bulwark_slow".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final java.util.UUID OCCUPIED_HAND_ATTACK_SLOW = java.util.UUID.fromString("d5b337d2-b6a9-4b99-96d3-3649ecf801a1");
    private static final java.util.UUID DUAL_SCIMITAR_ATTACK_SPEED = java.util.UUID.fromString("ffef2724-a45e-4529-8bca-c417a35ab4cc");
    private static final TagKey<Block> RAM_WOOD = ramTag("wood");
    private static final TagKey<Block> RAM_STONE = ramTag("stone");
    private static final TagKey<Block> RAM_IRON = ramTag("iron");
    private static final TagKey<Block> RAM_DIAMOND = ramTag("diamond");
    private static final Map<UUID, Long> LAST_FLAIL = new HashMap<>();
    private static final Map<UUID, ItemStack> ACTIVE_FLAIL_SPRITES = new HashMap<>();
    private static final Map<UUID, BallState> BALLS = new HashMap<>();
    private static final Map<UUID, RamState> RAMS = new HashMap<>();
    private static final Map<UUID, MorningStarState> MORNING_STARS = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLAW_OFFHAND = new HashMap<>();
    private static final Map<UUID, Long> LAST_SCIMITAR_ATTACK = new HashMap<>();
    private static final Map<UUID, PendingMeleeAttack> PENDING_MELEE = new HashMap<>();
    private static final Map<UUID, PendingBulwarkAttack> PENDING_BULWARK = new HashMap<>();
    private static final Map<UUID, Long> TARTSY_DASH_UNTIL = new HashMap<>();
    private static final Map<UUID, Set<Integer>> TARTSY_DASH_HITS = new HashMap<>();
    private static final Map<UUID, Long> BLADE_STAFF_REFLECT_UNTIL = new HashMap<>();
    private static final Map<UUID, Map<MobEffect, MobEffectInstance>> BLADE_STAFF_EFFECT_BASELINE =
        new HashMap<>();
    private static final Map<UUID, PendingEffectReflection> PENDING_BLADE_STAFF_EFFECTS =
        new HashMap<>();
    private static final ThreadLocal<Boolean> REFLECTING_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> BLADE_STAFF_AOE_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> BULWARK_BASH_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final String TARTSY_CRIT = "ArsenalTartsyCritical";
    private static final java.util.UUID PERMANENT_FRACTURE = java.util.UUID.nameUUIDFromBytes("nanonaitors_arsenal:ball_chain_fracture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final String CLAW_LAST_HAND = "ArsenalClawLastHand", CLAW_LAST_TARGET = "ArsenalClawLastTarget",
        CLAW_CRIT_CHAIN = "ArsenalClawCritChain";

    public static boolean onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return false;
        long now = player.level().getGameTime();
        if (TARTSY_DASH_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE) >= now) return true;
        ItemStack active = player.getUseItem();
        ItemStack main = player.getMainHandItem();
        DamageSource source = event.getSource();
        if (main.getItem() instanceof ArsenalWeaponItem staff
            && staff.kind() == WeaponKind.BLADE_STAFF && player.getOffhandItem().isEmpty()
            && BLADE_STAFF_REFLECT_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE) >= now
            && combatSource(player, source) && event.getAmount() > 0) {
            if (REFLECTING_DAMAGE.get() || player.invulnerableTime > 0) return true;
            if (!player.level().isClientSide && source.getEntity() instanceof LivingEntity attacker
                && attacker != player && player.level() instanceof ServerLevel level) {
                PendingEffectReflection pending = new PendingEffectReflection(attacker,
                    BLADE_STAFF_EFFECT_BASELINE.getOrDefault(player.getUUID(), harmfulEffects(player)),
                    now + 2L);
                PENDING_BLADE_STAFF_EFFECTS.put(player.getUUID(), pending);
                REFLECTING_DAMAGE.set(true);
                boolean returned;
                try {
                    returned = attacker.hurt(player.damageSources().playerAttack(player), event.getAmount());
                    if (returned && !isDirectMelee(source)) {
                        com.nanonaitor.arsenal.config.ConfiguredEffects.apply(attacker,"bladeStaffReflect",20,0);
                    }
                }
                finally { REFLECTING_DAMAGE.set(false); }
                if (!returned) return true;
                transferReflectedEffects(player, pending);
                player.getCooldowns().addCooldown(main.getItem(), recoveryCooldown(player, main, 10));
                BLADE_STAFF_REFLECT_UNTIL.remove(player.getUUID());
                BLADE_STAFF_EFFECT_BASELINE.remove(player.getUUID());
                player.stopUsingItem();
                damage(main, player, 1);
                level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.25F);
            }
            return true;
        }
        if (active.getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.TARTSY) {
            if (!player.level().isClientSide) {
                damage(active, player, 1);
                player.getCooldowns().addCooldown(active.getItem(), recoveryCooldown(player, active, 80));
                player.stopUsingItem();
                ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.05F);
            }
            return true;
        }
        boolean blocked = false;
        if (active.getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR
            && otherHandEmpty(player, active) && sunWarBlocks(source)) {
            blocked = true;
        } else if (active.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BALL_AND_CHAIN
            && player.getMainHandItem() == active && player.getOffhandItem().isEmpty()
            && !BALLS.containsKey(player.getUUID()) && directedShieldBlocks(player, source)) {
            blocked = true;
        } else if (active.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.SCIMITAR && ParityRules.guarding(player)
            && directedShieldBlocks(player, source)) {
            blocked = true;
        }
        if (!blocked) return false;
        if (!player.level().isClientSide) {
            if (active.getItem() instanceof ArsenalWeaponItem weapon
                && weapon.kind() == WeaponKind.SCIMITAR && hasDualScimitars(player)) {
                int wear = ParityRules.blockWear(event.getAmount());
                damage(player.getMainHandItem(), player, wear);
                damage(player.getOffhandItem(), player, wear);
                var referenceId=new ResourceLocation("spartanshields","iron_basic_shield");
                ItemStack reference=new ItemStack(BuiltInRegistries.ITEM.containsKey(referenceId)
                    ? BuiltInRegistries.ITEM.get(referenceId) : net.minecraft.world.item.Items.SHIELD);
                if (source.getEntity() instanceof LivingEntity attacker
                    && attacker.getMainHandItem().canDisableShield(reference,player,attacker)
                    && player.getRandom().nextFloat() < .25F
                        + .05F * EnchantmentHelper.getBlockEfficiency(attacker) + (attacker.isSprinting() ? .75F : 0)) {
                    ParityRules.disable(player, 100);
                    player.level().broadcastEntityEvent(player, (byte)30);
                }
            } else damage(active, player, 1);
            if (active.getItem() instanceof ArsenalShieldItem shield
                && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR) {
                int strain = active.getOrCreateTag().getInt("ArsenalGuardStrain") + 1;
                active.getOrCreateTag().putInt("ArsenalGuardStrain", strain >= 25 ? 0 : strain);
                if (strain >= 25) { player.getCooldowns().addCooldown(active.getItem(),60); player.stopUsingItem(); }
            }
            boolean metalGuard = active.getItem() instanceof ArsenalWeaponItem weapon
                && (weapon.kind() == WeaponKind.BALL_AND_CHAIN
                    || weapon.kind() == WeaponKind.SCIMITAR && hasDualScimitars(player));
            var blockSound = metalGuard
                ? net.minecraft.world.level.block.Blocks.IRON_BARS.defaultBlockState()
                    .getSoundType().getPlaceSound()
                : SoundEvents.SHIELD_BLOCK;
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), blockSound,
                SoundSource.PLAYERS, metalGuard ? 1.125F : 0.9F, 0.90F);
        }
        return true;
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack bulwark = equippedShield(player, ArsenalShieldItem.Type.SUN_WAR);
            if (!bulwark.isEmpty()) {
                event.setAmount(event.getAmount() * 0.85F);
            }
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.getPersistentData().getBoolean(TARTSY_CRIT)) {
            attacker.getPersistentData().putBoolean(TARTSY_CRIT, false);
            event.setAmount(event.getAmount() * 1.5F);
            if (attacker.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CRIT, event.getEntity().getX(), event.getEntity().getY(0.6D), event.getEntity().getZ(), 16, .3D, .3D, .3D, .15D);
                level.playSound(null, event.getEntity().blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        ItemStack weapon = TierEffects.attackingStack(attacker);
        if (!(weapon.getItem() instanceof ArsenalWeaponItem arsenal)) return;
        LivingEntity target = event.getEntity();
        if (arsenal.kind() == WeaponKind.BLADE_STAFF
            && !BLADE_STAFF_AOE_DAMAGE.get() && isDirectMelee(event.getSource())
            && attacker.level() instanceof ServerLevel level && event.getAmount() > 0.0F) {
            double radius = bladeStaffRadius(arsenal.tier());
            BLADE_STAFF_AOE_DAMAGE.set(true);
            try {
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(radius),
                        entity -> entity != target && entity.distanceToSqr(target) <= radius * radius
                            && validTarget(attacker, entity))) {
                    nearby.hurt(attacker.damageSources().playerAttack(attacker), event.getAmount());
                }
            } finally {
                BLADE_STAFF_AOE_DAMAGE.set(false);
            }
        }
        PendingMeleeAttack pending = PENDING_MELEE.remove(attacker.getUUID());
        boolean fullyCharged = pending != null && pending.fullyCharged
            && pending.kind == arsenal.kind() && pending.targetId == target.getId()
            && attacker.level().getGameTime() - pending.gameTime <= 1L;
        if (arsenal.kind() == WeaponKind.MORNING_STAR && fullyCharged) {
            MobEffectInstance current = target.getEffect(ModEffects.ARMOR_FRACTURE.get());
            int next = Math.min(arsenal.tier().fractureCap, current == null ? 1 : current.getAmplifier() + 2);
            int duration = target instanceof Player ? 200 : 600;
            com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,"morningStarFracture",duration,next-1);
            if (attacker.level() instanceof ServerLevel level) VisualEffects.armorFracture(level, target, next);
        }
        if (arsenal.kind() == WeaponKind.SCIMITAR) {
            int amplifier = TierEffects.weakness(attacker, arsenal.tier());
            com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,"scimitarHit",200,amplifier);
        }
    }

    /** Paired claws trade crowd control for combo consistency. */
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity().getLastDamageSource() != null
            && event.getEntity().getLastDamageSource().getEntity() instanceof Player player
            && hasMatchingClaws(player)) event.setStrength(event.getStrength() * 0.5F);
    }

    public static boolean onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if(!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getMainHandItem(),true))return true;
        if (hasDualScimitars(player)) return true;
        if (shieldOwnsInput(player)) return true;
        if (player.getMainHandItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR) {
            if (!player.level().isClientSide) {
                PENDING_BULWARK.put(player.getUUID(), new PendingBulwarkAttack(event.getTarget().getId(),
                    player.level().getGameTime(), attackChargeMultiplier(player)));
            }
            return false;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) return false;
        if (weapon.kind()==WeaponKind.BATTERING_RAM && RAMS.containsKey(player.getUUID())) return true;
        if (weapon.kind() == WeaponKind.BLADE_STAFF
            && BLADE_STAFF_REFLECT_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE) >= player.level().getGameTime()) return true;
        if (weapon.kind() == WeaponKind.FLAIL || weapon.kind() == WeaponKind.BALL_AND_CHAIN
            || weapon.kind() == WeaponKind.MORNING_STAR) return true;
        boolean piercedFrames = weapon.kind() == WeaponKind.CLAWS
            && event.getTarget() instanceof LivingEntity target
            && !player.level().isClientSide && player.getAttackStrengthScale(0.5F) >= 1.0F
            && hasMatchingClaws(player);
        if (piercedFrames && event.getTarget() instanceof LivingEntity target) {
            if(!CLAW_HITS.ready(player,target,player.level().getGameTime()))return true;
            CLAW_MAIN_PENDING.put(player,target);
            target.invulnerableTime = 0;
        }
        if (!player.level().isClientSide && (weapon.kind() == WeaponKind.MORNING_STAR
            || weapon.kind() == WeaponKind.CLAWS)) {
            PENDING_MELEE.put(player.getUUID(), new PendingMeleeAttack(event.getTarget().getId(),
                player.level().getGameTime(), weapon.kind(), player.getAttackStrengthScale(0.5F) >= 1.0F, 0,
                piercedFrames));
        }
        return false;
    }

    public static boolean onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if(!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(event.getEntity(),event.getEntity().getMainHandItem(),true))return true;
        if (hasDualScimitars(event.getEntity())) return true;
        if (shieldOwnsInput(event.getEntity())) return true;
        return event.getEntity().getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon
            && (weapon.kind() == WeaponKind.FLAIL || weapon.kind() == WeaponKind.BALL_AND_CHAIN
                || weapon.kind() == WeaponKind.MORNING_STAR || weapon.kind() == WeaponKind.CLAWS
                || weapon.kind() == WeaponKind.LINKED_CLAWS);
    }

    public static boolean onEntityJoin(EntityJoinLevelEvent event) {
        if(event.getEntity() instanceof ItemEntity dropped && dropped.getItem().getItem() instanceof ArsenalWeaponItem)
            setFlailFlag(dropped.getItem(),false);
        if (event.getEntity() instanceof Display.ItemDisplay display
            && display.getPersistentData().getBoolean(ServerWeaponVisuals.VISUAL_TAG)) return true;
        return event.getEntity() instanceof ItemEntity item
            && item.getItem().getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.LINKED_CLAWS;
    }

    private static boolean isDirectMelee(DamageSource source) {
        return source.getEntity() instanceof LivingEntity
            && source.getDirectEntity() == source.getEntity()
            && !source.is(DamageTypeTags.IS_PROJECTILE)
            && !source.is(DamageTypeTags.IS_EXPLOSION)
            && !source.is(DamageTypeTags.IS_FIRE)
            && !source.is(DamageTypeTags.WITCH_RESISTANT_TO)
            && !source.is(DamageTypes.THORNS)
            && !source.is(DamageTypes.SONIC_BOOM);
    }

    private static boolean combatSource(Player defender, DamageSource source) {
        return source.getEntity() != defender && (source.getEntity() instanceof LivingEntity
            || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile);
    }

    public static void prepareBulwarkDamage(LivingHurtEvent event) {
        if (BULWARK_BASH_DAMAGE.get() || !(event.getSource().getEntity() instanceof Player attacker)
            || !isDirectMelee(event.getSource())) return;
        if (TierEffects.attackingStack(attacker).getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType()==ArsenalShieldItem.Type.SUN_WAR) {
            PendingBulwarkAttack pending=PENDING_BULWARK.remove(attacker.getUUID());
            float charge=pending!=null && pending.targetId==event.getEntity().getId()
                && attacker.level().getGameTime()-pending.gameTime<=1L ? pending.chargeMultiplier : attackChargeMultiplier(attacker);
            event.setAmount(bulwarkDamage(attacker)*charge);
        }
    }

    public static void protectStaffAtHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem w
            && w.kind() == WeaponKind.BLADE_STAFF && player.getOffhandItem().isEmpty()
            && BLADE_STAFF_REFLECT_UNTIL.getOrDefault(player.getUUID(),Long.MIN_VALUE) >= player.level().getGameTime()
            && combatSource(player,event.getSource())) { event.setAmount(0); event.setCanceled(true); }
    }

    private static void scimitarBash(ServerPlayer player) {
        if (!ParityRules.guarding(player)) return;
        ItemStack main=player.getMainHandItem(), off=player.getOffhandItem();
        net.minecraft.world.entity.Entity hit=scimitarTarget(player,main);
        LivingEntity target=hit==null?null:damageOwner(hit);
        ParityRules.disable(player,30);
        player.swinging=false;
        player.swing(InteractionHand.MAIN_HAND,true);
        ServerLevel level=player.serverLevel();
        level.playSound(null,player.blockPosition(),SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,1,1);
        if (target == null) return;
        DamageSource source=player.damageSources().playerAttack(player);
        float amount=0;
        for(ItemStack stack:List.of(main,off)) amount+=EnchantmentBridge.modifyDamage(level,stack,target,source,
            (float)ParityRules.attribute(player,stack,Attributes.ATTACK_DAMAGE));
        if (TierEffects.hurtEntityWithStack(main,hit,source,amount)) {
            for(ItemStack stack:List.of(main,off)) {
                EnchantmentBridge.doPostAttackEffectsWithItemSource(level,target,source,stack);
                damage(stack,player,1);
            }
            target.knockback(1,player.getX()-target.getX(),player.getZ()-target.getZ());
        }
    }

    public static void handleControl(ServerPlayer player, byte action, boolean active) {
        if (player.isSpectator()) return;
        if (action == ModNetwork.CANCEL_WEAPON_INPUTS) { cancelWeaponInputs(player); return; }
        if(!levelAllowsControl(player,action,active))return;
        if (shieldOwnsInput(player)) {
            cancelWeaponInputs(player);
            if (action != ModNetwork.TARTSY_BASH && action != ModNetwork.BULWARK_BASH
                && action != ModNetwork.BULWARK_MENU_GUARD) return;
        }
        if (action == ModNetwork.FLAIL) flailControl(player, active);
        else if (action == ModNetwork.BALL_CHAIN) ballControl(player, active);
        else if (action == ModNetwork.RAM) ramControl(player, active);
        else if (action == ModNetwork.BULWARK_BASH) bulwarkBash(player);
        else if (action == ModNetwork.CLAW) clawAttack(player);
        else if (action == ModNetwork.CLAW_MAIN) clawMainAttack(player);
        else if (action == ModNetwork.MORNING_STAR) morningStarControl(player, active);
        else if (action == ModNetwork.BULWARK_ATTACK) bulwarkAttack(player, active);
        else if (action == ModNetwork.BULWARK_MENU_GUARD) bulwarkMenuGuard(player, active);
        else if (action == ModNetwork.SCIMITAR_ATTACK) scimitarAttack(player, active);
        else if (action == ModNetwork.BALL_WIND_BOOST) ballWindBoost(player, active);
        else if (action == ModNetwork.TARTSY_BASH) tartsyBash(player);
        // Obsolete staff auto-strike packets no longer perform attacks.
        else if (action == ModNetwork.BLADE_STAFF_REFLECT) beginBladeStaffReflection(player);
        else if (action == ModNetwork.SCIMITAR_BASH) scimitarBash(player);
    }

    private static void beginBladeStaffReflection(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BLADE_STAFF || !player.getOffhandItem().isEmpty()
            || player.getCooldowns().isOnCooldown(stack.getItem())
            || BLADE_STAFF_REFLECT_UNTIL.containsKey(player.getUUID())) return;
        BLADE_STAFF_REFLECT_UNTIL.put(player.getUUID(), player.level().getGameTime() + 20L);
        BLADE_STAFF_EFFECT_BASELINE.put(player.getUUID(), harmfulEffects(player));
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    private static boolean levelAllowsControl(ServerPlayer player,byte action,boolean active) {
        if(action==ModNetwork.SCIMITAR_ATTACK && ParityRules.pair(player)
            && (!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getMainHandItem(),true)
                || !com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getOffhandItem(),true)))return false;
        if(action==ModNetwork.BULWARK_BASH || action==ModNetwork.TARTSY_BASH
            || action==ModNetwork.BULWARK_ATTACK || action==ModNetwork.BULWARK_MENU_GUARD)return true;
        boolean offhand=action==ModNetwork.CLAW || action==ModNetwork.SCIMITAR_ATTACK && active;
        ItemStack stack=offhand?player.getOffhandItem():player.getMainHandItem();
        if(action==ModNetwork.SCIMITAR_BASH && !com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getOffhandItem(),true))return false;
        if(com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,stack,true))return true;
        if(!offhand) {
            cancelWeaponInputs(player);
            BLADE_STAFF_REFLECT_UNTIL.remove(player.getUUID());
            BLADE_STAFF_EFFECT_BASELINE.remove(player.getUUID());
            if(player.isUsingItem() && player.getUseItem()==stack)player.stopUsingItem();
        }
        return false;
    }

    public static boolean shieldOwnsInput(Player player) {
        return (player.isUsingItem() && (player.getUseItem().getItem() instanceof net.minecraft.world.item.ShieldItem
            || player.getUseItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)))
            || TARTSY_DASH_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE)
                >= player.level().getGameTime();
    }

    private static void cancelWeaponInputs(ServerPlayer player) {
        BallState ball = BALLS.get(player.getUUID());
        if (ball != null) cancelBall(player);
        cancelMorningStar(player);
        ramControl(player, false);
        flailControl(player, false);
    }

    private static void tartsyBash(ServerPlayer player) {
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof ArsenalShieldItem shield)
            || shield.shieldType() != ArsenalShieldItem.Type.TARTSY
            || player.getCooldowns().isOnCooldown(stack.getItem())) return;
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 0.001D) return;
        look = look.normalize();
        player.setDeltaMovement(look.x * 1.0125D, 0.12D, look.z * 1.0125D);
        player.hurtMarked = true;
        long now = player.level().getGameTime();
        TARTSY_DASH_UNTIL.put(player.getUUID(), now + 20L);
        TARTSY_DASH_HITS.put(player.getUUID(), new HashSet<>());
        ((ServerLevel)player.level()).playSound(null,player.blockPosition(),SoundEvents.SHIELD_BLOCK,
            SoundSource.PLAYERS,0.9F,1.0F);
        player.getCooldowns().addCooldown(stack.getItem(), recoveryCooldown(player, stack, 80));
        player.stopUsingItem();
    }

    private static void bladeStaffAttack(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.BLADE_STAFF
            || !player.getOffhandItem().isEmpty() || player.isUsingItem()
            || player.getAttackStrengthScale(0.5F) < 0.95F) return;
        LivingEntity target = clawTarget(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        if (target != null) player.attack(target);
    }

    private static void clawMainAttack(ServerPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem claws)
            || claws.kind() != WeaponKind.CLAWS) return;
        LivingEntity target = clawTarget(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        if (target != null) player.attack(target);
    }

    private static void clawAttack(ServerPlayer player) {
        clawAttack(player,true);
    }
    private static void clawAttack(ServerPlayer player,boolean mayDefer) {
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem claws) || claws.kind() != WeaponKind.CLAWS
            || !(player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked) || linked.kind() != WeaponKind.LINKED_CLAWS) return;
        LivingEntity target = clawTarget(player);
        player.swing(InteractionHand.OFF_HAND, true);
        long now = player.level().getGameTime();
        long previous = LAST_CLAW_OFFHAND.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        double attackSpeed = Math.max(0.1D, player.getAttributeValue(Attributes.ATTACK_SPEED));
        double cooldownTicks = 20.0D / attackSpeed;
        float strength = previous == Long.MIN_VALUE ? 1.0F
            : (float)Math.max(0.0D, Math.min(1.0D, (now - previous + 0.5D) / cooldownTicks));
        if (target == null) return;
        boolean pierceFrames = strength >= 1.0F;
        if(pierceFrames && !CLAW_HITS.ready(player,target,now)) {
            if(mayDefer)CLAW_DEFERRED.putIfAbsent(player,new DeferredClaw(target,player.getMainHandItem(),now+4));
            return;
        }
        CLAW_DEFERRED.remove(player);
        LAST_CLAW_OFFHAND.put(player.getUUID(), now);
        int oldInvulnerability=target.invulnerableTime;
        if (pierceFrames) {
            target.invulnerableTime = 0;
        }
        ServerLevel level = (ServerLevel)player.level();
        float baseDamage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = baseDamage * (0.2F + strength * strength * 0.8F);
        ItemStack linkedStack = player.getOffhandItem();
        DamageSource source = player.damageSources().playerAttack(player);
        damage = EnchantmentBridge.modifyDamage(level, linkedStack, target, source, damage);
        float enchantedKnockback = EnchantmentBridge.modifyKnockback(level, linkedStack, target, source, 0.0F);
        PENDING_MELEE.put(player.getUUID(), new PendingMeleeAttack(target.getId(), now,
            WeaponKind.CLAWS, strength >= 0.9F, 1, pierceFrames));
        if (target.hurt(source, damage)) {
            if(pierceFrames)CLAW_HITS.confirmed(player,target,now);
            if (enchantedKnockback > 0.0F) {
                double yaw = Math.toRadians(player.getYRot());
                target.knockback(enchantedKnockback, Math.sin(yaw), -Math.cos(yaw));
            }
            EnchantmentBridge.doPostAttackEffectsWithItemSource(level, target, source, linkedStack);
            player.getMainHandItem().hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            syncClawPair(player);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.15F);
        } else target.invulnerableTime=oldInvulnerability;
    }

    private static void bulwarkAttack(ServerPlayer player, boolean offhand) {
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ArsenalShieldItem shield)
            || shield.shieldType() != ArsenalShieldItem.Type.SUN_WAR
            || (offhand && !player.getMainHandItem().isEmpty())) return;
        if (player.getAttackStrengthScale(0.5F) < 0.95F) return;
        LivingEntity target = clawTarget(player);
        player.swing(hand, true);
        if (target != null) {
            ServerLevel level = (ServerLevel)player.level();
            float amount = bulwarkDamage(player) * attackChargeMultiplier(player);
            if (target.hurt(player.damageSources().playerAttack(player), amount)) {
                damage(stack, player, 1);
                level.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 0.8F, 0.82F);
            }
        }
        player.resetAttackStrengthTicker();
    }

    private static void bulwarkMenuGuard(ServerPlayer player, boolean active) {
        if (!active) {
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalShieldItem shield
                && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR) player.stopUsingItem();
            return;
        }
        ItemStack bulwark = equippedShield(player, ArsenalShieldItem.Type.SUN_WAR);
        if (bulwark.isEmpty() || !otherHandEmpty(player, bulwark)
            || player.getCooldowns().isOnCooldown(bulwark.getItem())) return;
        player.startUsingItem(player.getMainHandItem() == bulwark
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    private static void scimitarAttack(ServerPlayer player, boolean offhand) {
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.SCIMITAR) return;
        boolean dual = hasDualScimitars(player);
        if (dual && (player.isUsingItem() || ParityRules.disabled(player))) return;
        if (player.isUsingItem() && !(offhand && player.getUseItem() == player.getMainHandItem()
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && (main.kind() == WeaponKind.MORNING_STAR || main.kind() == WeaponKind.FLAIL
                || main.kind() == WeaponKind.BALL_AND_CHAIN))) return;
        if (!offhand && !dual) return; // ordinary main-hand scimitars use vanilla attacks
        long now = player.level().getGameTime();
        long previous = LAST_SCIMITAR_ATTACK.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        double cooldown = ParityRules.interval(player, offhand);
        if (previous != Long.MIN_VALUE && now >= previous && now - previous + 0.5D < cooldown) return;
        LAST_SCIMITAR_ATTACK.put(player.getUUID(), now);
        net.minecraft.world.entity.Entity hitbox = scimitarTarget(player,stack);
        LivingEntity target = damageOwner(hitbox);
        player.swinging = false;
        player.swing(hand, true);
        if (target != null) {
            ServerLevel level = (ServerLevel)player.level();
            DamageSource source = player.damageSources().playerAttack(player);
            float attributeDamage = (float)ParityRules.attribute(player,stack,Attributes.ATTACK_DAMAGE);
            float amount = EnchantmentBridge.modifyDamage(level, stack, target, source, attributeDamage);
            if (offhand ? OffhandDamage.hurt(stack,hitbox,target,source,amount)
                : TierEffects.hurtEntityWithStack(stack,hitbox,source,amount)) {
                EnchantmentBridge.doPostAttackEffectsWithItemSource(level, target, source, stack);
                float knockback=EnchantmentBridge.modifyKnockback(level,stack,target,source,0);
                if(knockback>0) target.knockback(knockback*.5D,Math.sin(Math.toRadians(player.getYRot())),
                    -Math.cos(Math.toRadians(player.getYRot())));
                damage(stack, player, 1);
                level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                    SoundSource.PLAYERS, 0.85F, offhand ? 1.08F : 0.98F);
            }
        }
        // A lone off-hand Scimitar has an independent cooldown and must not
        // drain or reset the main-hand Blade Staff's vanilla attack bar.
        if (dual || !offhand) player.resetAttackStrengthTicker();
    }

    private static float mainHandWeaponAddedDamage(Player player) {
        return (float)itemAttributeContribution(player, player.getMainHandItem(), Attributes.ATTACK_DAMAGE);
    }

    private static double offhandScimitarAttackSpeed(Player player) {
        double current = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double mainContribution = itemAttributeContribution(player, player.getMainHandItem(), Attributes.ATTACK_SPEED);
        return Math.max(0.1D, current - mainContribution + WeaponKind.SCIMITAR.speedModifier);
    }

    private static double itemAttributeContribution(Player player, ItemStack stack,
            net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        double base = player.getAttributeBaseValue(attribute);
        double value=base;
        var modifiers=stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attribute);
        for(var m:modifiers) if(m.getOperation()==AttributeModifier.Operation.ADDITION) value+=m.getAmount();
        double added=value;
        for(var m:modifiers) if(m.getOperation()==AttributeModifier.Operation.MULTIPLY_BASE) value+=added*m.getAmount();
        for(var m:modifiers) if(m.getOperation()==AttributeModifier.Operation.MULTIPLY_TOTAL) value*=1+m.getAmount();
        return value-base;
    }

    private static LivingEntity clawTarget(ServerPlayer player) {
        double reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        Vec3 from = player.getEyePosition(), to = from.add(player.getLookAngle().scale(reach));
        BlockHitResult wall=player.level().clip(new net.minecraft.world.level.ClipContext(from,to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(wall.getType()==HitResult.Type.BLOCK) {to=wall.getLocation();reach=from.distanceTo(to);}
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, to,
            player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0D),
            entity -> entity instanceof LivingEntity living && validTarget(player, living), reach * reach);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static LivingEntity damageOwner(net.minecraft.world.entity.Entity entity) {
        if(entity instanceof LivingEntity living)return living;
        if(entity instanceof net.minecraftforge.entity.PartEntity<?> part && part.getParent() instanceof LivingEntity living)return living;
        return null;
    }
    private static net.minecraft.world.entity.Entity scimitarTarget(ServerPlayer player,ItemStack stack) {
        double reach=ParityRules.attribute(player,stack,net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        Vec3 from=player.getEyePosition(),to=from.add(player.getLookAngle().scale(reach));
        BlockHitResult wall=player.level().clip(new net.minecraft.world.level.ClipContext(from,to,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(wall.getType()==HitResult.Type.BLOCK){to=wall.getLocation();reach=from.distanceTo(to);}
        EntityHitResult hit=ProjectileUtil.getEntityHitResult(player,from,to,
            player.getBoundingBox().expandTowards(to.subtract(from)).inflate(1),
            entity -> damageOwner(entity)!=null && validTarget(player,damageOwner(entity)),reach*reach);
        return hit==null?null:hit.getEntity();
    }

    private static boolean hasMatchingClaws(Player player) {
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main)
            || main.kind() != WeaponKind.CLAWS) return false;
        return player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked
            && linked.kind() == WeaponKind.LINKED_CLAWS && linked.tier() == main.tier();
    }

    private static boolean hasDualScimitars(Player player) {
        return player.getMainHandItem().getItem() instanceof ArsenalWeaponItem main
            && player.getOffhandItem().getItem() instanceof ArsenalWeaponItem off
            && main.kind() == WeaponKind.SCIMITAR && off.kind() == WeaponKind.SCIMITAR;
    }

    private static boolean otherHandEmpty(Player player, ItemStack held) {
        return player.getMainHandItem() == held
            ? player.getOffhandItem().isEmpty() : player.getMainHandItem().isEmpty();
    }

    private static boolean directedShieldBlocks(Player player, DamageSource source) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null || !sunWarBlocks(source)) return false;
        Vec3 incoming = sourcePosition.subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 facing = player.getViewVector(1.0F).multiply(1.0D, 0.0D, 1.0D);
        if (incoming.lengthSqr() < 0.0001D || facing.lengthSqr() < 0.0001D) return true;
        return incoming.normalize().dot(facing.normalize()) > 0.0D;
    }

    private static double horizontalDistanceSqr(Player player, LivingEntity target) {
        double x = target.getX() - player.getX(), z = target.getZ() - player.getZ();
        return x * x + z * z;
    }

    /** Adds Long Chain only along the horizontal projection of the attack. */
    private static Vec3 horizontalLongChainPath(Player player, ItemStack stack, double configuredDistance) {
        double bonus = Math.min(configuredDistance - 0.01D,
            ChainWeaponStats.longChainBonus(player, stack));
        double baseDistance = Math.max(0.01D, configuredDistance - bonus);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 0.0001D) return look.scale(baseDistance);
        return look.scale(baseDistance).add(horizontal.normalize().scale(bonus));
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player.level().isClientSide || !(player instanceof ServerPlayer server)) return;
        pairClaws(player);
        tickClawState(server);
        updateBulwarkMovement(player);
        updateOccupiedHandAttackPenalty(player);
        updateDualScimitarAttackSpeed(player);
        updateScimitarGuardModels(player);
        if(!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getMainHandItem(),false)) {
            cancelWeaponInputs(server);
            BLADE_STAFF_REFLECT_UNTIL.remove(player.getUUID());
            BLADE_STAFF_EFFECT_BASELINE.remove(player.getUUID());
        }
        if(player.isUsingItem() && (!com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getUseItem(),false)
            || ParityRules.guarding(player) && !com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,player.getOffhandItem(),false)))player.stopUsingItem();
        if ((!ParityRules.pair(player) || ParityRules.disabled(player)) && player.isUsingItem()
            && ParityRules.scimitar(player.getUseItem())) player.stopUsingItem();
        if (player.tickCount % 20 == 0) {
            for (int i=0;i<player.getInventory().getContainerSize();i++) {
                ItemStack stack=player.getInventory().getItem(i);
                if (stack.getItem() instanceof ArsenalShieldItem s && s.shieldType()==ArsenalShieldItem.Type.SUN_WAR)
                    stack.getOrCreateTag().putInt("ArsenalGuardStrain",Math.max(0,stack.getOrCreateTag().getInt("ArsenalGuardStrain")-1));
            }
        }
        if (shieldOwnsInput(player)) cancelWeaponInputs(server);
        updateBall(server);
        updateRam(server);
        updateMorningStar(server);
        updateTartsyDash(server);
        updateBladeStaff(server);
    }

    private static void updateTartsyDash(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long until = TARTSY_DASH_UNTIL.get(player.getUUID());
        if (until == null) return;
        if (now > until) {
            TARTSY_DASH_UNTIL.remove(player.getUUID());
            TARTSY_DASH_HITS.remove(player.getUUID());
            return;
        }
        Set<Integer> hit = TARTSY_DASH_HITS.computeIfAbsent(player.getUUID(), ignored -> new HashSet<>());
        ServerLevel level = (ServerLevel)player.level();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(.65D, .35D, .65D), target -> validTarget(player, target))) {
            if (!hit.add(target.getId())) continue;
            if (target.hurt(player.damageSources().playerAttack(player), 2.0F)) {
                com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,"tartsyDash",20,0);
                target.knockback(.55D, player.getX() - target.getX(), player.getZ() - target.getZ());
                player.getPersistentData().putBoolean(TARTSY_CRIT, true);
            }
        }
    }

    private static void updateBladeStaff(ServerPlayer player) {
        updateReflectedEffects(player);
        ItemStack stack = player.getMainHandItem();
        Long until = BLADE_STAFF_REFLECT_UNTIL.get(player.getUUID());
        if (until == null) return;
        long now = player.level().getGameTime();
        boolean valid = stack.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BLADE_STAFF && player.getOffhandItem().isEmpty();
        if (!valid) {
            if(player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem active
                && active.kind()==WeaponKind.BLADE_STAFF)player.stopUsingItem();
            BLADE_STAFF_REFLECT_UNTIL.remove(player.getUUID());
            BLADE_STAFF_EFFECT_BASELINE.remove(player.getUUID());
            return;
        }
        if (now >= until) {
            player.stopUsingItem();
            player.getCooldowns().addCooldown(stack.getItem(), recoveryCooldown(player, stack, 60));
            BLADE_STAFF_REFLECT_UNTIL.remove(player.getUUID());
            BLADE_STAFF_EFFECT_BASELINE.remove(player.getUUID());
            return;
        }
        if (!PENDING_BLADE_STAFF_EFFECTS.containsKey(player.getUUID())) {
            BLADE_STAFF_EFFECT_BASELINE.put(player.getUUID(), harmfulEffects(player));
        }
        if (!player.isUsingItem() || player.getUseItem() != stack)
            player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    private static double bladeStaffRadius(WeaponTier tier) {
        // The modern tier list does not currently register Sentient, but retaining
        // this tier-aware rule keeps its behavior correct when that material is ported.
        return "sentient".equals(tier.id) ? 3.0D : 2.0D;
    }

    private static Map<MobEffect, MobEffectInstance> harmfulEffects(LivingEntity entity) {
        Map<MobEffect, MobEffectInstance> effects = new HashMap<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (!effect.getEffect().isBeneficial()) {
                effects.put(effect.getEffect(), copyEffect(effect));
            }
        }
        return effects;
    }

    private static MobEffectInstance copyEffect(MobEffectInstance effect) {
        return new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(),
            effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }

    private static void updateReflectedEffects(ServerPlayer defender) {
        PendingEffectReflection pending = PENDING_BLADE_STAFF_EFFECTS.get(defender.getUUID());
        if (pending == null) return;
        transferReflectedEffects(defender, pending);
        if (defender.level().getGameTime() >= pending.expiresAt || !pending.attacker.isAlive()) {
            PENDING_BLADE_STAFF_EFFECTS.remove(defender.getUUID());
        }
    }

    private static void transferReflectedEffects(Player defender,
                                                   PendingEffectReflection pending) {
        for (Map.Entry<MobEffect, MobEffectInstance> entry :
                harmfulEffects(defender).entrySet()) {
            MobEffectInstance before = pending.baseline.get(entry.getKey());
            MobEffectInstance after = entry.getValue();
            boolean introduced = before == null || after.getAmplifier() > before.getAmplifier()
                || after.getDuration() > before.getDuration() + 1;
            if (!introduced) continue;
            defender.removeEffect(entry.getKey());
            if (before != null) defender.addEffect(copyEffect(before));
            pending.attacker.addEffect(copyEffect(after));
        }
    }

    private static final class PendingEffectReflection {
        private final LivingEntity attacker;
        private final Map<MobEffect, MobEffectInstance> baseline;
        private final long expiresAt;

        private PendingEffectReflection(LivingEntity attacker,
                                        Map<MobEffect, MobEffectInstance> baseline,
                                        long expiresAt) {
            this.attacker = attacker;
            this.baseline = new HashMap<>(baseline);
            this.expiresAt = expiresAt;
        }
    }

    private static int recoveryCooldown(Player player, ItemStack stack, int base) {
        int ticks = ModEnchantments.level(player, stack, ModEnchantments.RECOVERY) > 0
            ? Math.max(1, base / 2) : base;
        if (stack.getItem() instanceof ArsenalShieldItem
            && ModEnchantments.level(player, stack, ModEnchantments.BREECHED) > 0) {
            ticks *= 2;
        }
        return ticks;
    }

    private static int morningStarChargeTicks() {
        return Math.max(1, (int)Math.ceil(ArsenalConfig.MORNING_STAR_CHARGE_SECONDS.get() * 20.0D));
    }

    private static void morningStarControl(ServerPlayer player, boolean active) {
        if (!active) {
            releaseMorningStar(player);
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.MORNING_STAR) return;
        MorningStarState state = MORNING_STARS.get(player.getUUID());
        if (state == null) {
            if (player.getAttackStrengthScale(0.5F) < 1.0F) return;
            state = new MorningStarState(player.level().getGameTime(), weapon.tier());
            MORNING_STARS.put(player.getUUID(), state);
            player.startUsingItem(InteractionHand.MAIN_HAND);
        }
        state.lastHeartbeat = player.level().getGameTime();
    }

    private static void updateMorningStar(ServerPlayer player) {
        MorningStarState state = MORNING_STARS.get(player.getUUID());
        if (state == null) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.MORNING_STAR || weapon.tier() != state.tier
            || player.level().getGameTime() - state.lastHeartbeat > 4L) {
            cancelMorningStar(player);
            return;
        }
        if (!player.isUsingItem()) player.startUsingItem(InteractionHand.MAIN_HAND);
        if (!state.chimed && player.level().getGameTime() - state.started >= morningStarChargeTicks()) {
            state.chimed = true;
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.55F, 1.55F);
        }
        if (player.level().getGameTime() % 3L == 0L) {
            double progress = Math.min(1.0D, Math.max(0.0D,
                (player.level().getGameTime() - state.started) / (double)morningStarChargeTicks()));
            VisualEffects.morningStarCharge((ServerLevel)player.level(), player, progress);
        }
    }

    private static void cancelMorningStar(ServerPlayer player) {
        MORNING_STARS.remove(player.getUUID());
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.MORNING_STAR) player.stopUsingItem();
    }

    private static void releaseMorningStar(ServerPlayer player) {
        MorningStarState state = MORNING_STARS.remove(player.getUUID());
        if (state == null) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.MORNING_STAR || weapon.tier() != state.tier) {
            player.stopUsingItem();
            return;
        }
        long elapsed = Math.max(0L, player.level().getGameTime() - state.started);
        int chargeTicks = morningStarChargeTicks();
        boolean full = elapsed >= chargeTicks;
        int quarters = Math.min(4, (int)Math.floor(elapsed * 4.0D / chargeTicks));
        float multiplier = 1.0F + quarters * 0.10F;
        ServerLevel level = (ServerLevel)player.level();
        ItemStack stack = player.getMainHandItem();
        DamageSource source = player.damageSources().playerAttack(player);
        double reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get());
        Vec3 start = player.getEyePosition(), direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(reach));
        // Charge widens the strike laterally up to the weapon's current reach.
        // It never extends the forward axis; reach still governs the ordinary
        // center-line hit independently of charge.
        double maximumSweep = Math.max(1.0D, reach);
        double sweepRange = maximumSweep * quarters / 4.0D;
        double queryRange = Math.max(reach, sweepRange + 1.0D);
        AABB area = player.getBoundingBox().inflate(queryRange, 1.25D, queryRange);
        ArrayList<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> validTarget(player, target) && player.hasLineOfSight(target))) {
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 relative = center.subtract(start);
            double forward = relative.dot(direction);
            Vec3 sideways = relative.subtract(direction.scale(forward));
            double directWidth = 0.55D + target.getBbWidth() * 0.5D;
            boolean direct = forward >= -0.25D && forward <= reach + target.getBbWidth()
                && sideways.lengthSqr() <= directWidth * directWidth;

            Vec3 horizontal = center.subtract(player.position()).multiply(1.0D, 0.0D, 1.0D);
            Vec3 horizontalLook = direction.multiply(1.0D, 0.0D, 1.0D);
            if (horizontalLook.lengthSqr() > 0.0001D) horizontalLook = horizontalLook.normalize();
            double horizontalForward = horizontal.dot(horizontalLook);
            Vec3 horizontalRight = new Vec3(-horizontalLook.z, 0.0D, horizontalLook.x);
            double lateral = horizontal.dot(horizontalRight);
            double allowedSweep = sweepRange + target.getBbWidth() * 0.5D;
            // Charge widens only from side to side. The whole strip reaches as
            // far forward as the weapon, but charging never lengthens that axis.
            boolean swept = quarters > 0 && horizontalForward >= -target.getBbWidth() * 0.25D
                && horizontalForward <= reach + target.getBbWidth() * 0.5D
                && Math.abs(lateral) <= allowedSweep
                && Math.abs(center.y - player.getY(0.5D)) <= 1.75D;
            if (direct || swept) targets.add(target);
        }
        targets.sort(Comparator.comparingDouble(player::distanceToSqr));
        boolean hit = false;
        float attributeDamage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity target : targets) {
            float damage = EnchantmentBridge.modifyDamage(level, stack, target, source,
                attributeDamage * multiplier);
            if (!target.hurt(source, damage)) continue;
            hit = true;
            float knockback = EnchantmentBridge.modifyKnockback(level, stack, target, source, 0.0F);
            if (knockback > 0.0F) target.knockback(knockback, -direction.x, -direction.z);
            EnchantmentBridge.doPostAttackEffectsWithItemSource(level, target, source, stack);
            if (full) {
                applyMorningStarFracture(level, target, weapon);
                if (level.getRandom().nextFloat() < 0.20F) {
                    com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,"morningStarStun",60,0);
                }
            }
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.stopUsingItem();
        player.resetAttackStrengthTicker();
        if (sweepRange > 0.0D) VisualEffects.morningStarSweep(level, player, weapon.tier(),
            sweepRange, reach);
        if (hit) stack.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        level.playSound(null, player.blockPosition(), hit ? SoundEvents.PLAYER_ATTACK_SWEEP
            : SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, hit ? 1.0F : 0.65F,
            full ? 0.72F : 0.92F);
    }

    private static void applyMorningStarFracture(ServerLevel level, LivingEntity target,
            ArsenalWeaponItem weapon) {
        MobEffectInstance current = target.getEffect(ModEffects.ARMOR_FRACTURE.get());
        int next = Math.min(weapon.tier().fractureCap, current == null ? 1 : current.getAmplifier() + 2);
        com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,"morningStarFracture",target instanceof Player ? 200 : 600,next-1);
        VisualEffects.armorFracture(level, target, next);
    }

    private static void flailControl(ServerPlayer player, boolean active) {
        if (!active) {
            setServerFlailSprite(player, ItemStack.EMPTY);
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem activeWeapon
                && activeWeapon.kind() == WeaponKind.FLAIL) player.stopUsingItem();
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.FLAIL) return;
        if (isBlockingConventionalShield(player)) {
            setServerFlailSprite(player, ItemStack.EMPTY);
            return;
        }
        setServerFlailSprite(player, player.getMainHandItem());
        if (!player.isUsingItem()) player.startUsingItem(InteractionHand.MAIN_HAND);
        long now = player.level().getGameTime(), previous = LAST_FLAIL.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem())) return;
        LAST_FLAIL.put(player.getUUID(), now);
        ServerLevel level = (ServerLevel) player.level();
        ItemStack flail = player.getMainHandItem();
        DamageSource source = player.damageSources().playerAttack(player);
        float attackStrength = player.getAttackStrengthScale(0.5F);
        boolean hit = false;
        double radius = ChainWeaponStats.flailReach(player, flail);
        double vertical = ChainWeaponStats.flailVerticalReach(player);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius, vertical, radius),
                target -> validTarget(player, target) && player.hasLineOfSight(target)
                    && horizontalDistanceSqr(player, target) <= radius * radius
                    && Math.abs(target.getY(0.5D) - player.getY(0.5D)) <= vertical)) {
            target.invulnerableTime = 0;
            hit |= target.hurt(source, effectiveMeleeDamage(player, target, source, attackStrength));
        }
        if (hit) flail.hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        player.resetAttackStrengthTicker();
        level.playSound(null, player.blockPosition(), hit ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE,
            SoundSource.PLAYERS, hit ? 1.0F : 0.65F, 0.82F);
        VisualEffects.flail(level, player, weapon.tier());
    }

    private static boolean isBlockingConventionalShield(Player player) {
        if (!player.isUsingItem()) return false;
        ItemStack active = player.getUseItem();
        if (active.isEmpty() || active.getItem().getUseAnimation(active) != UseAnim.BLOCK) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(active.getItem());
        return id == null || !"defenders".equals(id.getNamespace());
    }

    private static float effectiveMeleeDamage(ServerPlayer player, LivingEntity target,
            DamageSource source, float attackStrength) {
        float attributeDamage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float scaledBase = attributeDamage * (0.2F + attackStrength * attackStrength * 0.8F);
        float enchantedDamage = EnchantmentBridge.modifyDamage((ServerLevel)player.level(),
            player.getMainHandItem(), target, source, attributeDamage);
        return scaledBase + attackStrength * (enchantedDamage - attributeDamage);
    }

    private static void setServerFlailSprite(ServerPlayer player, ItemStack active) {
        ItemStack previous = ACTIVE_FLAIL_SPRITES.remove(player.getUUID());
        if (previous != null && previous != active) setFlailFlag(previous, false);
        if (!active.isEmpty()) {
            setFlailFlag(active, true);
            ACTIVE_FLAIL_SPRITES.put(player.getUUID(), active);
        }
    }

    private static void setFlailFlag(ItemStack stack, boolean active) {
        stack.getOrCreateTag().putBoolean("ArsenalActive", active);
    }

    private static void ballControl(ServerPlayer player, boolean active) {
        BallState existing = BALLS.get(player.getUUID());
        if (existing != null && existing.releasing) return;
        if (!active) { releaseBall(player); return; }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BALL_AND_CHAIN) return;
        BallState state = BALLS.computeIfAbsent(player.getUUID(),
            id -> new BallState(player.level().getGameTime(), weapon.tier()));
        state.lastHeartbeat = player.level().getGameTime();
        setFlailFlag(player.getMainHandItem(), true);
        player.getMainHandItem().getOrCreateTag().putLong("ArsenalBallStarted",state.started);
        player.getMainHandItem().getOrCreateTag().putBoolean("ArsenalBallThrown",false);
    }

    private static void ballWindBoost(ServerPlayer player, boolean active) {
        BallState state = BALLS.get(player.getUUID());
        if (state == null || state.releasing) return;
        active = active && player.getOffhandItem().isEmpty();
        state.windBoost = active;
        player.getMainHandItem().getOrCreateTag().putBoolean("ArsenalBallBoost", active);
        if (active) state.nextSwing = Math.min(state.nextSwing, player.level().getGameTime()
            + ChainWeaponStats.swingIntervalTicks(player, player.getMainHandItem(), true));
    }

    private static void updateBall(ServerPlayer player) {
        BallState state = BALLS.get(player.getUUID());
        if (state == null) return;
        if (state.releasing) { updateBallRelease(player, state); return; }
        if (shieldOwnsInput(player)) {
            cancelBall(player); return;
        }
        if (!player.getOffhandItem().isEmpty()) ballWindBoost(player, false);
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BALL_AND_CHAIN || weapon.tier() != state.tier
            ) {
            cancelBall(player); return;
        }
        long now = player.level().getGameTime();
        if (!player.isUsingItem()) player.startUsingItem(InteractionHand.MAIN_HAND);
        if (now - state.lastHeartbeat > 3) { releaseBall(player); return; }
        if (now >= state.nextSwing) {
            int maxCharges = TierEffects.maxBallCharges(player, weapon.tier());
            int previousCharge = state.charge;
            state.nextSwing = now + ChainWeaponStats.swingIntervalTicks(player,
                player.getMainHandItem(), state.windBoost);
            state.charge = Math.min(maxCharges, state.charge + 1);
            lineAttack(player, ChainWeaponStats.ballWindupReach(player, player.getMainHandItem()), 0.5F,
                0.3F, false, true, weapon.tier());
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.65F, 0.72F);
            if (previousCharge < maxCharges && state.charge == maxCharges) {
                ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.55F, 1.55F);
                VisualEffects.ballFullCharge((ServerLevel)player.level(), player);
            }
        }
        VisualEffects.ballWindup((ServerLevel)player.level(), player, weapon.tier(), state.charge);
    }

    private static void releaseBall(ServerPlayer player) {
        BallState state = BALLS.get(player.getUUID());
        if (state == null || state.releasing) return;
        if (state.charge <= 0 || shieldOwnsInput(player)) {
            cancelBall(player); return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BALL_AND_CHAIN || weapon.tier() != state.tier
            ) {
            cancelBall(player); return;
        }
        state.releasing = true; state.releaseTick = player.level().getGameTime();
        state.releaseDuration = ChainWeaponStats.ballReleaseAnimationTicks(player, player.getMainHandItem());
        state.direction = player.getLookAngle().normalize();
        WeaponTier tier = state.tier;
        int maxCharges = TierEffects.maxBallCharges(player, tier);
        int effectiveCharge = TierEffects.effectiveCharge(player, tier, state.charge);
        double configuredReach = ChainWeaponStats.ballThrowReach(player,
            player.getMainHandItem(), effectiveCharge);
        Vec3 requestedPath = horizontalLongChainPath(player, player.getMainHandItem(), configuredReach);
        state.direction = requestedPath.normalize();
        double requestedDistance = requestedPath.length();
        state.distance = stopDistance((ServerLevel)player.level(), player.getEyePosition(), state.direction,
            requestedDistance);
        var visual=player.getMainHandItem().getOrCreateTag();
        visual.putBoolean("ArsenalBallThrown",true);
        visual.putInt("ArsenalBallCharge",state.charge);
        visual.putLong("ArsenalBallRelease",state.releaseTick);
        visual.putInt("ArsenalBallDuration",state.releaseDuration);
        visual.putDouble("ArsenalBallDistance",state.distance);
        visual.putDouble("ArsenalBallX",state.direction.x);
        visual.putDouble("ArsenalBallY",state.direction.y);
        visual.putDouble("ArsenalBallZ",state.direction.z);
        AttackImpact impact = lineAttackAlong(player, state.direction, state.distance,
            ballDamageMultiplier(player, tier, state.charge),
            ballKnockback(tier, state.charge), state.charge >= maxCharges, true, tier);
        boolean hitBlock = state.distance + 0.05D < requestedDistance;
        Vec3 blockImpact = player.getEyePosition().add(state.direction.scale(state.distance));
        playBallImpact(player, impact, hitBlock, blockImpact);
        ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW,
            SoundSource.PLAYERS, 0.9F, 0.72F);
    }

    private static void updateBallRelease(ServerPlayer player, BallState state) {
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BALL_AND_CHAIN || weapon.tier() != state.tier
            ) {
            cancelBall(player); return;
        }
        long age = player.level().getGameTime() - state.releaseTick;
        if (age >= state.releaseDuration / 2 && !state.returned) {
            state.returned = true;
            WeaponTier tier = state.tier;
            AttackImpact impact = lineAttackAlong(player, state.direction, state.distance,
                ballDamageMultiplier(player, tier, state.charge),
                ballKnockback(tier, state.charge), false, true, tier);
            playBallImpact(player, impact, false, Vec3.ZERO);
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS, 0.9F, 0.78F);
        }
        if (age >= state.releaseDuration) {
            BALLS.remove(player.getUUID());
            setFlailFlag(player.getMainHandItem(), false); player.stopUsingItem();
            player.getMainHandItem().getOrCreateTag().putBoolean("ArsenalBallThrown",false);
        }
        else {
            WeaponTier tier = state.tier;
            double progress = age / (double)state.releaseDuration;
            VisualEffects.ballRelease((ServerLevel)player.level(), player, tier, state, progress);
        }
    }

    private static void cancelBall(ServerPlayer player) {
        BALLS.remove(player.getUUID());
        if (player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BALL_AND_CHAIN) setFlailFlag(player.getMainHandItem(), false);
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BALL_AND_CHAIN) player.stopUsingItem();
    }

    private static AttackImpact lineAttack(ServerPlayer player, double distance, float multiplier,
            float knockback, boolean fracture, boolean applyEnchantments, WeaponTier tier) {
        Vec3 path = horizontalLongChainPath(player, player.getMainHandItem(), distance);
        return lineAttackAlong(player, path.normalize(), path.length(), multiplier,
            knockback, fracture, applyEnchantments, tier);
    }

    private static AttackImpact lineAttackAlong(ServerPlayer player, Vec3 requestedDirection,
            double distance, float multiplier, float knockback, boolean fracture,
            boolean applyEnchantments, WeaponTier tier) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition().add(0, -0.55D, 0);
        Vec3 direction = requestedDirection.normalize();
        Vec3 end = start.add(direction.scale(stopDistance(level, start, direction, distance)));
        AABB box = new AABB(start, end).inflate(0.75D, 1.0D, 0.75D);
        float attributeDamage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float base = attributeDamage * multiplier;
        ItemStack weapon = player.getMainHandItem();
        DamageSource source = player.damageSources().playerAttack(player);
        Vec3 firstHit = null;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, target -> validTarget(player, target))) {
            AABB targetBox = target.getBoundingBox().inflate(0.85D, 1.0D, 0.85D);
            if (!targetBox.contains(start) && !targetBox.contains(end)
                && targetBox.clip(start, end).isEmpty()) continue;
            BallState flight = BALLS.get(player.getUUID());
            boolean thrown = flight != null && flight.releasing;
            if (thrown) target.invulnerableTime = 0;
            float damage = applyEnchantments
                ? EnchantmentBridge.modifyDamage(level, weapon, target, source, base)
                : base;
            float finalKnockback = applyEnchantments
                ? EnchantmentBridge.modifyKnockback(level, weapon, target, source, knockback)
                : knockback;
            if (thrown && flight.charge >= TierEffects.maxBallCharges(player, tier))
                damage = compensateForArmor(target, damage, tier.armorPiercePercent() / 100.0F);
            if (target.hurt(source, damage)) {
                if (firstHit == null) firstHit = target.getBoundingBox().getCenter();
                target.knockback(finalKnockback, -direction.x, -direction.z);
                if (applyEnchantments) EnchantmentBridge.doPostAttackEffectsWithItemSource(level, target, source, weapon);
                player.getMainHandItem().hurtAndBreak(1, player, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                if (fracture) fractureArmor(target, tier);
            }
        }
        return new AttackImpact(firstHit);
    }

    private static void playBallImpact(ServerPlayer player, AttackImpact impact, boolean hitBlock,
            Vec3 blockImpact) {
        ServerLevel level = (ServerLevel)player.level();
        if (impact.position != null) {
            level.playSound(null, BlockPos.containing(impact.position), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0F, 0.78F);
        }
        if (hitBlock) {
            level.playSound(null, BlockPos.containing(blockImpact), SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.525F, 1.25F);
        }
    }

    public static float compensateForArmor(LivingEntity target, float damage, float piercing) {
        float armor=target.getArmorValue(), toughness=(float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float reduced=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(damage,armor,toughness);
        float desired=reduced+(damage-reduced)*piercing;
        if(desired<=reduced+0.001F)return damage;
        float low=damage,high=Math.max(damage*10,damage+armor*5+10);
        for(int i=0;i<16;i++){float mid=(low+high)*0.5F;if(net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(mid,armor,toughness)<desired)low=mid;else high=mid;}
        return high;
    }

    private static void fractureArmor(LivingEntity target, WeaponTier tier) {
        if (target instanceof Player) {
            int amp = tier.armorPiercePercent() >= 100 ? 4 : tier.armorPiercePercent() >= 75 ? 2 : tier.armorPiercePercent() >= 50 ? 1 : 0;
            com.nanonaitor.arsenal.config.ConfiguredEffects.apply(target,"ballPlayerFracture",200,amp);
            return;
        }
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        AttributeModifier old = armor.getModifier(PERMANENT_FRACTURE);
        double oldAmount = old == null ? 0.0D : old.getAmount();
        double reduction = Math.min(Math.max(0.0D, armor.getValue()), Math.max(2.0D, Math.max(0.0D, armor.getValue()) * 0.10D));
        replacePermanent(armor, new AttributeModifier(PERMANENT_FRACTURE, "Arsenal modifier",
            oldAmount - reduction, AttributeModifier.Operation.ADDITION));
    }

    private static void ramControl(ServerPlayer player, boolean active) {
        if (!active) {
            RAMS.remove(player.getUUID());
            if (player.isUsingItem()
                && player.getUseItem().getItem() instanceof ArsenalWeaponItem weapon
                && weapon.kind() == WeaponKind.BATTERING_RAM) player.stopUsingItem();
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.BATTERING_RAM
            || !player.getOffhandItem().isEmpty() || (!player.isCreative() && player.getFoodData().getFoodLevel() <= 6)) return;
        RamState state = RAMS.get(player.getUUID());
        if (state == null) {
            state = new RamState(player.getYRot(), player.getXRot(), attackChargeMultiplier(player));
            RAMS.put(player.getUUID(), state);
            // A held ram is one continuous attack. Snapshot its strength first,
            // then consume the cooldown exactly once when that ram begins.
            player.resetAttackStrengthTicker();
        }
        state.lastHeartbeat = player.level().getGameTime();
    }

    private static void updateRam(ServerPlayer player) {
        RamState state = RAMS.get(player.getUUID());
        if (state == null) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.BATTERING_RAM
            || !player.getOffhandItem().isEmpty() || player.level().getGameTime() - state.lastHeartbeat > 4
            || (!player.isCreative() && player.getFoodData().getFoodLevel() <= 6)) {
            RAMS.remove(player.getUUID()); if (player.isUsingItem()) player.stopUsingItem(); return;
        }
        if (!player.isUsingItem()) player.startUsingItem(InteractionHand.MAIN_HAND);
        player.setYRot(state.yaw); player.setXRot(state.pitch);
        Vec3 forward = Vec3.directionFromRotation(0.0F, state.yaw).normalize();
        player.setDeltaMovement(forward.x * 0.32D, player.getDeltaMovement().y, forward.z * 0.32D);
        player.hurtMarked = true;
        breakRamBlocks(player, weapon.tier(), forward);
        for (LivingEntity target : ((ServerLevel)player.level()).getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().move(forward.scale(0.8D)).inflate(0.7D, 0.4D, 0.7D), target -> validTarget(player, target))) {
            if (state.hit.add(target.getId()) && target.hurt(player.damageSources().playerAttack(player),
                    (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE) * state.damageMultiplier)) {
                damage(player.getMainHandItem(), player, 1); player.causeFoodExhaustion(0.5F);
            }
        }
    }

    private static void breakRamBlocks(ServerPlayer player, WeaponTier tier, Vec3 forward) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 right = new Vec3(-forward.z, 0, forward.x), center = player.position().add(forward.scale(1.25D));
        Set<BlockPos> positions = new HashSet<>();
        for (int lateral = -1; lateral <= 1; lateral++) for (int vertical = 0; vertical <= 2; vertical++)
            positions.add(BlockPos.containing(center.add(right.scale(lateral)).add(0, vertical, 0)));
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !ramCanBreak(state, tier)) continue;
            if (net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level, player.gameMode.getGameModeForPlayer(), player, pos) < 0) continue;
            if (!player.mayInteract(level,pos) || !player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,player.getMainHandItem())) continue;
            if (level.destroyBlock(pos, true, player)) {
                damage(player.getMainHandItem(), player, 1); player.causeFoodExhaustion(0.35F);
                if (player.getMainHandItem().isEmpty()) return;
            }
        }
    }

    private static boolean ramCanBreak(BlockState state, WeaponTier tier) {
        return state.is(switch (tier) {
            case WOOD -> RAM_WOOD;
            case STONE, COPPER, GOLD -> RAM_STONE;
            default -> tier.ramBreakLevel >= 3 ? RAM_DIAMOND : RAM_IRON;
        });
    }

    private static TagKey<Block> ramTag(String tier) {
        return TagKey.create(Registries.BLOCK,
            new ResourceLocation(ArsenalMod.MOD_ID, "battering_ram/" + tier));
    }

    private static void bulwarkBash(ServerPlayer player) {
        ItemStack active = player.getUseItem();
        if (!(active.getItem() instanceof ArsenalShieldItem shield)
            || shield.shieldType() != ArsenalShieldItem.Type.SUN_WAR
            || !otherHandEmpty(player, active)) return;
        long now = player.level().getGameTime(), ready = player.getPersistentData().getLong("ArsenalBulwarkReady");
        if (now < ready) return;
        int cooldown = recoveryCooldown(player, active, 60);
        player.getPersistentData().putLong("ArsenalBulwarkReady", now + cooldown);
        player.getCooldowns().addCooldown(active.getItem(), cooldown);
        player.stopUsingItem();
        InteractionHand hand = player.getMainHandItem() == active
            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        player.swing(hand, true);
        float damage = bulwarkDamage(player);
        ServerLevel level = (ServerLevel)player.level();
        ItemStack bulwark = active;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0D),
                target -> validTarget(player, target) && player.distanceToSqr(target) <= 16.0D)) {
            boolean hit;
            BULWARK_BASH_DAMAGE.set(true);
            try { hit = target.hurt(player.damageSources().playerAttack(player), damage); }
            finally { BULWARK_BASH_DAMAGE.remove(); }
            if (hit) {
                target.knockback(1.4D, player.getX() - target.getX(), player.getZ() - target.getZ());
                damage(bulwark, player, 1);
                if (bulwark.isEmpty()) break;
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.75F);
        level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.9F, 0.70F);
        player.resetAttackStrengthTicker();
    }

    private static void updateBulwarkMovement(Player player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        ItemStack held = equippedShield(player, ArsenalShieldItem.Type.SUN_WAR);
        if (held.isEmpty()) {
            speed.removeModifier(BULWARK_SLOW);
            return;
        }
        boolean guarding = player.isUsingItem() && player.getUseItem() == held && otherHandEmpty(player, held);
        replaceTransient(speed, new AttributeModifier(BULWARK_SLOW, "Arsenal modifier", guarding ? -0.75D : -0.40D,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void updateOccupiedHandAttackPenalty(Player player) {
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        ItemStack bulwark = equippedShield(player, ArsenalShieldItem.Type.SUN_WAR);
        boolean penalizedBulwark = !bulwark.isEmpty() && !otherHandEmpty(player, bulwark);
        boolean penalizedBladeStaff = player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.BLADE_STAFF && !player.getOffhandItem().isEmpty();
        if (!penalizedBulwark && !penalizedBladeStaff) {
            speed.removeModifier(OCCUPIED_HAND_ATTACK_SLOW);
            return;
        }
        replaceTransient(speed, new AttributeModifier(OCCUPIED_HAND_ATTACK_SLOW, "Arsenal modifier", -0.50D,
            AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void updateDualScimitarAttackSpeed(Player player) {
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null) return;
        // Remove the old global bonus; only the scheduler combines per-hand speeds.
        speed.removeModifier(DUAL_SCIMITAR_ATTACK_SPEED);
    }

    private static void updateScimitarGuardModels(Player player) {
        boolean guard = ParityRules.guarding(player);
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ParityRules.scimitar(stack)) stack.getOrCreateTag().putBoolean("ArsenalPaired",
                hasDualScimitars(player) && (stack==player.getMainHandItem() || stack==player.getOffhandItem()));
            if (stack.getItem() instanceof ArsenalWeaponItem weapon
                && weapon.kind() == WeaponKind.SCIMITAR) setFlailFlag(stack,
                    guard && stack == player.getOffhandItem());
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.SCIMITAR) setFlailFlag(offhand, guard);
    }

    private static int maxBallCharges(WeaponTier tier) { return tier == WeaponTier.GOLD ? 2 : 3; }

    /** Gold skips the middle power stage: its second revolution is a full charge. */
    private static int effectiveBallCharge(WeaponTier tier, int charge) {
        return tier == WeaponTier.GOLD && charge >= 2 ? 3 : charge;
    }

    /** Matches Minecraft's normal melee cooldown curve: 20% at empty, 100% at full. */
    private static float attackChargeMultiplier(Player player) {
        float charge = player.getAttackStrengthScale(0.5F);
        return 0.2F + charge * charge * 0.8F;
    }

    private static float ballDamageMultiplier(Player player, WeaponTier tier, int charge) {
        return ParityRules.ballMultiplier(TierEffects.effectiveCharge(player, tier, charge));
    }
    private static float ballKnockback(WeaponTier tier, int charge) {
        return tier == WeaponTier.GOLD ? 2.40F * charge / 2.0F : 0.75F + charge * 0.55F;
    }

    private static void pairClaws(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        boolean mayCreateLinkedClaw = off.isEmpty() || isLinkedClaw(off);

        // Linked claws are temporary projections, never real inventory items. A hand-swap can
        // otherwise move one into the hotbar/main hand before the old off-hand-only cleanup runs.
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isLinkedClaw(player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }

        if (main.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == WeaponKind.CLAWS
            && mayCreateLinkedClaw) {
            ItemStack desired = new ItemStack(ModItems.get(WeaponKind.LINKED_CLAWS, weapon.tier()).get());
            desired.setTag(main.hasTag() ? main.getTag().copy() : null);
            player.setItemSlot(EquipmentSlot.OFFHAND, desired);
        }
    }

    private static boolean isLinkedClaw(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == WeaponKind.LINKED_CLAWS;
    }

    private static void syncClawPair(Player player) {
        if (!player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked
            && linked.kind() == WeaponKind.LINKED_CLAWS) player.getOffhandItem().setTag(player.getMainHandItem().hasTag() ? player.getMainHandItem().getTag().copy() : null);
    }

    private static boolean sunWarBlocks(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_FIRE)) return false;
        String id = source.getMsgId().toLowerCase(Locale.ROOT);
        return !id.contains("drown") && !id.contains("starve") && !id.contains("cactus")
            && !id.contains("inwall") && !id.contains("hotfloor") && !id.contains("outofworld");
    }
    private static float bulwarkDamage(Player player) {
        return BULWARK_BASE_DAMAGE + player.getArmorValue() * BULWARK_ARMOR_DAMAGE;
    }
    private static ItemStack equippedShield(Player player, ArsenalShieldItem.Type type) {
        if (player.getMainHandItem().getItem() instanceof ArsenalShieldItem shield && shield.shieldType() == type) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof ArsenalShieldItem shield && shield.shieldType() == type) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }
    private static boolean validTarget(Player player, LivingEntity target) {
        return !player.isSpectator() && target != player && target.isAlive() && !target.isSpectator()
            && !player.isAlliedTo(target) && (!(target instanceof Player other)
                || player.canHarmPlayer(other) && (player.getServer()==null || player.getServer().isPvpAllowed()));
    }
    private static double stopDistance(ServerLevel level, Vec3 start, Vec3 direction, double distance) {
        Vec3 normalized = direction.normalize();
        Vec3 side = new Vec3(-normalized.z, 0.0D, normalized.x);
        if (side.lengthSqr() > 0.0001D) side = side.normalize().scale(0.35D);
        Vec3[] offsets = {
            Vec3.ZERO,
            new Vec3(0.0D, -0.45D, 0.0D),
            new Vec3(0.0D, 0.35D, 0.0D),
            side,
            side.scale(-1.0D)
        };
        double closest = distance;
        for (Vec3 offset : offsets) {
            Vec3 rayStart = start.add(offset);
            closest = Math.min(closest,
                blockingDistance(level, rayStart, normalized, distance));
        }
        return closest;
    }

    /**
     * Finds the first meaningful obstruction while allowing chain attacks to
     * pass through grass, flowers and other replaceable/non-colliding plants.
     * Re-clipping from just beyond an ignored shape preserves collision with a
     * real wall immediately behind it.
     */
    private static double blockingDistance(ServerLevel level, Vec3 start,
            Vec3 direction, double distance) {
        Vec3 end = start.add(direction.scale(distance));
        Vec3 cursor = start;
        for (int ignored = 0; ignored < 16 && cursor.distanceToSqr(end) > 0.0001D;
             ignored++) {
            HitResult hit = level.clip(new net.minecraft.world.level.ClipContext(cursor,
                end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                (net.minecraft.world.entity.Entity)null));
            if (!(hit instanceof BlockHitResult block)) return distance;
            BlockState state = level.getBlockState(block.getBlockPos());
            if (!state.canBeReplaced()
                && !state.getCollisionShape(level, block.getBlockPos()).isEmpty()) {
                return start.distanceTo(block.getLocation());
            }
            cursor = block.getLocation().add(direction.scale(0.05D));
        }
        return distance;
    }
    private static net.minecraft.world.phys.shapes.CollisionContext playerForClip(Vec3 start, ServerLevel level) {
        return net.minecraft.world.phys.shapes.CollisionContext.empty();
    }
    private static void damage(ItemStack stack, LivingEntity owner, int amount) {
        if (amount > 0 && !stack.isEmpty()) stack.hurtAndBreak(amount, owner,
            e -> e.broadcastBreakEvent(owner.getOffhandItem() == stack ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND));
    }

    public static final class BallState {
        final WeaponTier tier;
        long started, lastHeartbeat, nextSwing, releaseTick; int charge, releaseDuration = 16;
        boolean releasing, returned, windBoost; double distance; Vec3 direction;
        BallState(long tick, WeaponTier tier) { started = lastHeartbeat = nextSwing = tick; this.tier = tier; }
        public int charge() { return charge; }
        public double distance() { return distance; }
    }
    private record AttackImpact(Vec3 position) {}
    private static final class RamState {
        final float yaw, pitch, damageMultiplier; long lastHeartbeat; final Set<Integer> hit = new HashSet<>();
        RamState(float yaw, float pitch, float damageMultiplier) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.damageMultiplier = damageMultiplier;
        }
    }
    private static final class MorningStarState {
        final long started;
        final WeaponTier tier;
        long lastHeartbeat;
        boolean chimed;
        MorningStarState(long started, WeaponTier tier) {
            this.started = this.lastHeartbeat = started;
            this.tier = tier;
        }
    }
    private static final PairedClawHitLimiter<Player,LivingEntity> CLAW_HITS=new PairedClawHitLimiter<>();
    private static final Map<Player,LivingEntity> CLAW_MAIN_PENDING=new java.util.WeakHashMap<>();
    private static final Map<Player,DeferredClaw> CLAW_DEFERRED=new java.util.WeakHashMap<>();
    private record DeferredClaw(LivingEntity target,ItemStack stack,long expires) {}
    public static void confirmClawHit(Player player,LivingEntity target) {
        if(CLAW_MAIN_PENDING.remove(player)==target)CLAW_HITS.confirmed(player,target,player.level().getGameTime());
    }
    private static void tickClawState(ServerPlayer player) {
        CLAW_MAIN_PENDING.remove(player);
        for(String key:List.of(CLAW_CRIT_CHAIN,CLAW_LAST_HAND,CLAW_LAST_TARGET))player.getPersistentData().remove(key);
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
            ItemStack stack=player.getInventory().getItem(slot);
            if(stack.hasTag() && stack.getItem() instanceof ArsenalWeaponItem weapon
                && (weapon.kind()==WeaponKind.CLAWS || weapon.kind()==WeaponKind.LINKED_CLAWS))
                for(String key:List.of(CLAW_CRIT_CHAIN,CLAW_LAST_HAND,CLAW_LAST_TARGET,"ClawCriticalChain"))stack.getTag().remove(key);
        }
        DeferredClaw pending=CLAW_DEFERRED.get(player);
        if(pending==null)return;
        long now=player.level().getGameTime();
        if(now>pending.expires || player.getMainHandItem()!=pending.stack || !hasMatchingClaws(player)
            || shieldOwnsInput(player) || !validTarget(player,pending.target) || clawTarget(player)!=pending.target
            || !com.nanonaitor.arsenal.compat.LevelRequirements.canUse(player,pending.stack,false)) {
            CLAW_DEFERRED.remove(player);return;
        }
        if(CLAW_HITS.ready(player,pending.target,now)) {
            CLAW_DEFERRED.remove(player);clawAttack(player,false);
        }
    }

    private record PendingMeleeAttack(int targetId, long gameTime, WeaponKind kind, boolean fullyCharged,
        int hand, boolean piercedFrames) {}
    private record PendingBulwarkAttack(int targetId, long gameTime, float chargeMultiplier) {}
    private CombatEvents() {}
}
