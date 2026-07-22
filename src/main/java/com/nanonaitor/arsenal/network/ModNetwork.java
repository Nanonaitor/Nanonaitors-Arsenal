package com.nanonaitor.arsenal.network;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL =
        NetworkRegistry.INSTANCE.newSimpleChannel(NanonaitorsArsenal.MOD_ID);

    private ModNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(OffhandClawAttackMessage.Handler.class,
            OffhandClawAttackMessage.class, 0, Side.SERVER);
        CHANNEL.registerMessage(FlailSwingMessage.Handler.class,
            FlailSwingMessage.class, 1, Side.SERVER);
        CHANNEL.registerMessage(BatteringRamChargeMessage.Handler.class,
            BatteringRamChargeMessage.class, 2, Side.SERVER);
        CHANNEL.registerMessage(BallAndChainSwingMessage.Handler.class,
            BallAndChainSwingMessage.class, 3, Side.SERVER);
        CHANNEL.registerMessage(BallAndChainReleaseAnimationMessage.Handler.class,
            BallAndChainReleaseAnimationMessage.class, 4, Side.CLIENT);
        CHANNEL.registerMessage(FlailAnimationMessage.Handler.class,
            FlailAnimationMessage.class, 5, Side.CLIENT);
    }
}
