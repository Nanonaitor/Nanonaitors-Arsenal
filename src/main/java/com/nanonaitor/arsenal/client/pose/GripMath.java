package com.nanonaitor.arsenal.client.pose;

/**
 * Finds where a held item is from the right arm's angles, and aims the left
 * arm at a grip point on it, so two-handed weapons keep both hands on them.
 *
 * Model space is HumanoidModel's (pixels; -Y up, -Z forward, -X the player's
 * right). A part's rotation is Rz * Ry * Rx applied to its local frame, whose
 * +Y runs down the arm. Item offsets and axes are given in that arm-local
 * frame, already including ItemInHandLayer's transforms and the item's
 * third-person display transform.
 */
final class GripMath {
    private static final double[] RIGHT_SHOULDER = {-5.0D, 2.0D, 0.0D};
    private static final double[] LEFT_SHOULDER = {5.0D, 2.0D, 0.0D};
    /** Distance from shoulder pivot to the centre of the gripping hand. */
    private static final double REACH = 10.0D;

    static double[] rotate(float ax, float ay, float az, double[] v) {
        double cx = Math.cos(ax), sx = Math.sin(ax);
        double x = v[0], y = v[1] * cx - v[2] * sx, z = v[1] * sx + v[2] * cx;
        double cy = Math.cos(ay), sy = Math.sin(ay);
        double x2 = x * cy + z * sy, z2 = -x * sy + z * cy;
        double cz = Math.cos(az), sz = Math.sin(az);
        return new double[] {x2 * cz - y * sz, x2 * sz + y * cz, z2};
    }

    /**
     * Turns an arm about its own length (a forearm roll), returning the model's
     * {x, y, z} angles for it: R = Rz Ry Rx followed by a turn about local Y.
     */
    static float[] roll(float[] arm, float roll) {
        double[] ex = rotate(arm[0], arm[1], arm[2], new double[] {1, 0, 0});
        double[] ey = rotate(arm[0], arm[1], arm[2], new double[] {0, 1, 0});
        double[] ez = rotate(arm[0], arm[1], arm[2], new double[] {0, 0, 1});
        double c = Math.cos(roll), s = Math.sin(roll);
        // Columns of R * Ry(roll): x' = c*ex - s*ez, y' = ey, z' = s*ex + c*ez.
        double[] cx = {c * ex[0] - s * ez[0], c * ex[1] - s * ez[1], c * ex[2] - s * ez[2]};
        double[] cz = {s * ex[0] + c * ez[0], s * ex[1] + c * ez[1], s * ex[2] + c * ez[2]};
        double r20 = cx[2], r21 = ey[2], r22 = cz[2], r10 = cx[1], r00 = cx[0];
        double b = Math.asin(Math.max(-1.0D, Math.min(1.0D, -r20)));
        double a = Math.atan2(r21, r22);
        double g = Math.atan2(r10, r00);
        return new float[] {(float)a, (float)b, (float)g};
    }

    /**
     * Left-arm angles {x, y, z} that put the left hand on the held item.
     *
     * @param right       right arm angles {x, y, z}
     * @param localCenter item reference point in the right arm's local frame (px)
     * @param localAxis   item axis in the right arm's local frame (unit)
     * @param minOffset   nearest grip distance from the reference point (px)
     * @param maxOffset   farthest grip distance from the reference point (px)
     */
    static float[] leftGrip(float[] right, double[] localCenter, double[] localAxis,
                            double minOffset, double maxOffset) {
        double[] c = rotate(right[0], right[1], right[2], localCenter);
        double[] d = rotate(right[0], right[1], right[2], localAxis);
        double[] center = {RIGHT_SHOULDER[0] + c[0], RIGHT_SHOULDER[1] + c[1], RIGHT_SHOULDER[2] + c[2]};
        double bestError = Double.MAX_VALUE;
        double[] grip = center;
        for (double s = minOffset; s <= maxOffset + 1.0E-6D; s += 0.5D) {
            double[] p = {center[0] + d[0] * s, center[1] + d[1] * s, center[2] + d[2] * s};
            double dx = p[0] - LEFT_SHOULDER[0], dy = p[1] - LEFT_SHOULDER[1], dz = p[2] - LEFT_SHOULDER[2];
            double error = Math.abs(Math.sqrt(dx * dx + dy * dy + dz * dz) - REACH);
            if (error < bestError) { bestError = error; grip = p; }
        }
        double vx = grip[0] - LEFT_SHOULDER[0], vy = grip[1] - LEFT_SHOULDER[1], vz = grip[2] - LEFT_SHOULDER[2];
        double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length < 1.0E-6D) return new float[] {-1.0F, 0.5F, 0.0F};
        vx /= length; vy /= length; vz /= length;
        // Invert Ry * Rx applied to (0, 1, 0): (sin y sin x, cos x, cos y sin x).
        double ax = -Math.acos(Math.max(-1.0D, Math.min(1.0D, vy)));
        double s = Math.sin(ax);
        double ay = Math.abs(s) < 1.0E-6D ? 0.0D : Math.atan2(vx / s, vz / s);
        return new float[] {(float)ax, (float)ay, 0.0F};
    }

    private GripMath() {}
}
