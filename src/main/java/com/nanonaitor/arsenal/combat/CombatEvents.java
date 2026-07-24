package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.ArsenalMod;
import com.nanonaitor.arsenal.item.*;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.registry.ModEffects;
import com.nanonaitor.arsenal.registry.ModItems;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
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
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;

public final class CombatEvents {
    private static final float BULWARK_BASE_DAMAGE = 1.0F;
    private static final float BULWARK_ARMOR_DAMAGE = 1.0F;
    private static final Identifier BULWARK_SLOW = Identifier.fromNamespaceAndPath(ArsenalMod.MOD_ID, "bulwark_slow");
    private static final TagKey<Block> RAM_WOOD = ramTag("wood");
    private static final TagKey<Block> RAM_STONE = ramTag("stone");
    private static final TagKey<Block> RAM_IRON = ramTag("iron");
    private static final TagKey<Block> RAM_DIAMOND = ramTag("diamond");
    private static final Map<UUID, Long> LAST_FLAIL = new HashMap<>();
    private static final Map<UUID, ItemStack> ACTIVE_FLAIL_SPRITES = new HashMap<>();
    private static final Map<UUID, BallState> BALLS = new HashMap<>();
    private static final Map<UUID, RamState> RAMS = new HashMap<>();
    private static final Map<UUID, Long> LAST_CLAW_OFFHAND = new HashMap<>();
    private static final Map<UUID, PendingMeleeAttack> PENDING_MELEE = new HashMap<>();
    private static final Identifier PERMANENT_FRACTURE = Identifier.fromNamespaceAndPath(ArsenalMod.MOD_ID, "ball_chain_fracture");
    private static final String CLAW_LAST_HAND = "ArsenalClawLastHand", CLAW_LAST_TARGET = "ArsenalClawLastTarget";

