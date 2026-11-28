package com.nanonaitor.arsenal.compat;

import com.nanonaitor.arsenal.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;
import java.util.EnumMap;

/**
 * Optional JustLevelingFork or Talents integration; never alters their files or player progression.
 * Talents is a renamed JustLevelingFork (package com.seniors.talents) with the same capability
 * API, but its melee aptitude is named "attack" where JustLevelingFork's is "strength".
 */
public final class LevelRequirements {
    public static final String FILE="nanonaitors_arsenal-level-requirements.toml";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final EnumMap<WeaponTier,ForgeConfigSpec.IntValue> LEVELS=new EnumMap<>(WeaponTier.class);
    /** One supported levelling mod: its mod id, capability class and melee aptitude name. */
    private record Backend(String modId,String capabilityClass,String aptitude,String displayName){}
    private static final java.util.List<Backend> BACKENDS=java.util.List.of(
        new Backend("justlevelingfork","com.seniors.justlevelingfork.common.capability.AptitudeCapability","strength","Strength"),
        new Backend("talents","com.seniors.talents.common.capability.AptitudeCapability","attack","Attack"));
    private static Backend backend;
    private static Method getCapability,getLevel;
    private static boolean initialized,failed;
    private static final java.util.Map<Player,Long> LAST_MESSAGE=new java.util.WeakHashMap<>();
    static {
        var b=new ForgeConfigSpec.Builder();b.push("justLevelingFork");
        ENABLED=b.comment("If JustLevelingFork or Talents is installed, require Strength (JustLevelingFork) or Attack (Talents) to attack/use Arsenal weapons.",
            "False disables only Arsenal's added requirements; it does not erase independent locks configured by other mods.")
            .define("enabled",true);
        b.push("strengthByTier");
        for(var tier:WeaponTier.activeValues()) LEVELS.put(tier,b.comment(reference(tier))
            .defineInRange(tier.id,defaultLevel(tier),0,1000));
        b.pop();b.pop();SPEC=b.build();
    }
    public static int defaultLevel(WeaponTier tier) {
        if(tier.custom())return defaultLevel(tier.baseline());
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
        if(tier.custom())return "Custom material: defaults to the built-in tier of its harvest level.";
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
    public static boolean active() { return installed()!=null && ENABLED.get(); }
    /** The first installed levelling mod, or null when neither is present. */
    private static Backend installed() {
        if(backend==null)for(Backend b:BACKENDS)if(ModList.get().isLoaded(b.modId)){backend=b;break;}
        return backend;
    }
    /** Name of the aptitude shown in messages: Strength or Attack. */
    public static String aptitudeName() { var b=installed(); return b==null?"Strength":b.displayName; }
    public static int required(ItemStack stack) {
        return stack.getItem() instanceof ArsenalWeaponItem weapon ? LEVELS.get(weapon.tier()).get():0;
    }
    public static boolean meets(int current,int requirement) { return requirement<=0 || current>=requirement; }
    public static int level(Player player) {
        if(!initialized) {
            initialized=true;
            try {
                var type=Class.forName(installed().capabilityClass);
                getCapability=type.getMethod("get",Player.class);
                getLevel=type.getMethod("getAptitudeLevel",String.class);
            } catch(ReflectiveOperationException|LinkageError error) { logFailure(error); }
        }
        if(failed)return -1;
        try {
            Object data=getCapability.invoke(null,player);
            return data==null?-1:((Number)getLevel.invoke(data,installed().aptitude)).intValue();
        } catch(ReflectiveOperationException|LinkageError error) {logFailure(error);return -1;}
    }
    private static void logFailure(Throwable error) {
        if(!failed)com.mojang.logging.LogUtils.getLogger().error("Arsenal cannot read the "+aptitudeName()+" level; requirement checks fail closed. Disable integration in {} to bypass.",FILE,error);
        failed=true;
    }
    public static boolean canUse(Player player,ItemStack stack,boolean notify) {
        if(!(stack.getItem() instanceof ArsenalWeaponItem) || !active() || player.isCreative())return true;
        // Both levelling mods give fake players (machines, deployers) no levels; failing closed would lock them out.
        if(player instanceof net.minecraftforge.common.util.FakePlayer)return true;
        int needed=required(stack);
        if(needed==0)return true;
        int current=level(player);
        if(meets(current,needed))return true;
        if(notify) {
            long now=player.level().getGameTime(),last=LAST_MESSAGE.getOrDefault(player,Long.MIN_VALUE);
            if(last==Long.MIN_VALUE || now<last || now-last>=20) {
                LAST_MESSAGE.put(player,now);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(current<0
                    ? "Cannot read your "+aptitudeName()+" level yet. Check the log if this persists."
                    : "Requires "+aptitudeName()+" "+needed+" (your level: "+current+").")
                    .withStyle(net.minecraft.ChatFormatting.RED),true);
            }
        }
        return false;
    }
    private LevelRequirements() {}
}
