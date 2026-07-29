package io.github.brainage04.magic_carpet.entity.model;

import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class MagicCarpetEntityModel extends EntityModel<MagicCarpetEntityRenderState> {
    private final ModelPart bb_main;

    public MagicCarpetEntityModel(ModelPart modelPart) {
        super(modelPart);
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition modelPartData = modelData.getRoot();
        PartDefinition bb_main = modelPartData.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, 0.0F, 12.0F, 24.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5).addBox(-12.0F, 0.0F, 4.0F, 24.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5).addBox(-12.0F, 0.0F, -12.0F, 24.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5).addBox(-12.0F, 0.0F, -4.0F, 24.0F, 1.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-12.0F, 0.0F, -16.0F, 24.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(modelData, 64, 14);
    }
}