package com.nanonaitor.arsenal.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Exact 1.12 entity-model export supplied with the Sun-War Bulwark artwork. */
public final class ModelSunWarBulwark extends LegacyShieldModel {
    private final LegacyPart shield;
    private final LegacyPart main;

    public ModelSunWarBulwark() {
        textureWidth = 64;
        textureHeight = 64;

        shield = new LegacyPart(this);
        shield.setRotationPoint(6.65F, 23.75F, 0.75F);

        LegacyPart slab = child(shield, 0.0F, 0.0F, 0.0F, 0.0F);
        slab.cubeList.add(new LegacyBox(slab, 0, 0,
            -13.65F, -17.75F, -2.75F, 14, 19, 3, 0.0F, false));
        slab.cubeList.add(new LegacyBox(slab, 34, 0,
            -3.65F, -12.9107F, -0.25F, 2, 7, 4, 0.0F, false));
        slab.cubeList.add(new LegacyBox(slab, 0, 38,
            -11.65F, -12.9107F, -0.25F, 2, 7, 4, 0.0F, false));

        LegacyPart metal = child(shield, 0.5F, -16.5F, 0.5F, 0.0F);
        metal.cubeList.add(new LegacyBox(metal, 0, 22,
            -15.65F, -4.75F, -3.75F, 17, 4, 4, 0.0F, false));

        LegacyPart lowerLeft = child(metal, -7.9087F, 20.4397F, -1.75F, 0.2138F);
        lowerLeft.cubeList.add(new LegacyBox(lowerLeft, 26, 30,
            -8.0F, -3.0F, -2.0F, 9, 4, 4, 0.0F, false));

        LegacyPart lowerRight = child(metal, -6.3786F, 20.4397F, -1.725F, -0.2138F);
        lowerRight.cubeList.add(new LegacyBox(lowerRight, 0, 30,
            -1.0F, -3.0F, -2.0F, 9, 4, 4, 0.0F, false));

        main = new LegacyPart(this);
        main.setRotationPoint(0.0F, 24.0F, 0.0F);

        LegacyPart rightRail = child(main, 5.5709F, -8.811F, -0.5F, 0.0175F);
        rightRail.cubeList.add(new LegacyBox(rightRail, 50, 33,
            -0.5F, -13.0F, -2.5F, 1, 26, 5, 0.0F, true));

        LegacyPart leftRail = child(main, -5.0905F, -5.8357F, -1.5F, -0.0654F);
        leftRail.cubeList.add(new LegacyBox(leftRail, 50, 33,
            -1.0F, -16.0F, -1.5F, 1, 26, 5, 0.0F, false));
    }

    private LegacyPart child(LegacyPart parent, float x, float y, float z, float rz) {
        LegacyPart part = new LegacyPart(this);
        part.setRotationPoint(x, y, z);
        part.rotateAngleZ = rz;
        parent.addChild(part);
        return part;
    }

    public ModelPart bake() {
        return bakeParts(shield, main);
    }
}
