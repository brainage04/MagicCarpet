package io.github.brainage04.magic_carpet.item.custom;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class MagicCarpetItem extends Item {
    public MagicCarpetItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        int distance = user.isCreative() ? 4 : 3;
        HitResult hit = user.raycast(distance, 0, true);

        if (hit.getType() != HitResult.Type.BLOCK) return ActionResult.FAIL;

        user.playSound(SoundEvents.BLOCK_WOOL_PLACE, 1.0F, 1.0F);

        MagicCarpetEntity entity = new MagicCarpetEntity(MagicCarpetEntity.ENTITY_TYPE, world);
        Vec3d pos = hit.getPos();
        // go back half a block to avoid placing the entity inside a block
        pos.offset(Direction.getFacing(user.getPos()), 0.5);
        entity.setPosition(pos);
        world.spawnEntity(entity);

        if (!user.isCreative()) {
            user.getStackInHand(hand).decrement(1);
        }

        return ActionResult.SUCCESS;
    }
}
