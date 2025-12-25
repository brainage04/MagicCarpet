package io.github.brainage04.magic_carpet.item.custom;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public abstract class MagicCarpetItem extends Item {
    public MagicCarpetItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        ItemStack itemStack = user.getItemInHand(hand);
        HitResult hitResult = getPlayerPOVHitResult(world, user, ClipContext.Fluid.ANY);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResult.PASS;
        } else {
            Vec3 vec3d = user.getViewVector(1.0F);
            List<Entity> list = world.getEntities(user, user.getBoundingBox().expandTowards(vec3d.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);

            if (!list.isEmpty()) {
                Vec3 vec3d2 = user.getEyePosition();
                for (Entity entity : list) {
                    AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                    if (box.contains(vec3d2)) {
                        return InteractionResult.PASS;
                    }
                }
            }

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                MagicCarpetEntity entity = createEntity(world, hitResult, itemStack, user);

                if (entity == null) {
                    return InteractionResult.FAIL;
                } else if (!world.noCollision(entity, entity.getBoundingBox())) {
                    return InteractionResult.FAIL;
                } else {
                    if (!world.isClientSide()) {
                        world.addFreshEntity(entity);
                        world.gameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getLocation());
                        itemStack.consume(1, user);
                    }

                    user.awardStat(Stats.ITEM_USED.get(this));
                    return InteractionResult.SUCCESS;
                }
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    public abstract MagicCarpetEntity createEntity(Level world);

    private MagicCarpetEntity createEntity(Level world, HitResult hitResult, ItemStack stack, Player player) {
        MagicCarpetEntity entity = createEntity(world);
        if (entity == null) return null;
        entity.setPos(hitResult.getLocation());

        if (world instanceof ServerLevel serverWorld) {
            EntityType.createDefaultStackConfig(serverWorld, stack, player).accept(entity);
        }

        return entity;
    }
}
