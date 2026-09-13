package com.nanonaitor.arsenal.client.model;

import java.util.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Lossless adapter for the original 64x64 entity-model cube coordinates and UVs. */
public abstract class LegacyShieldModel {
    protected int textureWidth, textureHeight;
    protected ModelPart bakeParts(LegacyPart... parts) {
        MeshDefinition mesh = new MeshDefinition();
        for (int i=0;i<parts.length;i++) parts[i].add(mesh.getRoot(),"root"+i);
        return LayerDefinition.create(mesh,textureWidth,textureHeight).bakeRoot();
    }
    protected static final class LegacyPart {
        float x,y,z,rotateAngleX,rotateAngleY,rotateAngleZ;
        final List<LegacyBox> cubeList=new ArrayList<>();
        final List<LegacyPart> children=new ArrayList<>();
        LegacyPart(LegacyShieldModel model) {}
        void setRotationPoint(float x,float y,float z){this.x=x;this.y=y;this.z=z;}
        void addChild(LegacyPart part){children.add(part);}
        void add(PartDefinition parent,String name){
            CubeListBuilder cubes=CubeListBuilder.create();
            for(var c:cubeList) cubes.texOffs(c.u,c.v).mirror(c.mirror).addBox(c.x,c.y,c.z,c.w,c.h,c.d);
            var part=parent.addOrReplaceChild(name,cubes,PartPose.offsetAndRotation(x,y,z,rotateAngleX,rotateAngleY,rotateAngleZ));
            for(int i=0;i<children.size();i++)children.get(i).add(part,"part"+i);
        }
    }
    protected static final class LegacyBox {
        int u,v; float x,y,z,w,h,d; boolean mirror;
        LegacyBox(LegacyPart parent,int u,int v,float x,float y,float z,int w,int h,int d,float inflate,boolean mirror){
            this.u=u;this.v=v;this.x=x;this.y=y;this.z=z;this.w=w;this.h=h;this.d=d;this.mirror=mirror;
        }
    }
}
