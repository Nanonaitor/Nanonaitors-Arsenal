package com.nanonaitor.arsenal.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;

/** Exact 1.12 entity-model export supplied with the Sun-War Bulwark artwork. */
public final class ModelSunWarBulwark extends ModelBase {
    private final ModelRenderer shield;
    private final ModelRenderer main;

    public ModelSunWarBulwark() {
        textureWidth = 64;
        textureHeight = 64;

        shield = new ModelRenderer(this);
        shield.setRotationPoint(6.65F, 23.75F, 0.75F);

        ModelRenderer slab = child(shield, 0.0F, 0.0F, 0.0F, 0.0F);
        slab.cubeList.add(new ModelBox(slab, 0, 0,
            -13.65F, -17.75F, -2.75F, 14, 19, 3, 0.0F, false));
        slab.cubeList.add(new ModelBox(slab, 34, 0,
            -3.65F, -12.9107F, -0.25F, 2, 7, 4, 0.0F, false));
        slab.cubeList.add(new ModelBox(slab, 0, 38,
            -11.65F, -12.9107F, -0.25F, 2, 7, 4, 0.0F, false));

        ModelRenderer metal = child(shield, 0.5F, -16.5F, 0.5F, 0.0F);
        metal.cubeList.add(new ModelBox(metal, 0, 22,
            -15.65F, -4.75F, -3.75F, 17, 4, 4, 0.0F, false));

        ModelRenderer lowerLeft = child(metal, -7.9087F, 20.4397F, -1.75F, 0.2138F);
        lowerLeft.cubeList.add(new ModelBox(lowerLeft, 26, 30,
            -8.0F, -3.0F, -2.0F, 9, 4, 4, 0.0F, false));

        ModelRenderer lowerRight = child(metal, -6.3786F, 20.4397F, -1.725F, -0.2138F);
        lowerRight.cubeList.add(new ModelBox(lowerRight, 0, 30,
            -1.0F, -3.0F, -2.0F, 9, 4, 4, 0.0F, false));

        main = new ModelRenderer(this);
        main.setRotationPoint(0.0F, 24.0F, 0.0F);

        ModelRenderer rightRail = child(main, 5.5709F, -8.811F, -0.5F, 0.0175F);
        rightRail.cubeList.add(new ModelBox(rightRail, 50, 33,
            -0.5F, -13.0F, -2.5F, 1, 26, 5, 0.0F, true));

        ModelRenderer leftRail = child(main, -5.0905F, -5.8357F, -1.5F, -0.0654F);
        leftRail.cubeList.add(new ModelBox(leftRail, 50, 33,
            -1.0F, -16.0F, -1.5F, 1, 26, 5, 0.0F, false));
    }

    private ModelRenderer child(ModelRenderer parent, float x, float y, float z, float rz) {
        ModelRenderer part = new ModelRenderer(this);
        part.setRotationPoint(x, y, z);
        part.rotateAngleZ = rz;
        parent.addChild(part);
        return part;
    }

    public void render(float scale) {
        shield.render(scale);
        main.render(scale);
    }
}
