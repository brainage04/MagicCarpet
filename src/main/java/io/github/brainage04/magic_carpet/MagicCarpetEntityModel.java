package io.github.brainage04.magic_carpet;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.util.Identifier;

public class MagicCarpetEntityModel extends EntityModel<MagicCarpetEntityRenderState> {
    public static final EntityModelLayer ENTITY_MODEL_LAYER =
            new EntityModelLayer(Identifier.of(MagicCarpet.MOD_ID, "magic_carpet"), "main");

    private final ModelPart base;

    public MagicCarpetEntityModel(ModelPart modelPart) {
        super(modelPart);
        this.base = modelPart.getChild(EntityModelPartNames.CUBE);
    }

    // You can use BlockBench, make your model and export it to get this method for your entity model.
    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        modelPartData.addChild(EntityModelPartNames.CUBE, ModelPartBuilder.create().uv(0, 0).cuboid(-6F, 12F, -6F, 12F, 12F, 12F), ModelTransform.pivot(0F, 0F, 0F));
        return TexturedModelData.of(modelData, 64, 64);
    }
}