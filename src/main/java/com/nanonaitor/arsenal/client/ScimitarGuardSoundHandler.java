package com.nanonaitor.arsenal.client;

import com.nanonaitor.arsenal.NanonaitorsArsenal;
import com.nanonaitor.arsenal.compat.ScimitarShieldCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid=NanonaitorsArsenal.MOD_ID,value=Side.CLIENT)
public final class ScimitarGuardSoundHandler {
    private static long quietUntil;
    private ScimitarGuardSoundHandler() {}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null){quietUntil=0;return;}
        if(ScimitarShieldCompat.isGuarding(mc.player) || ScimitarShieldCompat.disabled(mc.player))
            quietUntil=mc.world.getTotalWorldTime()+4;
        // A deliberate new attack should immediately restore its normal sound.
        else if(org.lwjgl.input.Mouse.isButtonDown(0) && !org.lwjgl.input.Mouse.isButtonDown(1)) quietUntil=0;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void sound(PlaySoundEvent event) {
        Minecraft mc=Minecraft.getMinecraft();
        if(mc.player==null || event.getResultSound()==null || !ScimitarShieldCompat.isPair(mc.player))return;
        if(!ScimitarShieldCompat.isGuarding(mc.player) && !ScimitarShieldCompat.disabled(mc.player)
            && mc.world.getTotalWorldTime()>quietUntil)return;
        ISound sound=event.getResultSound();
        ResourceLocation id=sound.getSoundLocation();
        if(!isSwing(id.getResourceDomain(),id.getResourcePath()))return;
        EntityPlayer nearest=mc.world.getClosestPlayer(sound.getXPosF(),sound.getYPosF(),sound.getZPosF(),2,false);
        if(nearest==mc.player)event.setResultSound(null);
    }
    static boolean isSwing(String domain,String path) {
        if("minecraft".equals(domain)) return "entity.player.attack.sweep".equals(path)
            || "entity.player.attack.nodamage".equals(path) || "entity.player.attack.weak".equals(path)
            || "entity.player.attack.strong".equals(path);
        return ChainSwingSoundFilter.isExtraMeleeSound(domain,path);
    }
}
