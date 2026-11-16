package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

/** Client input helpers shared by the weapon input handlers. */
@SideOnly(Side.CLIENT)
final class ClientInput {
    private ClientInput() {}

    /**
     * Releases the attack key and discards queued clicks. A canceled MouseEvent
     * never delivers vanilla's key-up, so the latch must be cleared by hand.
     */
    static void clearAttackKey(Minecraft mc) {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        while (mc.gameSettings.keyBindAttack.isPressed()) { /* drain queued clicks */ }
    }

    /**
     * Whether the use key is held, also reading a mouse-bound key directly: a
     * canceled MouseEvent can leave KeyBinding#isKeyDown false while the
     * physical button is still down.
     */
    static boolean isUseKeyPhysicallyDown(Minecraft mc) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown()) return true;
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        int mouseButton = keyCode + 100;
        return keyCode < 0 && mouseButton >= 0
            && mouseButton < Mouse.getButtonCount() && Mouse.isButtonDown(mouseButton);
    }

    /**
     * Hides the client player's offhand item from RLCombat's NORMAL-priority
     * mouse listener and puts it back afterwards. One instance per call site.
     */
    static final class HiddenOffhand {
        private EntityPlayerSP player;
        private ItemStack hidden = ItemStack.EMPTY;

        void hide(EntityPlayerSP target) {
            restore();
            player = target;
            hidden = target.getHeldItemOffhand();
            target.inventory.offHandInventory.set(0, ItemStack.EMPTY);
        }

        void restore() {
            if (player != null && !hidden.isEmpty()) {
                player.inventory.offHandInventory.set(0, hidden);
            }
            player = null;
            hidden = ItemStack.EMPTY;
        }
    }
}
