package com.nanonaitor.arsenal.combat;

import net.minecraft.util.math.Vec3d;

/** Pure geometry shared by gameplay and regression tests. */
public final class SegmentMath {
    private static final double EPSILON = 1.0E-9D;

    private SegmentMath() {}

    /** Squared distance between the closest points of segments p1-q1 and p2-q2. */
    public static double distanceSq(Vec3d p1, Vec3d q1, Vec3d p2, Vec3d q2) {
        Vec3d d1 = q1.subtract(p1);
        Vec3d d2 = q2.subtract(p2);
        Vec3d r = p1.subtract(p2);
        double a = d1.dotProduct(d1);
        double e = d2.dotProduct(d2);
        double f = d2.dotProduct(r);
        double s;
        double t;
        if (a <= EPSILON && e <= EPSILON) return r.dotProduct(r);
        if (a <= EPSILON) {
            s = 0.0D;
            t = clamp(f / e);
        } else {
            double c = d1.dotProduct(r);
            if (e <= EPSILON) {
                t = 0.0D;
                s = clamp(-c / a);
            } else {
                double b = d1.dotProduct(d2);
                double denominator = a * e - b * b;
                s = denominator > EPSILON ? clamp((b * f - c * e) / denominator) : 0.0D;
                t = (b * s + f) / e;
                if (t < 0.0D) {
                    t = 0.0D;
                    s = clamp(-c / a);
                } else if (t > 1.0D) {
                    t = 1.0D;
                    s = clamp((b - c) / a);
                }
            }
        }
        return p1.add(d1.scale(s)).squareDistanceTo(p2.add(d2.scale(t)));
    }

    private static double clamp(double value) {
        return value < 0.0D ? 0.0D : value > 1.0D ? 1.0D : value;
    }
}
