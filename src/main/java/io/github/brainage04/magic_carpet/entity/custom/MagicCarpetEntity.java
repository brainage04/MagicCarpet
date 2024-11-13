package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.item.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.input.Input;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MagicCarpetEntity extends VehicleEntity {
    public static final EntityType<MagicCarpetEntity> ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MagicCarpet.MOD_ID, "magic_carpet"),
            EntityType.Builder.create(MagicCarpetEntity::new, SpawnGroup.MISC)
                    .dimensions(2.00f, 0.25f)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MagicCarpet.MOD_ID, "magic_carpet")))
    );

    private float movementForward = 0.0f;
    private float movementSideways = 0.0f;
    private boolean pressingSpace = false;

    // range: 0-1. lower = faster acceleration from 0 to max velocity
    private static final double SMOOTHING_FACTOR = 0.1;

    public MagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected Item asItem() {
        return ModItems.MAGIC_CARPET;
    }

    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        super.tick();

        if (isLogicalSideForUpdatingMovement()) {
            updateVelocity();
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {

    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {

    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ActionResult actionResult = super.interact(player, hand);
        if (actionResult != ActionResult.PASS) {
            return actionResult;
        } else {
            return player.shouldCancelInteraction() || !getWorld().isClient && !player.startRiding(this) ? ActionResult.PASS : ActionResult.SUCCESS;
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().size() < getMaxPassengers();
    }

    protected int getMaxPassengers() {
        return 2;
    }

    @Environment(EnvType.CLIENT)
    public void setInputs(Input input) {
        this.movementForward = input.movementForward;
        this.movementSideways = input.movementSideways;
        this.pressingSpace = input.playerInput.jump();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity var2 = getFirstPassenger();
        LivingEntity var10000;
        if (var2 instanceof LivingEntity livingEntity) {
            var10000 = livingEntity;
        } else {
            var10000 = super.getControllingPassenger();
        }

        return var10000;
    }

    private float getVerticalVelocity(float pitch) {
        if (pitch >= 45.0f) {
            // move down at 10 blocks/second max
            return -0.50f;
        } else if (45.0f > pitch && pitch > 15.0f) {
            return (-pitch + 30.0f) / 30.0f;
        } else {
            // move up at 10 blocks/second max
            return 0.50f;
        }
    }

    private void updateVelocity() {
        LivingEntity passenger = getControllingPassenger();

        if (!(passenger instanceof PlayerEntity player)) return;

        Vec3d targetVelocity = new Vec3d(
                MathHelper.sin(-player.getYaw() * 0.017453292F) * movementForward +
                        MathHelper.cos(-player.getYaw() * 0.017453292F) * movementSideways,
                0.0,
                MathHelper.cos(player.getYaw() * 0.017453292F) * movementForward +
                        MathHelper.sin(player.getYaw() * 0.017453292F) * movementSideways
        ).normalize().add(
                new Vec3d(
                        0.0,
                        pressingSpace ? getVerticalVelocity(player.getPitch()) : getFinalGravity(),
                        0.0
                )
        );

        Vec3d currentVelocity = getVelocity();
        Vec3d smoothedVelocity = currentVelocity.lerp(targetVelocity, SMOOTHING_FACTOR);

        setVelocity(smoothedVelocity);

        move(MovementType.PLAYER, this.getVelocity());
    }
}