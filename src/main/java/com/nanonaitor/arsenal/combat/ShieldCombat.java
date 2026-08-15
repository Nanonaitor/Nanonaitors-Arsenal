package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemArsenalShield;
import com.nanonaitor.arsenal.item.ItemSunWarBulwark;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ShieldCombat {
    private static final String BASH_READY = "ArsenalBulwarkBashReady";
    private static final UUID MOVEMENT_UUID = UUID.fromString("f223d9ba-b0ca-4ed4-bb87-58709fd6ea1e");
    private static final double VANILLA_USE_FACTOR = 0.20D;
    private static final double BULWARK_PASSIVE_REDUCTION = 0.15D;
    private static final int BASH_COOLDOWN_TICKS = 60;
    private static final Map<EntityPlayer, PendingBulwarkAttack> PENDING_ATTACKS =
        new WeakHashMap<>();

    private ShieldCombat() {}

    public static boolean isGuarding(EntityPlayer player, Class<? extends ItemArsenalShield> type) {
        return player.isHandActive() && type.isInstance(player.getActiveItemStack().getItem());
    }

    public static boolean isBulwarkReady(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark
            && player.getHeldItemOffhand().isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        DamageSource source = event.getSource();

        if (isGuarding(player, ItemSunWarBulwark.class) && isBulwarkReady(player)
            && isBulwarkCombatDamage(source)) {
            event.setCanceled(true);
            player.getHeldItemMainhand().damageItem(1, player);
            playBlock(player, 0.65F);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDisabledBulwarkAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!(player.getHeldItemMainhand().getItem() instanceof ItemSunWarBulwark)) return;
        if (!player.getHeldItemOffhand().isEmpty()) {
            event.setCanceled(true);
            return;
        }
        if (!player.world.isRemote && event.getTarget() instanceof EntityLivingBase) {
            PENDING_ATTACKS.put(player, new PendingBulwarkAttack(
                event.getTarget().getEntityId(), player.world.getTotalWorldTime(),
                attackChargeMultiplier(player)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (source.getTrueSource() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) source.getTrueSource();
            if (isBulwarkReady(attacker)) {
                PendingBulwarkAttack pending = PENDING_ATTACKS.remove(attacker);
                float multiplier = pending != null
                    && pending.targetId == event.getEntityLiving().getEntityId()
                    && attacker.world.getTotalWorldTime() - pending.worldTime <= 1L
                    ? pending.chargeMultiplier : attackChargeMultiplier(attacker);
                event.setAmount(armorScaledDamage(attacker) * multiplier);
            }
        }

        if (!(event.getEntityLiving() instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (isBulwarkReady(player) && !isVoid(source)) {
            event.setAmount(event.getAmount() * (float)(1.0D - BULWARK_PASSIVE_REDUCTION));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (event.phase == TickEvent.Phase.START) updateBulwarkMovement(player);
    }

    public static void bash(EntityPlayer player) {
        if (player.world.isRemote || !isBulwarkReady(player)
            || !isGuarding(player, ItemSunWarBulwark.class)) return;
        long now = player.world.getTotalWorldTime();
        NBTTagCompound data = player.getEntityData();
        if (now < data.getLong(BASH_READY)) return;
        data.setLong(BASH_READY, now + BASH_COOLDOWN_TICKS);

        AxisAlignedBB area = player.getEntityBoundingBox().grow(4.0D);
        List<EntityLivingBase> targets = player.world.getEntitiesWithinAABB(EntityLivingBase.class, area,
            // MmmMmmMmmMmm's dummy reports isEntityAlive() as false while it
            // is still a valid damage target. Match the proven Ram behavior.
            target -> target != player && !target.isDead && !player.isOnSameTeam(target));
        float damage = armorScaledDamage(player) * attackChargeMultiplier(player);
        for (EntityLivingBase target : targets) {
            if (player.getDistanceSq(target) > 16.0D) continue;
            if (target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage)) {
                player.getHeldItemMainhand().damageItem(1, player);
                target.knockBack(player, 1.4F, player.posX - target.posX, player.posZ - target.posZ);
            }
        }
        player.getCooldownTracker().setCooldown(player.getHeldItemMainhand().getItem(), BASH_COOLDOWN_TICKS);
        player.resetActiveHand();
        player.swingArm(EnumHand.MAIN_HAND);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.2F, 0.65F);
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ENTITY_IRONGOLEM_ATTACK, SoundCategory.PLAYERS, 0.9F, 0.70F);
        player.resetCooldown();
    }

    private static float armorScaledDamage(EntityPlayer player) {
        IAttributeInstance armor = player.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        // The live attribute includes armor items, potion modifiers, Baubles and
        // quality-system modifiers rather than only counting worn armor pieces.
        return 1.0F + (float) Math.max(0.0D, armor == null
            ? player.getTotalArmorValue() : armor.getAttributeValue());
    }

    private static float attackChargeMultiplier(EntityPlayer player) {
        float strength = player.getCooledAttackStrength(0.5F);
        return 0.2F + strength * strength * 0.8F;
    }

    private static void updateBulwarkMovement(EntityPlayer player) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier old = speed.getModifier(MOVEMENT_UUID);
        if (old != null) speed.removeModifier(old);
        if (isBulwarkReady(player)) {
            // Active item use multiplies movement input by 0.2 in 1.12.2. A +25%
            // attribute modifier therefore produces the intended 25% final speed.
            double amount = isGuarding(player, ItemSunWarBulwark.class)
                ? (0.25D / VANILLA_USE_FACTOR) - 1.0D : -0.40D;
            speed.applyModifier(new AttributeModifier(MOVEMENT_UUID,
                "Sun-War Bulwark movement", amount, 2).setSaved(false));
        }
    }

    private static ItemStack findEquipped(EntityPlayer player, Class<? extends ItemArsenalShield> type) {
        if (type.isInstance(player.getHeldItemOffhand().getItem())) return player.getHeldItemOffhand();
        if (type.isInstance(player.getHeldItemMainhand().getItem())) return player.getHeldItemMainhand();
        return ItemStack.EMPTY;
    }

    private static void playBlock(EntityPlayer player, float pitch) {
        player.world.playSound(null, player.posX, player.posY, player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, pitch);
    }

    private static boolean isBulwarkCombatDamage(DamageSource source) {
        if (isVoid(source) || source.isFireDamage()) return false;
        String type = source.damageType.toLowerCase(Locale.ROOT);
        if ("fall".equals(type) || "drown".equals(type) || "starve".equals(type)
            || "cactus".equals(type) || "inwall".equals(type) || "hotfloor".equals(type)) return false;
        return source.getTrueSource() != null || source.getImmediateSource() != null
            || source.isProjectile() || source.isExplosion() || source.isMagicDamage();
    }

    private static boolean isVoid(DamageSource source) {
        return source == DamageSource.OUT_OF_WORLD || "outOfWorld".equals(source.damageType);
    }

    private static final class PendingBulwarkAttack {
        private final int targetId;
        private final long worldTime;
        private final float chargeMultiplier;

        private PendingBulwarkAttack(int targetId, long worldTime,
                                     float chargeMultiplier) {
            this.targetId = targetId;
            this.worldTime = worldTime;
            this.chargeMultiplier = chargeMultiplier;
        }
    }
}
