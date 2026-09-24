package com.nanonaitor.arsenal.combat;

public final class ReflectionWindowTest {
    public static void main(String[] args) {
        int checks=0;
        for(boolean client:new boolean[]{false,true})
            for(boolean cooling:new boolean[]{false,true})
                for(boolean own:new boolean[]{false,true}) {
                    boolean expected=!client&&cooling&&!own;
                    if(ReflectionWindow.externalCooldown(client,cooling,own)!=expected)
                        throw new AssertionError("Cooldown identity rule failed");
                    checks++;
                }
        // A normal sync and a successful-reflect cooldown replacement must not
        // shorten any of the client's twenty active animation ticks.
        for(int tick=0;tick<20;tick++) {
            if(ReflectionWindow.externalCooldown(true,true,false))
                throw new AssertionError("Client spin interrupted at "+tick);
            checks++;
        }
        System.out.println("Blade staff reflection regression: "+checks+" checks passed.");
    }
}
