package io.github.brainage04.magic_carpet.item.custom;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.List;

/*
    Most of this was stolen from net.minecraft.item.BoatItem lmao
 */
public class MagicCarpetItem extends Item {
    public MagicCarpetItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        HitResult hitResult = raycast(world, user, RaycastContext.FluidHandling.ANY);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return ActionResult.PASS;
        } else {
            Vec3d vec3d = user.getRotationVec(1.0F);
            List<Entity> list = world.getOtherEntities(user, user.getBoundingBox().stretch(vec3d.multiply(5.0)).expand(1.0), EntityPredicates.CAN_HIT);

            if (!list.isEmpty()) {
                Vec3d vec3d2 = user.getEyePos();
                for (Entity entity : list) {
                    Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());
                    if (box.contains(vec3d2)) {
                        return ActionResult.PASS;
                    }
                }
            }

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                MagicCarpetEntity entity = createEntity(world, hitResult, itemStack, user);

                if (entity == null) {
                    return ActionResult.FAIL;
                } else if (!world.isSpaceEmpty(entity, entity.getBoundingBox())) {
                    return ActionResult.FAIL;
                } else {
                    if (!world.isClient) {
                        world.spawnEntity(entity);
                        world.emitGameEvent(user, GameEvent.ENTITY_PLACE, hitResult.getPos());
                        itemStack.decrementUnlessCreative(1, user);
                    }

                    user.incrementStat(Stats.USED.getOrCreateStat(this));
                    return ActionResult.SUCCESS;
                }
            } else {
                return ActionResult.PASS;
            }
        }
    }

    private MagicCarpetEntity createEntity(World world, HitResult hitResult, ItemStack stack, PlayerEntity player) {
        MagicCarpetEntity entity = MagicCarpetEntity.ENTITY_TYPE.create(world, SpawnReason.SPAWN_ITEM_USE);
        if (entity == null) return null;
        entity.setPosition(hitResult.getPos());

        if (world instanceof ServerWorld serverWorld) {
            EntityType.copier(serverWorld, stack, player).accept(entity);
        }

        return entity;
    }
}
