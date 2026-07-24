package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.item.ArsenalShieldItem;
import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.HitResult;

public final class ClientControls {
    private static boolean ballWasDown, flailWasDown;
    private static long lastFlailHeartbeat = Long.MIN_VALUE, lastHeartbeat = Long.MIN_VALUE;
    private static boolean ramLocked;
    private static float lockedYaw, lockedPitch;
    private static long flailVisualUntil = Long.MIN_VALUE, ballStarted = Long.MIN_VALUE,
        ballReleaseStarted = Long.MIN_VALUE;
    private static int releasedCharge;
    private static double releasedDistance;
    private static ItemStack activeFlailSprite = ItemStack.EMPTY, activeBallSprite = ItemStack.EMPTY;

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientControls::tick);
        InputEvent.InteractionKeyMappingTriggered.BUS.addListener(ClientControls::interaction);
        net.minecraftforge.client.event.RenderLivingEvent.Post.BUS.addListener(ClientWeaponRenderer::render);
        net.minecraftforge.client.event.RenderHandEvent.BUS.addListener(ClientWeaponRenderer::renderFirstPerson);
    }

    private static void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) { clearFlailSprite(); clearBallSprite(); ballWasDown = false; flailWasDown = false; ramLocked = false; return; }
        boolean attack = minecraft.screen == null && minecraft.options.keyAttack.isDown();
        if (attack && player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalShieldItem shield
            && shield.shieldType() == ArsenalShieldItem.Type.SUN_WAR) {
            ModNetwork.send(ModNetwork.BULWARK_BASH, true);
        }
        if (!(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)) {
            if (ballWasDown) ModNetwork.send(ModNetwork.BALL_CHAIN, false);
            if (flailWasDown) ModNetwork.send(ModNetwork.FLAIL, false);
            clearFlailSprite(); clearBallSprite();
            ballWasDown = false; flailWasDown = false; ramLocked = false; return;
        }
        long now = player.level().getGameTime();
        boolean emptyOffhand = player.getOffhandItem().isEmpty();
        boolean flailDown = weapon.kind() == WeaponKind.FLAIL && attack;
        updateFlailSprite(player.getMainHandItem(), flailDown);
        if (flailDown && !player.isUsingItem()) player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (flailDown && (lastFlailHeartbeat == Long.MIN_VALUE || now < lastFlailHeartbeat
            || now - lastFlailHeartbeat >= 2)) {
            lastFlailHeartbeat = now;
            flailVisualUntil = now + 3;
            ModNetwork.send(ModNetwork.FLAIL, true);
        }
        if (!flailDown && flailWasDown) {
            ModNetwork.send(ModNetwork.FLAIL, false);
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof ArsenalWeaponItem active
                && active.kind() == WeaponKind.FLAIL) player.stopUsingItem();
        }
        flailWasDown = flailDown;
        if (weapon.kind() == WeaponKind.BALL_AND_CHAIN) {
            boolean releasing = within(now, ballReleaseStarted, 16L);
            boolean down = attack && emptyOffhand && !releasing;
            if (down && !ballWasDown) ballStarted = now;
            if (down && (lastHeartbeat == Long.MIN_VALUE || now < lastHeartbeat || now - lastHeartbeat >= 2)) {
                lastHeartbeat = now; ModNetwork.send(ModNetwork.BALL_CHAIN, true);
            }
            if (!down && ballWasDown) {
                int maxCharges = weapon.tier() == com.nanonaitor.arsenal.item.WeaponTier.GOLD ? 2 : 3;
                releasedCharge = Math.max(1, Math.min(maxCharges, (int)((now - ballStarted) / 25) + 1));
                releasedDistance = releasedCharge * 4.0D;
                ballReleaseStarted = now;
                ModNetwork.send(ModNetwork.BALL_CHAIN, false);
            }
            updateBallSprite(player.getMainHandItem(), down || within(now, ballReleaseStarted, 16L));
            ballWasDown = down;
        } else if (ballWasDown) {
            ModNetwork.send(ModNetwork.BALL_CHAIN, false); ballWasDown = false; clearBallSprite();
        } else clearBallSprite();
        if (weapon.kind() == WeaponKind.BATTERING_RAM) {
            boolean charging = attack && emptyOffhand && (player.isCreative() || player.getFoodData().getFoodLevel() > 6);
            if (charging) {
                if (!ramLocked) { ramLocked = true; lockedYaw = player.getYRot(); lockedPitch = player.getXRot(); }
                player.setYRot(lockedYaw); player.yRotO = lockedYaw; player.setXRot(lockedPitch); player.xRotO = lockedPitch;
                if (lastHeartbeat == Long.MIN_VALUE || now < lastHeartbeat || now - lastHeartbeat >= 2) {
                    lastHeartbeat = now; ModNetwork.send(ModNetwork.RAM, true);
                }
            } else ramLocked = false;
        } else ramLocked = false;
    }
    private static boolean interaction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.isAttack() && player != null && minecraft.screen == null
            && player.getMainHandItem().getItem() instanceof ArsenalWeaponItem held
            && (held.kind() == WeaponKind.FLAIL || held.kind() == WeaponKind.BATTERING_RAM)) {
            // These held attacks are driven continuously from tick(). Suppress vanilla's
            // competing hand swing and block-mining animation; the Ram crushes blocks
            // through its forward path logic rather than striking each block normally.
            event.setSwingHand(false);
            return true;
        }
        if ((!event.isAttack() && !event.isUseItem()) || player == null || minecraft.screen != null
            || !(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.CLAWS
            || !(player.getOffhandItem().getItem() instanceof ArsenalWeaponItem linked)
            || linked.kind() != WeaponKind.LINKED_CLAWS) return false;
        if (event.isAttack()) {
            player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.45F, 1.35F);
            return false;
        }
        if (minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.BLOCK) return false;
        player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.45F, 1.20F);
        event.setSwingHand(false);
        player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
        ModNetwork.send(ModNetwork.CLAW, true);
        return true;
    }
    static boolean flailVisual(long now) { return now <= flailVisualUntil; }
    static boolean ballWindup(long now) { return ballWasDown && !within(now, ballReleaseStarted, 16L); }
    static long ballStarted() { return ballStarted; }
    static boolean ballRelease(long now) { return within(now, ballReleaseStarted, 16L); }
    static long ballReleaseStarted() { return ballReleaseStarted; }
    static int releasedCharge() { return releasedCharge; }
    static double releasedDistance() { return releasedDistance; }
    private static boolean within(long now, long started, long duration) {
        return started != Long.MIN_VALUE && now >= started && now - started < duration;
    }
    private static void updateFlailSprite(ItemStack stack, boolean active) {
        if (!active) { clearFlailSprite(); return; }
        if (activeFlailSprite != stack) {
            clearFlailSprite();
            activeFlailSprite = stack;
        }
        setFlailFlag(stack, true);
    }
    private static void clearFlailSprite() {
        if (!activeFlailSprite.isEmpty()) setFlailFlag(activeFlailSprite, false);
        activeFlailSprite = ItemStack.EMPTY;
    }
    private static void updateBallSprite(ItemStack stack, boolean active) {
        if (!active) { clearBallSprite(); return; }
        if (activeBallSprite != stack) {
            clearBallSprite();
            activeBallSprite = stack;
        }
        setFlailFlag(stack, true);
    }
    private static void clearBallSprite() {
        if (!activeBallSprite.isEmpty()) setFlailFlag(activeBallSprite, false);
        activeBallSprite = ItemStack.EMPTY;
    }
    private static void setFlailFlag(ItemStack stack, boolean active) {
        CustomModelData old = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.EMPTY);
        java.util.ArrayList<Boolean> flags = new java.util.ArrayList<>(old.flags());
        if (flags.isEmpty()) flags.add(false);
        if (flags.get(0) == active) return;
        flags.set(0, active);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
            new CustomModelData(old.floats(), flags, old.strings(), old.colors()));
    }
    private ClientControls() {}
}
