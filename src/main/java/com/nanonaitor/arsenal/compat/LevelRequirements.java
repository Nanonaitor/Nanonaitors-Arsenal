package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;
import java.util.EnumMap;

/** Optional JustLevelingFork integration; never alters its files or player progression. */
public final class LevelRequirements {
    public static final String FILE="nanonaitors_arsenal-level-requirements.toml";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final EnumMap<WeaponTier,ForgeConfigSpec.IntValue> LEVELS=new EnumMap<>(WeaponTier.class);
    private static Method getCapability,getLevel;
    private static boolean initialized,failed;
    private static final java.util.Map<Player,Long> LAST_MESSAGE=new java.util.WeakHashMap<>();
    static {
        var b=new ForgeConfigSpec.Builder();b.push("justLevelingFork");
        ENABLED=b.comment("If JustLevelingFork is installed, require Strength to attack/use Arsenal weapons.",
            "False disables only Arsenal's added requirements; it does not erase independent locks configured by other mods.")
            .define("enabled",true);
        b.push("strengthByTier");
        for(var tier:WeaponTier.values()) LEVELS.put(tier,b.comment(reference(tier))
            .defineInRange(tier.id,defaultLevel(tier),0,1000));
        b.pop();b.pop();SPEC=b.build();
    }
    public static int defaultLevel(WeaponTier tier) {
        return switch(tier) {
            case WOOD,STONE->1;
            case COPPER,GOLD->3;
            case BRONZE,UMBRIUM->4;
            case IRON->8;
            case SILVER,STEEL,DESERT_MYRMEX,JUNGLE_MYRMEX->12;
            case DESERT_VENOM,JUNGLE_VENOM->14;
            case DIAMOND->16;
            case NETHERITE->19;
            case SENTIENT->30;
            default->25;
        };
    }
    public static String reference(WeaponTier tier) {
        return switch(tier) {
            case COPPER,SILVER,BRONZE->"Matches Restless Horizons' Spartan Weaponry "+tier.id+" weapons.";
            case STEEL->"No exact configured steel weapon: default 12 interpolates iron 8 and diamond 16.";
            case LIVING,SENTIENT->"No exact configured parasite equivalent: editable endgame fallback (living 25, sentient 30).";
            case UMBRIUM->"Matches Defiled Lands Preborn umbrium sword.";
            case DESERT_MYRMEX,JUNGLE_MYRMEX,DESERT_VENOM,JUNGLE_VENOM->"Matches configured chitin (12) / stinger (14) weapons.";
            default->"Matches Restless Horizons' vanilla or Ice & Fire sword requirement; 0 disables this tier's Arsenal lock.";
        };
    }
    public static boolean enabledFor(boolean installed,boolean enabled) { return installed && enabled; }
    public static boolean active() { return ModList.get().isLoaded("justlevelingfork") && ENABLED.get(); }
    public static int required(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon ? LEVELS.get(weapon.tier()).get():0;
    }
    public static boolean meets(int current,int requirement) { return requirement<=0 || current>=requirement; }
    public static int level(Player player) {
        if(!initialized) {
            initialized=true;
            try {
                var type=Class.forName("com.seniors.justlevelingfork.common.capability.AptitudeCapability");
                getCapability=type.getMethod("get",Player.class);
                getLevel=type.getMethod("getAptitudeLevel",String.class);
            } catch(ReflectiveOperationException|LinkageError error) { logFailure(error); }
        }
        if(failed)return -1;
        try {
            Object data=getCapability.invoke(null,player);
            return data==null?-1:((Number)getLevel.invoke(data,"strength")).intValue();
        } catch(ReflectiveOperationException|LinkageError error) {logFailure(error);return -1;}
    }
    private static void logFailure(Throwable error) {
        if(!failed)com.mojang.logging.LogUtils.getLogger().error("Arsenal cannot read JustLevelingFork Strength; requirement checks fail closed. Disable integration in {} to bypass.",FILE,error);
        failed=true;
    }
    public static boolean canUse(Player player,ItemStack stack,boolean notify) {
        if(!(stack.getItem() instanceof ArsenalWeaponItem) || !active() || player.isCreative())return true;
        int needed=required(stack);
        if(needed==0)return true;
        int current=level(player);
        if(meets(current,needed))return true;
        if(notify) {
            long now=player.level().getGameTime(),last=LAST_MESSAGE.getOrDefault(player,Long.MIN_VALUE);
            if(last==Long.MIN_VALUE || now<last || now-last>=20) {
                LAST_MESSAGE.put(player,now);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(current<0
                    ? "Cannot read your Strength level yet. Check the log if this persists."
                    : "Requires Strength "+needed+" (your level: "+current+").")
                    .withStyle(net.minecraft.ChatFormatting.RED),true);
            }
        }
        return false;
    }
    private LevelRequirements() {}
}
