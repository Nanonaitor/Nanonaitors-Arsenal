package com.nanonaitor.arsenal.combat;

public final class ClawBalanceRegressionTest {
    private static int checks;
    private static void check(boolean result) {
        checks++;if(!result)throw new AssertionError("Claw balance check "+checks);
    }
    public static void main(String[] args) {
        PairedClawHitLimiter<Object,Object> limit=new PairedClawHitLimiter<>();
        Object player=new Object(), otherPlayer=new Object(), target=new Object(),otherTarget=new Object();
        check(limit.ready(player,target,100));
        // A miss does not reserve a slot.
        check(limit.ready(player,target,100));
        limit.confirmed(player,target,100);
        for(long tick=100;tick<104;tick++)check(!limit.ready(player,target,tick));
        check(limit.ready(player,target,104));
        check(limit.ready(player,otherTarget,100));
        check(limit.ready(otherPlayer,target,100));
        limit.confirmed(player,target,104); // Other hand uses the same window.
        check(!limit.ready(player,target,107));check(limit.ready(player,target,108));
        check(limit.ready(player,target,0)); // Rewound/reset world clock cannot lock the weapon.
        int accepted=0;
        PairedClawHitLimiter<Object,Object> fast=new PairedClawHitLimiter<>();
        for(int tick=0;tick<20;tick++)for(int hand=0;hand<2;hand++) {
            if(fast.ready(player,target,tick)){fast.confirmed(player,target,tick);accepted++;}
        }
        check(accepted==5);
        net.minecraft.init.Bootstrap.register();
        checkLegacyCounterCleanup();
        System.out.println("Passed "+checks+" claw balance regression checks");
    }
    private static void checkLegacyCounterCleanup() {
        com.nanonaitor.arsenal.item.ItemClaws claws=new com.nanonaitor.arsenal.item.ItemClaws(
            com.nanonaitor.arsenal.item.WeaponTier.IRON);
        net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(claws);
        net.minecraft.nbt.NBTTagCompound tag=new net.minecraft.nbt.NBTTagCompound();
        tag.setInteger("ClawCriticalChain",3);tag.setInteger("LastConfirmedClawTarget",1);
        tag.setString("OwnerTest","preserved");stack.setTagCompound(tag);
        claws.resetPair(stack);
        check(!tag.hasKey("ClawCriticalChain"));check(!tag.hasKey("LastConfirmedClawTarget"));
        check("preserved".equals(tag.getString("OwnerTest")));
    }
}
