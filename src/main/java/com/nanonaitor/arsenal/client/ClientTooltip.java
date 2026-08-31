package com.nanonaitor.arsenal.client;

import net.minecraft.client.Minecraft;

/** Client-only keyboard state kept out of tooltip method signatures for dedicated-server safety. */
public final class ClientTooltip {
    public static boolean expanded() { return Minecraft.getInstance().options.keyShift.isDown(); }
    private ClientTooltip() {}
}
