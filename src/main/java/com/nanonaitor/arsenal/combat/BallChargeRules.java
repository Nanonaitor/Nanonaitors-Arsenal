package com.nanonaitor.arsenal.combat;
public final class BallChargeRules {
    private BallChargeRules() {}
    public static int nextCharge(int current, int maximum, boolean matchingSet) {
        int next=Math.min(maximum,current+1);
        return matchingSet && next==2 ? maximum : next;
    }
}
