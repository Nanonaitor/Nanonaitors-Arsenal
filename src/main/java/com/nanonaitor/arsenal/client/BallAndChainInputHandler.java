package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ArsenalCompatManager;
import com.nanonaitor.arsenal.item.ItemBallAndChain;
import com.nanonaitor.arsenal.network.BallAndChainSwingMessage;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID, value = Side.CLIENT)
public final class BallAndChainInputHandler {
    private static final java.util.UUID USE_SPEED_UUID = java.util.UUID.fromString(
        "ddf96815-9596-4d0c-93ea-a08c00a16ae4");
    private static long lastHeartbeatTick = Long.MIN_VALUE;
    private static boolean wasSwinging;
    private static boolean guarding;

    private BallAndChainInputHandler() {}

    /**
     * Send the release on the physical mouse-up event as well as the client-tick
     * transition.  Some 1.12 combat/input mods consume the attack key transition,
     * which used to leave the server charging forever and never start the throw.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMouseRelease(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        if (key >= 0 || event.getButton() != key + 100 || mc.currentScreen != null) {
            return;
        }
        EntityPlayer player = mc.player;
        if (player != null && player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            // Own both edges: otherwise RLCombat starts an ordinary targeted
            // melee swing in parallel with Arsenal's damage-window sweeps.
            event.setCanceled(true);
            clearVanillaAttack(mc);
            if (!event.isButtonstate() && wasSwinging) {
                ModNetwork.CHANNEL.sendToServer(new BallAndChainSwingMessage(false));
                wasSwinging = false;
                lastHeartbeatTick = Long.MIN_VALUE;
            }
        }
    }

    public static boolean isAttackPhysicallyDown() {
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.inGameHasFocus || mc.currentScreen != null) return false;
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        if (key < 0) {
            int button = key + 100;
            return button >= 0 && button < org.lwjgl.input.Mouse.getButtonCount()
                && org.lwjgl.input.Mouse.isButtonDown(button);
        }
        return key > 0 && key < org.lwjgl.input.Keyboard.KEYBOARD_SIZE
            && org.lwjgl.input.Keyboard.isKeyDown(key);
    }

    private static void clearVanillaAttack(Minecraft mc) {
        net.minecraft.client.settings.KeyBinding.setKeyBindState(
            mc.gameSettings.keyBindAttack.getKeyCode(), false);
        while (mc.gameSettings.keyBindAttack.isPressed()) { /* drain queued clicks */ }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null && mc.currentScreen == null
            && mc.player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain) {
            // START prevents held-click attacks in Minecraft's tick; END also
            // covers combat mods polling the binding after that tick.
            clearVanillaAttack(mc);
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null) {
            wasSwinging = false;
            guarding = false;
            lastHeartbeatTick = Long.MIN_VALUE;
            return;
        }
        boolean holdingWeapon = player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain;
        boolean retrieving = BallAndChainAnimationHandler.isReleaseAnimationActive(player);
        boolean canGuard = holdingWeapon
            && !retrieving
            && player.getHeldItemOffhand().isEmpty()
            && minecraft.currentScreen == null
            && minecraft.gameSettings.keyBindUseItem.isKeyDown();
        if (!canGuard) {
            guarding = false;
        } else if (!guarding && !wasSwinging
            && !isAttackPhysicallyDown()
            && player.isHandActive()
            && player.getActiveHand() == EnumHand.MAIN_HAND) {
            // Once blocking begins, attack input cannot silently convert the
            // same use action into a wind-up. The use button must be released
            // before a Ball and Chain attack can begin.
            guarding = true;
        }
        boolean canSwing = holdingWeapon
            && !retrieving
            && !guarding
            && minecraft.currentScreen == null
            && isAttackPhysicallyDown();
        updateUseSpeed(player, canSwing || retrieving);
        player.getEntityData().setBoolean("ArsenalBallAndChainActive",
            holdingWeapon && (canSwing || retrieving));
        if (!canSwing) {
            if (wasSwinging) {
                ModNetwork.CHANNEL.sendToServer(new BallAndChainSwingMessage(false));
                wasSwinging = false;
                lastHeartbeatTick = Long.MIN_VALUE;
            }
            if (holdingWeapon && !retrieving && !guarding && player.isHandActive()
                && player.getActiveHand() == EnumHand.MAIN_HAND) {
                player.resetActiveHand();
            }
            return;
        }
        if (!player.isHandActive()) {
            player.setActiveHand(EnumHand.MAIN_HAND);
        }
        wasSwinging = true;
        long now = player.world.getTotalWorldTime();
        if (lastHeartbeatTick == Long.MIN_VALUE || now < lastHeartbeatTick
            || now - lastHeartbeatTick >= 2L) {
            lastHeartbeatTick = now;
            ModNetwork.CHANNEL.sendToServer(new BallAndChainSwingMessage(true));
        }
    }

    public static boolean isSwinging() {
        return wasSwinging;
    }

    public static boolean isGuardingInput() {
        return guarding;
    }

    /** Only Arsenal's server-timed sound events may represent this weapon's attacks. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void suppressRlCombatSwingSound(PlaySoundEvent event) {
        if (event.getResultSound() == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayer player = minecraft.player;
        if (player == null || !(player.getHeldItemMainhand().getItem() instanceof ItemBallAndChain)
            || minecraft.world == null) return;
        net.minecraft.client.audio.ISound playing = event.getResultSound();
        net.minecraft.util.ResourceLocation sound = playing.getSoundLocation();
        if (!ChainSwingSoundFilter.isExtraMeleeSound(sound.getResourceDomain(),
            sound.getResourcePath())) return;
        // RLCombat also schedules sounds after mouse-up, and vanilla attack
        // sounds can occur while an entity is targeted. Do not gate on input.
        // Restrict suppression to sounds originating at this player, not every
        // other player's attacks. Arsenal's namespaced timed sounds pass through.
        EntityPlayer source = minecraft.world.getClosestPlayer(playing.getXPosF(),
            playing.getYPosF(), playing.getZPosF(), 2.0D, false);
        if (source == player) {
            event.setResultSound(null);
        }
    }

    private static void updateUseSpeed(EntityPlayer player, boolean active) {
        IAttributeInstance speed = player.getEntityAttribute(
            SharedMonsterAttributes.MOVEMENT_SPEED);
        AttributeModifier old = speed.getModifier(USE_SPEED_UUID);
        if (old != null) speed.removeModifier(old);
        if (active && player.isHandActive()) {
            // Minecraft 1.12.2 multiplies movement input by 0.2 whenever an
            // item is active. This transient x5 modifier cancels only that
            // animation-state penalty; all other speed effects still apply.
            speed.applyModifier(new AttributeModifier(USE_SPEED_UUID,
                "Ball and Chain animation movement compensation", 4.0D, 2)
                .setSaved(false));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttackEntity(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote && player.getHeldItemMainhand().getItem()
            instanceof ItemBallAndChain) {
            event.setCanceled(true);
        }
    }
}
