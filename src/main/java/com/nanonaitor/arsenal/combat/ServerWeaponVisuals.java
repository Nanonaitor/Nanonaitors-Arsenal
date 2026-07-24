package com.nanonaitor.arsenal.combat;

import com.nanonaitor.arsenal.item.ArsenalWeaponItem;
import com.nanonaitor.arsenal.item.WeaponKind;
import com.nanonaitor.arsenal.item.WeaponTier;
import com.nanonaitor.arsenal.registry.ModItems;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ServerWeaponVisuals {
    public static final String VISUAL_TAG = "ArsenalTransientWeaponVisual";
    private static final int LINKS = 16;
    private static final Map<UUID, Rig> RIGS = new HashMap<>();
    private static final Map<UUID, Long> FLAIL_UNTIL = new HashMap<>();

    public static void startFlail(ServerPlayer player) {
        FLAIL_UNTIL.put(player.getUUID(), player.level().getGameTime() + 3L);
    }
    public static void stopFlail(ServerPlayer player) {
        FLAIL_UNTIL.remove(player.getUUID());
        clear(player);
    }
    public static void tick(ServerPlayer player) {
        Long until = FLAIL_UNTIL.get(player.getUUID());
        if (until == null) return;
        if (player.level().getGameTime() > until || !player.isAlive()
            || !(player.getMainHandItem().getItem() instanceof ArsenalWeaponItem weapon)
            || weapon.kind() != WeaponKind.FLAIL) {
            FLAIL_UNTIL.remove(player.getUUID()); clear(player); return;
        }
        double angle = (player.level().getGameTime() % 25L) / 25.0D * Math.PI * 2.0D;
        Vec3 anchor = player.position().add(0.32D, 1.25D, 0.0D);
        Vec3 ball = player.position().add(Math.cos(angle) * 4.0D, 1.05D, Math.sin(angle) * 4.0D);
        show(player, weapon.tier(), anchor, ball);
    }
    public static void showBallWindup(ServerPlayer player, WeaponTier tier, long started) {
        double angle = (player.level().getGameTime() - started) / 25.0D * Math.PI * 2.0D;
        Vec3 look = horizontalLook(player);
        Vec3 anchor = player.position().add(0.32D, 1.25D, 0.12D);
        Vec3 ball = anchor.add(look.scale(0.75D + Math.cos(angle) * 0.45D)).add(0, Math.sin(angle) * 0.72D, 0);
        show(player, tier, anchor, ball);
    }
    public static void showBallRelease(ServerPlayer player, WeaponTier tier, Vec3 direction,
            double maxDistance, double progress) {
        Vec3 anchor = player.position().add(0.32D, 1.25D, 0.12D);
        Vec3 ball = anchor.add(direction.scale(maxDistance * Math.sin(progress * Math.PI)));
        show(player, tier, anchor, ball);
    }
    public static void clear(ServerPlayer player) {
        Rig rig = RIGS.remove(player.getUUID());
        if (rig != null) rig.discard();
    }
    private static void show(ServerPlayer player, WeaponTier tier, Vec3 anchor, Vec3 ball) {
        ServerLevel level = (ServerLevel)player.level();
        Rig rig = RIGS.get(player.getUUID());
        if (rig == null || rig.level != level || rig.ballTier != tier || !rig.alive()) {
            if (rig != null) rig.discard();
            rig = new Rig(level, tier, anchor);
            RIGS.put(player.getUUID(), rig);
        }
        rig.update(anchor, ball);
    }
    private static Vec3 horizontalLook(ServerPlayer player) {
        double yaw = Math.toRadians(player.getYRot());
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }
    private static ItemStack ball(WeaponTier tier) {
        return new ItemStack(ModItems.BALL_VISUALS.get(tier).get());
    }
    private static final class Rig {
        final ServerLevel level; final WeaponTier ballTier;
        final List<Display.ItemDisplay> links = new ArrayList<>();
        final Display.ItemDisplay ball;
        Rig(ServerLevel level, WeaponTier tier, Vec3 origin) {
            this.level = level; this.ballTier = tier;
            for (int i = 0; i < LINKS; i++) {
                links.add(spawn(level, new ItemStack((i & 1) == 0
                    ? ModItems.CHAIN_LINK_FLAT.get() : ModItems.CHAIN_LINK_UPRIGHT.get()), origin));
            }
            ball = spawn(level, ball(tier), origin);
        }
        void update(Vec3 anchor, Vec3 end) {
            Vec3 delta = end.subtract(anchor); double length = delta.length();
            int used = Math.max(1, Math.min(LINKS, (int)Math.ceil(length / 0.28D)));
            float yaw = (float)Math.toDegrees(Math.atan2(delta.x, delta.z));
            float pitch = (float)-Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
            for (int i = 0; i < LINKS; i++) {
                Display.ItemDisplay link = links.get(i);
                if (i >= used) { link.setInvisible(true); continue; }
                link.setInvisible(false); double t = (i + 1.0D) / (used + 1.0D);
                Vec3 point = anchor.add(delta.scale(t));
                link.setPos(point.x, point.y, point.z); link.setYRot(yaw); link.setXRot(pitch);
            }
            ball.setPos(end.x, end.y, end.z);
        }
        boolean alive() { return ball.isAlive() && links.stream().allMatch(Display.ItemDisplay::isAlive); }
        void discard() {
            for (Display.ItemDisplay link : links) {
                link.setInvisible(true);
                link.getSlot(0).set(ItemStack.EMPTY);
                link.discard();
            }
            ball.setInvisible(true);
            ball.getSlot(0).set(ItemStack.EMPTY);
            ball.discard();
        }
        static Display.ItemDisplay spawn(ServerLevel level, ItemStack stack, Vec3 position) {
            Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            display.getSlot(0).set(stack); display.setNoGravity(true); display.setInvulnerable(true); display.setSilent(true);
            display.getPersistentData().putBoolean(VISUAL_TAG, true); display.setPos(position.x, position.y, position.z);
            level.addFreshEntity(display); return display;
        }
    }
    private ServerWeaponVisuals() {}
}
