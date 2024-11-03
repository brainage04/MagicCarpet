package io.github.brainage04.magic_carpet;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
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

        user.playSound(SoundEvents.BLOCK_WOOL_BREAK, 1.0F, 1.0F);

        world.spawnEntity(new MagicCarpetEntity(MagicCarpetEntity.ENTITY_TYPE, world));

        return ActionResult.SUCCESS;
    }
}
