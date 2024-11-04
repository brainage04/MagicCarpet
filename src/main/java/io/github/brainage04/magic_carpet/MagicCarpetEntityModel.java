package io.github.brainage04.magic_carpet;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class MagicCarpetEntityModel extends EntityModel<MagicCarpetEntityRenderState> {
    public static final EntityModelLayer ENTITY_MODEL_LAYER =
            new EntityModelLayer(Identifier.of(MagicCarpet.MOD_ID, "magic_carpet"), "main");

    private final ModelPart bb_main;

    public MagicCarpetEntityModel(ModelPart modelPart) {
        super(modelPart);
        this.bb_main = root.getChild("bb_main");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();
        ModelPartData bb_main = modelPartData.addChild("bb_main", ModelPartBuilder.create().uv(0, 0).cuboid(-12.0F, 0.0F, 12.0F, 24.0F, 1.0F, 4.0F, new Dilation(0.0F))
                .uv(0, 5).cuboid(-12.0F, 0.0F, 4.0F, 24.0F, 1.0F, 8.0F, new Dilation(0.0F))
                .uv(0, 5).cuboid(-12.0F, 0.0F, -12.0F, 24.0F, 1.0F, 8.0F, new Dilation(0.0F))
                .uv(0, 5).cuboid(-12.0F, 0.0F, -4.0F, 24.0F, 1.0F, 8.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-12.0F, 0.0F, -16.0F, 24.0F, 1.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 14);
    }
}