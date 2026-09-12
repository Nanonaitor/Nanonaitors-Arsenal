package com.nanonaitor.arsenal.client.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;

/** Exact 1.12 entity-model export supplied with the Charge Targsy artwork. */
public final class ModelTartsyShield extends ModelBase {
    private final ModelRenderer bone;
    private final ModelRenderer main;

    public ModelTartsyShield() {
        textureWidth = 64;
        textureHeight = 64;

        bone = new ModelRenderer(this);
        bone.setRotationPoint(0.0F, 15.5F, -1.5F);

        ModelRenderer cubeR1 = child(bone, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F);
        cubeR1.cubeList.add(new ModelBox(cubeR1, 0, 24,
            -3.5F, -3.5F, -3.5F, 7, 7, 7, 0.0F, false));

        ModelRenderer cubeR2 = child(bone, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 1.5708F);
        cubeR2.cubeList.add(new ModelBox(cubeR2, 0, 38,
            -3.5F, -3.5F, -3.5F, 7, 7, 7, 0.0F, false));

        ModelRenderer bone2 = child(bone, 0.0F, -6.5F, -3.5355F, 0.7854F, 0.0F, 0.0F);
        ModelRenderer cubeR3 = child(bone2, 0.0F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F);
        cubeR3.cubeList.add(new ModelBox(cubeR3, 24, 40,
            3.625F, 9.4298F, -5.625F, 2, 2, 2, 0.0F, false));

        main = new ModelRenderer(this);
        main.setRotationPoint(0.0F, 24.0F, 0.0F);
        main.cubeList.add(new ModelBox(main, 15, 14,
            -1.0F, -11.0F, -1.0F, 2, 6, 3, 0.0F, false));

        addSpike(main, -5.192F, -8.674F, -2.2929F,
            -0.9553F, -0.5236F, -0.9553F, true);
        addSpike(main, 5.192F, -8.674F, -2.2929F,
            -0.9553F, 0.5236F, 0.9553F, false);
        addSpike(main, 0.0F, -14.1057F, -2.2657F,
            -0.9553F, 0.5236F, -0.6155F, false);

        ModelRenderer rimRight = child(main, 4.9493F, -3.7243F, -1.5F,
            0.0F, 0.0F, -0.7418F);
        rimRight.cubeList.add(new ModelBox(rimRight, 0, 14,
            -4.0F, -1.0F, -0.5F, 8, 2, 1, 0.0F, true));

        ModelRenderer rimTopRight = child(main, 3.3061F, -14.8082F, -1.5F,
            0.0F, 0.0F, -2.3562F);
        rimTopRight.cubeList.add(new ModelBox(rimTopRight, 0, 14,
            -6.0F, -0.675F, -0.5F, 8, 2, 1, 0.0F, false));

        ModelRenderer rimTopLeft = child(main, -3.3061F, -14.8082F, -1.5F,
            0.0F, 0.0F, 2.3562F);
        // The supplied Java export duplicated this exact box. Rendering it once
        // preserves the shape while eliminating coplanar z-fighting.
        rimTopLeft.cubeList.add(new ModelBox(rimTopLeft, 0, 14,
            -2.0F, -0.675F, -0.5F, 8, 2, 1, 0.0F, true));

        ModelRenderer rimLeft = child(main, -4.9493F, -3.7243F, -1.5F,
            0.0F, 0.0F, 0.7854F);
        rimLeft.cubeList.add(new ModelBox(rimLeft, 0, 14,
            -4.0F, -1.0F, -0.5F, 8, 2, 1, 0.0F, false));

        ModelRenderer plate = child(main, -0.0014F, -8.675F, -1.85F,
            0.0F, 0.0F, 0.7854F);
        plate.cubeList.add(new ModelBox(plate, 0, 0,
            -5.9986F, -6.0F, -1.15F, 12, 12, 2, 0.0F, false));
    }

    private void addSpike(ModelRenderer parent, float x, float y, float z,
                          float rx, float ry, float rz, boolean mirror) {
        ModelRenderer spike = child(parent, x, y, z, rx, ry, rz);
        spike.cubeList.add(new ModelBox(spike, 28, 34,
            -1.0F, -1.0F, -1.0F, 2, 2, 2, 0.0F, mirror));
    }

    private ModelRenderer child(ModelRenderer parent, float x, float y, float z,
                                float rx, float ry, float rz) {
        ModelRenderer part = new ModelRenderer(this);
        part.setRotationPoint(x, y, z);
        part.rotateAngleX = rx;
        part.rotateAngleY = ry;
        part.rotateAngleZ = rz;
        parent.addChild(part);
        return part;
    }

    public void render(float scale) {
        bone.render(scale);
        main.render(scale);
    }
}
