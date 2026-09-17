package com.nanonaitor.arsenal.combat;
import java.util.Map;
import java.util.WeakHashMap;
public final class PairedClawHitLimiter<P,T> {
    private final Map<P,Map<T,Long>> hits=new WeakHashMap<>();
    public boolean ready(P player,T target,long now) {
        var targets=hits.get(player);Long last=targets==null?null:targets.get(target);
        return last==null || now<last || now-last>=4;
    }
    public void confirmed(P player,T target,long now) {
        hits.computeIfAbsent(player,p->new WeakHashMap<>()).put(target,now);
    }
}
