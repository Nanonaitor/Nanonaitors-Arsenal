package com.nanonaitor.arsenal.compat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/** Uses ShieldBreak's live settings and the installed Spartan iron shield's protection. */
public final class ScimitarShieldCompat {
    private ScimitarShieldCompat() {}
    public static boolean isPair(EntityPlayer player) {
        return player.getHeldItemMainhand().getItem() instanceof com.nanonaitor.arsenal.item.ItemScimitar
            && player.getHeldItemOffhand().getItem() instanceof com.nanonaitor.arsenal.item.ItemScimitar;
    }
    public static boolean disabled(EntityPlayer player) {
        return isPair(player) && (player.getCooldownTracker().hasCooldown(player.getHeldItemMainhand().getItem())
            || player.getCooldownTracker().hasCooldown(player.getHeldItemOffhand().getItem()));
    }
    public static boolean isGuarding(EntityPlayer player) {
        return isPair(player) && !disabled(player) && player.isHandActive()
            && player.getActiveItemStack().getItem() instanceof com.nanonaitor.arsenal.item.ItemScimitar;
    }
    public static void disable(EntityPlayer player, int ticks) {
        player.getCooldownTracker().setCooldown(player.getHeldItemMainhand().getItem(), ticks);
        player.getCooldownTracker().setCooldown(player.getHeldItemOffhand().getItem(), ticks);
        player.getEntityData().setBoolean("ArsenalScimitarGuard", false);
        player.resetActiveHand();
    }
    public static int bashCooldown() {
        if (!Loader.isModLoaded("spartanshields")) return 30;
        try { return Class.forName("com.oblivioussp.spartanshields.util.ConfigHandler").getField("cooldownShieldBash").getInt(null); }
        catch (ReflectiveOperationException e) { return 30; }
    }
    public static boolean bashEnabled() {
        if (!Loader.isModLoaded("spartanshields")) return true;
        try { return !Class.forName("com.oblivioussp.spartanshields.util.ConfigHandler").getField("disableShieldBash").getBoolean(null); }
        catch (ReflectiveOperationException e) { return true; }
    }
    private static Object settings() {
        if (!Loader.isModLoaded("shieldbreak")) return null;
        try { return Class.forName("shieldbreak.handlers.ModConfig").getField("server").get(null); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("ShieldBreak configuration API changed", e); }
    }
    private static double number(Object config, String field, double fallback) {
        if (config == null) return fallback;
        try { return ((Number)config.getClass().getField(field).get(config)).doubleValue(); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    public static int raiseDelay() { return (int)number(settings(), "shieldRaiseTickDelay", 2); }
    public static void blocked(EntityPlayer player, DamageSource source, float amount) {
        if (player.world.isRemote) return;
        Object config = settings();
        Item iron = Item.REGISTRY.getObject(new ResourceLocation("spartanshields", "shield_basic_iron"));
        ItemStack reference = iron == null || iron == net.minecraft.init.Items.AIR
            ? new ItemStack(net.minecraft.init.Items.SHIELD) : new ItemStack(iron);
        double protection = Math.max(number(config,"damageMinimumThreshold",1),
            Math.min(number(config,"damageMaximumThreshold",20),reference.getMaxDamage()/number(config,"damageDurabilityScaling",100)));
        EntityLivingBase attacker = source.getTrueSource() instanceof EntityLivingBase
            ? (EntityLivingBase)source.getTrueSource() : null;
        boolean parry = player.getItemInUseMaxCount() < raiseDelay() + number(config,"parryTickRange",10);
        int cooldown = 0;
        if (attacker != null) {
            ItemStack weapon = attacker.getHeldItemMainhand();
            if (!weapon.isEmpty() && weapon.getItem().canDisableShield(weapon,reference,player,attacker)
                && player.getRNG().nextFloat() < number(config,"shieldBypassChance",0.25)
                    + EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY,weapon)*0.05)
                cooldown = (int)number(config,"shieldBypassCooldown",100);
        }
        if (cooldown == 0 && !parry && amount > protection) {
            cooldown = (int)Math.max(number(config,"cooldownTicksMinimum",5),Math.min(
                number(config,"cooldownTicksMaximum",60),(amount-protection)*number(config,"cooldownTicksScaling",10)));
        }
        if (cooldown > 0) {
            disable(player,cooldown);
            player.world.setEntityState(player,(byte)30);
        } else player.world.playSound(null,player.posX,player.posY,player.posZ,
            SoundEvents.ITEM_SHIELD_BLOCK,player.getSoundCategory(),1.0F,parry?0.8F:1.0F);
        if (attacker != null && !source.isProjectile()) {
            float power=(float)number(config,cooldown>0?"knockbackBreak":parry?"knockbackParry":"knockbackNormal",0.5);
            attacker.knockBack(player,power,player.posX-attacker.posX,player.posZ-attacker.posZ);
            if (config != null && (cooldown>0 || parry)) applyEffects(config,attacker,player,cooldown==0);
        }
    }
    private static void applyEffects(Object config, EntityLivingBase attacker, EntityPlayer defender, boolean parry) {
        try {
            String suffix=parry?"Parry":"Break";
            for (String who:new String[]{"Attacker","Defender"}) {
                Object list=config.getClass().getMethod("getEffect"+who+suffix).invoke(config);
                if (!(list instanceof Iterable)) continue;
                for(Object effect:(Iterable<?>)list) {
                    Class<?> type=effect.getClass();
                    net.minecraft.potion.Potion potion=(net.minecraft.potion.Potion)type.getMethod("getPotion").invoke(effect);
                    int duration=((Number)type.getMethod("getDuration").invoke(effect)).intValue();
                    int amp=((Number)type.getMethod("getAmplifier").invoke(effect)).intValue();
                    (who.equals("Attacker")?attacker:defender).addPotionEffect(new net.minecraft.potion.PotionEffect(potion,duration,amp));
                }
            }
        } catch(ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
