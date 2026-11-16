package com.nanonaitor.arsenal.data;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Per-player combat flags that must never be written to the save file.
 *
 * {@link Entity#getEntityData()} is persisted with the player, so a flag that
 * was set when the game closed (guarding, swinging, attack locks) came back on
 * the next login. This capability has no storage, so every flag starts clear
 * on login, respawn and dimension reload, separately on client and server.
 */
@Mod.EventBusSubscriber(modid = NanonaitorsArsenal.MOD_ID)
public final class TransientData {
    private static final ResourceLocation KEY =
        new ResourceLocation(NanonaitorsArsenal.MOD_ID, "transient_combat_state");
    /** Flags that versions up to 2.0.19 stored in the saved player data. */
    private static final String[] LEGACY_SAVED_KEYS = {
        "ArsenalBallAndChainActive", "ArsenalBallWindBoost", "ArsenalBulwarkMenuGuard",
        "ArsenalBulwarkBashReady", "ArsenalDoubleBladeReflecting", "ArsenalFlailActive",
        "ArsenalMorningCharging", "ArsenalMorningChargeVisual", "ArsenalScimitarGuard",
        "ArsenalShieldAttackLock", "ArsenalTartsyInvulnerableUntil",
        "ArsenalTartsyCriticalReady", "ArsenalElectricEffectTick", "ArsenalSrpRamRage",
        "ArsenalBladeStaffCothCleanseRefresh"
    };

    @CapabilityInject(Holder.class)
    private static Capability<Holder> CAPABILITY = null;

    private TransientData() {}

    /** Call once during pre-initialization. */
    public static void register() {
        CapabilityManager.INSTANCE.register(Holder.class, new Capability.IStorage<Holder>() {
            @Override
            public NBTBase writeNBT(Capability<Holder> capability, Holder instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<Holder> capability, Holder instance, EnumFacing side,
                                NBTBase nbt) {}
        }, Holder::new);
    }

    /**
     * Returns the unsaved flag compound for a player. Other entities get an
     * empty throwaway compound, so reads are false/zero and writes are ignored.
     */
    public static NBTTagCompound of(Entity entity) {
        Holder holder = entity == null || CAPABILITY == null ? null
            : entity.getCapability(CAPABILITY, null);
        return holder == null ? new NBTTagCompound() : holder.data;
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityPlayer) event.addCapability(KEY, new Provider());
    }

    /** Older versions saved these flags; drop them so they cannot linger in player files. */
    @SubscribeEvent
    public static void removeLegacySavedFlags(PlayerEvent.PlayerLoggedInEvent event) {
        NBTTagCompound saved = event.player.getEntityData();
        for (String key : LEGACY_SAVED_KEYS) saved.removeTag(key);
    }

    public static final class Holder {
        private final NBTTagCompound data = new NBTTagCompound();
    }

    private static final class Provider implements ICapabilityProvider {
        private final Holder holder = new Holder();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CAPABILITY;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == CAPABILITY ? CAPABILITY.cast(holder) : null;
        }
    }
}
