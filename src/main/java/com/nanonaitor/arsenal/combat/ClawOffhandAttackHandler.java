package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.item.ItemClaws;
import com.nanonaitor.arsenal.network.ModNetwork;
import com.nanonaitor.arsenal.network.OffhandClawAttackMessage;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class ClawOffhandAttackHandler {
    private static final Map<EntityPlayer, Long> LAST_ATTACK_TICK = new WeakHashMap<>();

    private ClawOffhandAttackHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack main = player.getHeldItemMainhand();
        if (event.getHand() != EnumHand.MAIN_HAND
            || !(main.getItem() instanceof ItemClaws)
            || !(event.getTarget() instanceof EntityLivingBase)) {
            return;
        }
        ItemClaws claws = (ItemClaws) main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) {
            return;
        }

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        if (player.world.isRemote) {
            player.swingArm(EnumHand.OFF_HAND);
            ModNetwork.CHANNEL.sendToServer(new OffhandClawAttackMessage(
                event.getTarget().getEntityId()));
        }
    }

    public static void tryServerAttack(EntityPlayer player, EntityLivingBase target) {
        ItemStack main = player.getHeldItemMainhand();
        if (!(main.getItem() instanceof ItemClaws)) {
            return;
        }
        ItemClaws claws = (ItemClaws) main.getItem();
        if (!ClawPairHandler.hasMatchingLinkedClaw(player, claws)) {
            return;
        }
        long now = player.world.getTotalWorldTime();
        long last = LAST_ATTACK_TICK.containsKey(player) ? LAST_ATTACK_TICK.get(player) : Long.MIN_VALUE;
        double cooldownTicks = 20.0D / claws.getDisplayedAttackSpeed();
        float strength = last == Long.MIN_VALUE ? 1.0F
            : MathHelper.clamp((float) ((now - last + 0.5D) / cooldownTicks), 0.0F, 1.0F);
        LAST_ATTACK_TICK.put(player, now);

        boolean fullyCharged = strength >= 0.95F;
        boolean canPierce = fullyCharged && claws.getLastConfirmedHand(main) == 0;
        int previousResistance = target.hurtResistantTime;
        if (canPierce) {
            target.hurtResistantTime = 0;
        }

        float baseDamage = (float) player.getEntityAttribute(
            SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        float enchantmentDamage = EnchantmentHelper.getModifierForCreature(
            main, target.getCreatureAttribute());
        float damage = baseDamage * (0.2F + strength * strength * 0.8F)
            + enchantmentDamage * strength;

        boolean hit = target.attackEntityFrom(DamageSource.causePlayerDamage(player), damage);
        if (!hit) {
            target.hurtResistantTime = previousResistance;
            return;
        }

        // Broadcast the offhand animation only after damage is resolved. RLCombat's
        // default weakerOffhand rule keys off the active swing hand and would
        // otherwise halve this paired-weapon attack.
        player.swingArm(EnumHand.OFF_HAND);
        claws.confirmHand(main, 1);
        main.damageItem(1, player);
        player.addExhaustion(0.1F);

        int fireAspect = EnchantmentHelper.getFireAspectModifier(player);
        if (fireAspect > 0) {
            target.setFire(fireAspect * 4);
        }
        int knockback = EnchantmentHelper.getKnockbackModifier(player);
        if (knockback > 0) {
            target.knockBack(player, knockback * 0.5F,
                MathHelper.sin(player.rotationYaw * 0.017453292F),
                -MathHelper.cos(player.rotationYaw * 0.017453292F));
        }
        EnchantmentHelper.applyThornEnchantments(target, player);
        EnchantmentHelper.applyArthropodEnchantments(player, target);

        SoundEvent sound = SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP;
        float pitch = fullyCharged ? 1.1F : 1.35F;
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            serverPlayer.connection.sendPacket(new SPacketSoundEffect(sound,
                player.getSoundCategory(), target.posX, target.posY, target.posZ,
                1.0F, pitch));
            player.world.playSound(player, target.posX, target.posY, target.posZ,
                sound, player.getSoundCategory(), 1.0F, pitch);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        ItemStack main = player.getHeldItemMainhand();
        if (!player.world.isRemote || event.getHand() != EnumHand.MAIN_HAND
            || !(main.getItem() instanceof ItemClaws)) {
            return;
        }
        ItemClaws claws = (ItemClaws) main.getItem();
        if (ClawPairHandler.hasMatchingLinkedClaw(player, claws)) {
            player.swingArm(EnumHand.OFF_HAND);
        }
    }
}