    public static boolean onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return false;
        ItemStack active = player.getUseItem();
        if (!(active.getItem() instanceof ArsenalShieldItem shield)) return false;
        DamageSource source = event.getSource();
        if (shield.shieldType() != ArsenalShieldItem.Type.SUN_WAR || !player.getOffhandItem().isEmpty()
            || player.getMainHandItem() != active || !sunWarBlocks(source)) return false;
        if (!player.level().isClientSide()) {
            damage(active, player, 1);
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
                SoundSource.PLAYERS, 0.9F, 0.90F);
        }
        return true;
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack bulwark = equippedShield(player, ArsenalShieldItem.Type.SUN_WAR);
            if (!bulwark.isEmpty() && player.getMainHandItem() == bulwark && player.getOffhandItem().isEmpty()) {
                event.setAmount(event.getAmount() * 0.85F);
            }
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (attacker.getMainHandItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR) {
            if (!attacker.getOffhandItem().isEmpty()) {
                event.setAmount(0.0F);
                return;
            }
            event.setAmount(bulwarkDamage(attacker));
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!(weapon.getItem() instanceof ArsenalWeaponItem arsenal)) return;
        LivingEntity target = event.getEntity();
        PendingMeleeAttack pending = PENDING_MELEE.remove(attacker.getUUID());
        boolean fullyCharged = pending != null && pending.fullyCharged
            && pending.kind == arsenal.kind() && pending.targetId == target.getId()
            && attacker.level().getGameTime() - pending.gameTime <= 1L;
        if (arsenal.kind() == WeaponKind.MORNING_STAR && fullyCharged) {
            MobEffectInstance current = target.getEffect(ModEffects.ARMOR_FRACTURE.getHolder().orElseThrow());
            int next = Math.min(arsenal.tier().fractureCap, current == null ? 1 : current.getAmplifier() + 2);
            int duration = target instanceof Player ? 200 : 600;
            target.addEffect(new MobEffectInstance(ModEffects.ARMOR_FRACTURE.getHolder().orElseThrow(),
                duration, next - 1, false, true, true));
        }
        if (arsenal.kind() == WeaponKind.SCIMITAR && fullyCharged && attacker.getRandom().nextInt(100) < 10) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, true, true));
        }
        if (arsenal.kind() == WeaponKind.CLAWS) {
            attacker.getPersistentData().putInt(CLAW_LAST_HAND, 0);
            attacker.getPersistentData().putInt(CLAW_LAST_TARGET, target.getId());
        }
    }

    public static boolean onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR && !player.getOffhandItem().isEmpty()) return true;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) return false;
        if (weapon.kind() == WeaponKind.FLAIL || weapon.kind() == WeaponKind.BALL_AND_CHAIN) return true;
        if (!player.level().isClientSide()
            && (weapon.kind() == WeaponKind.MORNING_STAR || weapon.kind() == WeaponKind.SCIMITAR)) {
            PENDING_MELEE.put(player.getUUID(), new PendingMeleeAttack(event.getTarget().getId(),
                player.level().getGameTime(), weapon.kind(), player.getAttackStrengthScale(0.5F) >= 0.9F));
        }
        if (weapon.kind() == WeaponKind.CLAWS && event.getTarget() instanceof LivingEntity target
            && !player.level().isClientSide() && player.getAttackStrengthScale(0.5F) >= 0.9F
            && player.getPersistentData().getIntOr(CLAW_LAST_HAND, -1) == 1
            && player.getPersistentData().getIntOr(CLAW_LAST_TARGET, -1) == target.getId()) target.invulnerableTime = 0;
        return false;
    }

    public static boolean onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        return event.getEntity().getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon
            && (weapon.kind() == WeaponKind.FLAIL || weapon.kind() == WeaponKind.BALL_AND_CHAIN);
    }

    public static boolean onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity() instanceof Display.ItemDisplay display
            && display.getPersistentData().getBooleanOr(ServerWeaponVisuals.VISUAL_TAG, false)) return true;
        return event.getEntity() instanceof ItemEntity item
            && item.getItem().getItem() instanceof ArsenalWeaponItem weapon
            && weapon.kind() == WeaponKind.LINKED_CLAWS;
    }

    public static void handleControl(ServerPlayer player, byte action, boolean active) {
        if (action == ModNetwork.FLAIL) flailControl(player, active);
        else if (action == ModNetwork.BALL_CHAIN) ballControl(player, active);
        else if (action == ModNetwork.RAM) ramControl(player);
        else if (action == ModNetwork.BULWARK_BASH) bulwarkBash(player);
        else if (action == ModNetwork.CLAW) clawAttack(player);
    }

    private static void clawAttack(ServerPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem claws) || claws.kind() != WeaponKind.CLAWS
            || !(player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked) || linked.kind() != WeaponKind.LINKED_CLAWS) return;
        double reach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        Vec3 from = player.getEyePosition(), to = from.add(player.getLookAngle().scale(reach));
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(player, from, to,
            player.getBoundingBox().expandTowards(player.getLookAngle().scale(reach)).inflate(1.0D),
            entity -> entity instanceof LivingEntity living && validTarget(player, living), reach * reach);
        player.swing(InteractionHand.OFF_HAND, true);
        long now = player.level().getGameTime();
        long previous = LAST_CLAW_OFFHAND.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        double attackSpeed = Math.max(0.1D, player.getAttributeValue(Attributes.ATTACK_SPEED));
        double cooldownTicks = 20.0D / attackSpeed;
        float strength = previous == Long.MIN_VALUE ? 1.0F
            : (float)Math.max(0.0D, Math.min(1.0D, (now - previous + 0.5D) / cooldownTicks));
        LAST_CLAW_OFFHAND.put(player.getUUID(), now);
        if (hitResult == null || !(hitResult.getEntity() instanceof LivingEntity target)) return;
        boolean pierceFrames = strength >= 0.9F
            && player.getPersistentData().getIntOr(CLAW_LAST_HAND, -1) == 0
            && player.getPersistentData().getIntOr(CLAW_LAST_TARGET, -1) == target.getId();
        if (pierceFrames) {
            target.invulnerableTime = 0;
        }
        ServerLevel level = (ServerLevel)player.level();
        float baseDamage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = baseDamage * (0.2F + strength * strength * 0.8F);
        ItemStack linkedStack = player.getOffhandItem();
        DamageSource source = player.damageSources().playerAttack(player);
        damage = EnchantmentHelper.modifyDamage(level, linkedStack, target, source, damage);
        float enchantedKnockback = EnchantmentHelper.modifyKnockback(level, linkedStack, target, source, 0.0F);
        if (target.hurtServer(level, source, damage)) {
            if (enchantedKnockback > 0.0F) {
                double yaw = Math.toRadians(player.getYRot());
                target.knockback(enchantedKnockback, Math.sin(yaw), -Math.cos(yaw));
            }
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, linkedStack);
            player.getPersistentData().putInt(CLAW_LAST_HAND, 1);
            player.getPersistentData().putInt(CLAW_LAST_TARGET, target.getId());
            player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            syncClawPair(player);
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.15F);
            if (pierceFrames) level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.PLAYERS, 0.9F, 1.35F);
        }
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer server)) return;
        pairClaws(player);
        updateBulwarkMovement(player);
        updateBall(server);
        updateRam(server);
    }

    private static void flailControl(ServerPlayer player, boolean active) {
        if (!active) {
            setServerFlailSprite(player, ItemStack.EMPTY);
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem activeWeapon
                && activeWeapon.kind() == WeaponKind.FLAIL) player.stopUsingItem();
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.FLAIL) return;
        setServerFlailSprite(player, player.getMainHandItem());
        if (!player.isUsingItem()) player.startUsingItem(InteractionHand.MAIN_HAND);
        long now = player.level().getGameTime(), previous = LAST_FLAIL.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - previous < 25) return;
        LAST_FLAIL.put(player.getUUID(), now);
        ServerLevel level = (ServerLevel) player.level();
        ItemStack flail = player.getMainHandItem();
        DamageSource source = flail.getDamageSource(player, () -> player.damageSources().playerAttack(player));
        float attackStrength = player.getAttackStrengthScale(0.5F);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0D),
                target -> validTarget(player, target) && player.hasLineOfSight(target))) {
            target.invulnerableTime = 0;
            hit |= target.hurtServer(level, source, effectiveMeleeDamage(player, target, source, attackStrength));
        }
        if (hit) flail.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.resetAttackStrengthTicker();
        level.playSound(null, player.blockPosition(), hit ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_NODAMAGE,
            SoundSource.PLAYERS, hit ? 1.0F : 0.65F, 0.82F);
        VisualEffects.flail(level, player, weapon.tier());
    }

    private static float effectiveMeleeDamage(ServerPlayer player, LivingEntity target,
            DamageSource source, float attackStrength) {
        float attributeDamage = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float scaledBase = attributeDamage * (0.2F + attackStrength * attackStrength * 0.8F);
        float enchantedDamage = EnchantmentHelper.modifyDamage((ServerLevel)player.level(),
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
        CustomModelData old = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        ArrayList<Boolean> flags = new ArrayList<>(old.flags());
        if (flags.isEmpty()) flags.add(false);
        if (flags.get(0) == active) return;
        flags.set(0, active);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(old.floats(), flags, old.strings(), old.colors()));
    }

    private static void ballControl(ServerPlayer player, boolean active) {
        if (!active) { releaseBall(player); return; }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BALL_AND_CHAIN || !player.getOffhandItem().isEmpty()) return;
        BallState state = BALLS.computeIfAbsent(player.getUUID(), id -> new BallState(player.level().getGameTime()));
        state.lastHeartbeat = player.level().getGameTime();
    }

    private static void updateBall(ServerPlayer player) {
        BallState state = BALLS.get(player.getUUID());
        if (state == null) return;
        if (state.releasing) { updateBallRelease(player, state); return; }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.BALL_AND_CHAIN || !player.getOffhandItem().isEmpty()) {
            BALLS.remove(player.getUUID()); return;
        }
        long now = player.level().getGameTime();
        if (!player.isUsingItem()) player.startUsingItem(InteractionHand.MAIN_HAND);
        if (now - state.lastHeartbeat > 3) { releaseBall(player); return; }
        int index = (int)((now - state.started) / 25);
        if (index != state.lastSwing) {
            int maxCharges = maxBallCharges(weapon.tier());
            int previousCharge = state.charge;
            state.lastSwing = index;
            state.charge = Math.min(maxCharges, state.charge + 1);
            lineAttack(player, 3.0D, 1.0F, 0.3F, false, weapon.tier());
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.65F, 0.72F);
            if (previousCharge < maxCharges && state.charge == maxCharges) {
                ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 0.55F, 1.55F);
            }
        }
        VisualEffects.ballWindup((ServerLevel)player.level(), player, weapon.tier(), state.charge);
    }

    private static void releaseBall(ServerPlayer player) {
        BallState state = BALLS.get(player.getUUID());
        if (state == null || state.releasing || state.charge <= 0) {
            BALLS.remove(player.getUUID()); return;
        }
        state.releasing = true; state.releaseTick = player.level().getGameTime();
        state.direction = player.getLookAngle().normalize();
        WeaponTier tier = ((ArsenalWeaponItem)player.getMainHandItem().getItem()).tier();
        int maxCharges = maxBallCharges(tier);
        state.distance = stopDistance((ServerLevel)player.level(), player.getEyePosition(), state.direction,
            state.charge * 4.0D);
        lineAttack(player, state.distance, ballDamageMultiplier(tier, state.charge),
            ballKnockback(tier, state.charge), state.charge >= maxCharges, tier);
        ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
            SoundSource.PLAYERS, 0.9F, 0.72F);
    }

    private static void updateBallRelease(ServerPlayer player, BallState state) {
        long age = player.level().getGameTime() - state.releaseTick;
        if (age == 8 && !state.returned) {
            state.returned = true;
            WeaponTier tier = ((ArsenalWeaponItem)player.getMainHandItem().getItem()).tier();
            lineAttack(player, state.distance, ballDamageMultiplier(tier, state.charge),
                ballKnockback(tier, state.charge), state.charge >= maxBallCharges(tier), tier);
            ((ServerLevel)player.level()).playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN,
                SoundSource.PLAYERS, 0.9F, 0.78F);
        }
        if (age >= 16) { BALLS.remove(player.getUUID()); player.stopUsingItem(); }
        else {
            WeaponTier tier = ((ArsenalWeaponItem)player.getMainHandItem().getItem()).tier();
            double progress = age / 16.0D;
            VisualEffects.ballRelease((ServerLevel)player.level(), player, tier, state, progress);
        }
    }

    private static void lineAttack(ServerPlayer player, double distance, float multiplier,
            float knockback, boolean fracture, WeaponTier tier) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition().add(0, -0.55D, 0), direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(stopDistance(level, start, direction, distance)));
        AABB box = new AABB(start, end).inflate(0.75D, 1.0D, 0.75D);
        float base = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, target -> validTarget(player, target))) {
            if (!target.getBoundingBox().inflate(0.7D, 1.0D, 0.7D).clip(start, end).isPresent()) continue;
            target.invulnerableTime = 0;
            if (target.hurtServer(level, player.damageSources().playerAttack(player), base)) {
                target.knockback(knockback, -direction.x, -direction.z);
                player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                if (fracture) fractureArmor(target, tier);
            }
        }
    }

    private static void fractureArmor(LivingEntity target, WeaponTier tier) {
        if (target instanceof Player) {
            int amp = tier.armorPiercePercent() >= 100 ? 4 : tier.armorPiercePercent() >= 75 ? 2 : tier.armorPiercePercent() >= 50 ? 1 : 0;
            target.addEffect(new MobEffectInstance(ModEffects.ARMOR_FRACTURE.getHolder().orElseThrow(), 200, amp));
            return;
        }
        var armor = target.getAttribute(Attributes.ARMOR);
        if (armor == null) return;
        AttributeModifier old = armor.getModifier(PERMANENT_FRACTURE);
        double oldAmount = old == null ? 0.0D : old.amount();
        double reduction = Math.max(2.0D, Math.max(0.0D, armor.getValue()) * 0.10D);
        armor.addOrReplacePermanentModifier(new AttributeModifier(PERMANENT_FRACTURE,
            oldAmount - reduction, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void ramControl(ServerPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.BATTERING_RAM
            || !player.getOffhandItem().isEmpty() || (!player.isCreative() && player.getFoodData().getFoodLevel() <= 6)) return;
        RAMS.computeIfAbsent(player.getUUID(), id -> new RamState(player.getYRot(), player.getXRot()))
            .lastHeartbeat = player.level().getGameTime();
    }

    private static void updateRam(ServerPlayer player) {
        RamState state = RAMS.get(player.getUUID());
        if (state == null) return;
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon) || weapon.kind() != WeaponKind.BATTERING_RAM
            || !player.getOffhandItem().isEmpty() || player.level().getGameTime() - state.lastHeartbeat > 3
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
            if (state.hit.add(target.getId()) && target.hurtServer((ServerLevel)player.level(), player.damageSources().playerAttack(player),
                    (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
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
            case IRON -> RAM_IRON;
            case DIAMOND, NETHERITE -> RAM_DIAMOND;
        });
    }

    private static TagKey<Block> ramTag(String tier) {
        return TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(ArsenalMod.MOD_ID, "battering_ram/" + tier));
    }

    private static void bulwarkBash(ServerPlayer player) {
        if (!(player.getUseItem().getItem() instanceof ArsenalShieldItem shield)
            || shield.shieldType() != ArsenalShieldItem.Type.SUN_WAR || !player.getOffhandItem().isEmpty()) return;
        long now = player.level().getGameTime(), ready = player.getPersistentData().getLongOr("ArsenalBulwarkReady", 0);
        if (now < ready) return;
        player.getPersistentData().putLong("ArsenalBulwarkReady", now + 60);
        player.getCooldowns().addCooldown(player.getMainHandItem(), 60);
        player.stopUsingItem();
        player.swing(InteractionHand.MAIN_HAND, true);
        float damage = bulwarkDamage(player);
        ServerLevel level = (ServerLevel)player.level();
        ItemStack bulwark = player.getMainHandItem();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4.0D),
                target -> validTarget(player, target) && player.distanceToSqr(target) <= 16.0D)) {
            if (target.hurtServer(level, player.damageSources().playerAttack(player), damage)) {
                target.knockback(1.4D, player.getX() - target.getX(), player.getZ() - target.getZ());
                damage(bulwark, player, 1);
                if (bulwark.isEmpty()) break;
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.0F, 0.75F);
        level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 0.9F, 0.70F);
    }

    private static void updateBulwarkMovement(Player player) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean held = player.getMainHandItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR && player.getOffhandItem().isEmpty();
        if (!held) {
            speed.removeModifier(BULWARK_SLOW);
            return;
        }
        boolean guarding = player.isUsingItem() && player.getUseItem() == player.getMainHandItem();
        speed.addOrUpdateTransientModifier(new AttributeModifier(BULWARK_SLOW, guarding ? -0.75D : -0.40D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static int maxBallCharges(WeaponTier tier) { return tier == WeaponTier.GOLD ? 2 : 3; }
    private static float ballDamageMultiplier(WeaponTier tier, int charge) {
        return tier == WeaponTier.GOLD ? 2.75F * charge / 2.0F : new float[]{0, 1.75F, 2.25F, 2.75F}[charge];
    }
    private static float ballKnockback(WeaponTier tier, int charge) {
        return tier == WeaponTier.GOLD ? 2.40F * charge / 2.0F : 0.75F + charge * 0.55F;
    }

    private static void pairClaws(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof ArsenalWeaponItem weapon && weapon.kind() == WeaponKind.CLAWS) {
            ItemStack off = player.getOffhandItem();
            if (off.isEmpty() || off.getItem() instanceof ArsenalWeaponItem linked && linked.kind() == WeaponKind.LINKED_CLAWS) {
                ItemStack desired = new ItemStack(ModItems.get(WeaponKind.LINKED_CLAWS, weapon.tier()).get());
                desired.applyComponents(main.getComponentsPatch());
                player.setItemSlot(EquipmentSlot.OFFHAND, desired);
            }
        } else if (player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked && linked.kind() == WeaponKind.LINKED_CLAWS) {
            player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static void syncClawPair(Player player) {
        if (!player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked
            && linked.kind() == WeaponKind.LINKED_CLAWS) player.getOffhandItem().applyComponents(player.getMainHandItem().getComponentsPatch());
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
        return target != player && target.isAlive() && !player.isAlliedTo(target);
    }
    private static double stopDistance(ServerLevel level, Vec3 start, Vec3 direction, double distance) {
        HitResult hit = level.clip(new net.minecraft.world.level.ClipContext(start, start.add(direction.scale(distance)),
            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, playerForClip(start, level)));
        return hit instanceof BlockHitResult block ? start.distanceTo(block.getLocation()) : distance;
    }
    private static net.minecraft.world.phys.shapes.CollisionContext playerForClip(Vec3 start, ServerLevel level) {
        return net.minecraft.world.phys.shapes.CollisionContext.empty();
    }
    private static void damage(ItemStack stack, LivingEntity owner, int amount) {
        if (amount > 0 && !stack.isEmpty()) stack.hurtAndBreak(amount, owner,
            owner.getOffhandItem() == stack ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
    }

    public static final class BallState {
        long started, lastHeartbeat, releaseTick; int lastSwing = -1, charge; boolean releasing, returned; double distance; Vec3 direction;
        BallState(long tick) { started = lastHeartbeat = tick; }
        public int charge() { return charge; }
        public double distance() { return distance; }
    }
    private static final class RamState {
        final float yaw, pitch; long lastHeartbeat; final Set<Integer> hit = new HashSet<>();
        RamState(float yaw, float pitch) { this.yaw = yaw; this.pitch = pitch; }
    }
    private record PendingMeleeAttack(int targetId, long gameTime, WeaponKind kind, boolean fullyCharged) {}
    private CombatEvents() {}
}
