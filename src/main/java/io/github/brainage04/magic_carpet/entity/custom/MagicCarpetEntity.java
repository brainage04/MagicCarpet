package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.MagicCarpet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;

public abstract class MagicCarpetEntity extends VehicleEntity {
    public static <T extends MagicCarpetEntity> EntityType<T> generateEntityType(String carpetTier, EntityType.EntityFactory<T> entityFactory) {
        String entityId = "%s_%s".formatted(carpetTier, "magic_carpet");
        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, entityId),
                EntityType.Builder.of(entityFactory, MobCategory.MISC)
                        .sized(2.00f, 0.25f)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, entityId)))
        );
    }

    private float movementForward = 0.0f;
    private float movementSideways = 0.0f;
    private boolean pressingSpace = false;

    public float prevRenderPitch;
    public float prevRenderRoll;
    public float renderPitch;
    public float renderRoll;

    public MagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    protected abstract double getMaxSpeed();

    protected abstract double getAccelerationTime();

    protected double getSmoothingFactor() {
        // Calculate ticks needed to reach max speed
        double ticks = getAccelerationTime() * 20.0;
        // Formula: to reach ~99% of max speed in n ticks using lerp
        // f = 1 - 0.01^(1/n)
        return 1.0 - Math.pow(0.01, 1.0 / ticks);
    }

    @Override
    protected abstract Item getDropItem();

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void tick() {
        super.tick();

        this.prevRenderPitch = this.renderPitch;
        this.prevRenderRoll = this.renderRoll;

        float yaw = (float) Math.toRadians(this.getYRot());
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);

        double x = this.getDeltaMovement().x();
        double y = this.getDeltaMovement().y();
        double z = this.getDeltaMovement().z();

        this.renderPitch = (float) Mth.clamp(
                (y - (cos * z - sin * x)) * 30.0F,
                -45.0F,
                45.0F
        );

        this.renderRoll = (float) Mth.clamp(
                (sin * z + cos * x) * 30.0F,
                -30.0F,
                30.0F
        );

        if (isLocalInstanceAuthoritative()) {
            updateVelocity();
        }

        collectAdditionalPassengers();

        if (hasControllingPassenger()) {
            //noinspection DataFlowIssue - should be obvious why
            setYRot(getControllingPassenger().getYRot());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult actionResult = super.interact(player, hand);
        if (actionResult != InteractionResult.PASS) {
            return actionResult;
        } else {
            return player.isSecondaryUseActive() || !level().isClientSide() && !player.startRiding(this)
                    ? InteractionResult.PASS
                    : InteractionResult.SUCCESS;
        }
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < getMaxPassengers();
    }

    protected int getMaxPassengers() {
        return 2;
    }

    @Environment(EnvType.CLIENT)
    public void setInputs(ClientInput input) {
        Vec2 movement = input.getMoveVector();
        this.movementForward = movement.y;
        this.movementSideways = movement.x;
        this.pressingSpace = input.keyPresses.jump();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity var2 = getFirstPassenger();
        if (var2 instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        return super.getControllingPassenger();
    }

    private float getVerticalVelocity(float pitch) {
        if (pitch >= 45.0f) {
            return -0.50f;
        } else if (pitch > 15.0f) {
            return (-pitch + 30.0f) / 30.0f;
        } else {
            return 0.50f;
        }
    }

    private void updateVelocity() {
        LivingEntity passenger = getControllingPassenger();

        if (!(passenger instanceof Player player)) return;

        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 targetVelocity = getTargetVelocity(player, yaw);

        Vec3 currentVelocity = getDeltaMovement();
        Vec3 smoothedVelocity = currentVelocity.lerp(targetVelocity, getSmoothingFactor());

        setDeltaMovement(smoothedVelocity);
        move(MoverType.PLAYER, this.getDeltaMovement());
    }

    private @NonNull Vec3 getTargetVelocity(Player player, float yaw) {
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);

        Vec3 targetVelocity = new Vec3(
                cos * movementSideways - sin * movementForward,
                pressingSpace ? getVerticalVelocity(player.getXRot()) : getGravity(),
                sin * movementSideways + cos * movementForward
        );

        // apply max speed to horizontal components
        double maxSpeed = getMaxSpeed();
        targetVelocity = new Vec3(
                targetVelocity.x * maxSpeed,
                targetVelocity.y * maxSpeed,
                targetVelocity.z * maxSpeed
        );
        return targetVelocity;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        float offset = 0.0F;
        if (this.getPassengers().size() > 1) {
            int i = this.getPassengers().indexOf(passenger);
            if (i == 0) {
                offset = 0.4F;
            } else {
                offset = -0.4F;
            }
        }
        return new Vec3(0.0, dimensions.height() / 3.0F, offset).yRot(-this.getYRot() * 0.017453292F);
    }

    private void collectAdditionalPassengers() {
        List<Entity> list = this.level().getEntities(
                this,
                this.getBoundingBox().inflate(0.2, -0.01, 0.2),
                EntitySelector.pushableBy(this)
        );

        if (!list.isEmpty()) {
            boolean bl = !this.level().isClientSide() && !(this.getControllingPassenger() instanceof Player);

            for (Entity entity : list) {
                if (bl && this.getPassengers().size() < this.getMaxPassengers()
                        && !entity.isPassenger()
                        && entity.getBbWidth() < this.getBbWidth()
                        && entity instanceof LivingEntity
                        && !(entity instanceof WaterAnimal)
                        && !(entity instanceof Player)
                        && !(entity instanceof Creaking)) {
                    entity.startRiding(this);
                } else {
                    this.push(entity);
                }
            }
        }
    }
}