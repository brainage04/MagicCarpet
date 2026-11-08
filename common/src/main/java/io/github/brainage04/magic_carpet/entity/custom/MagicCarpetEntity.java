package io.github.brainage04.magic_carpet.entity.custom;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public abstract class MagicCarpetEntity extends VehicleEntity {
    private static final double DECELERATION_TIME_MULTIPLIER = 2.0;

    private static final double REST_SPEED_SQR = 1.0E-6;

    private static final double HALF_LENGTH = 1.0;

    private static final double HALF_WIDTH = 0.75;
    private static final double SURFACE_HEIGHT = 0.1;

    private static final double MAX_TILT_PITCH = 15.0;

    private static final double MAX_TILT_ROLL = 12.0;

    private float movementForward = 0.0f;
    private float movementSideways = 0.0f;
    private boolean pressingSpace = false;

    public float prevRenderPitch;
    public float prevRenderRoll;
    public float renderPitch;
    public float renderRoll;

    public float prevAnimationSpeed;

    public float animationSpeed;

    public MagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    public abstract double getMaxSpeed();

    protected abstract double getAccelerationTime();

    protected abstract void spawnAmbientParticle(Vec3 position, Vec3 backward, float speed);

    private static double smoothingFactor(double seconds) {
        return 1.0 - Math.pow(0.01, 1.0 / (seconds * 20.0));
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

        double maxSpeed = getMaxSpeed();
        this.renderPitch =
                (float)
                        Mth.clamp(
                                (y - (cos * z - sin * x)) / maxSpeed * MAX_TILT_PITCH,
                                -MAX_TILT_PITCH,
                                MAX_TILT_PITCH);

        this.renderRoll =
                (float)
                        Mth.clamp(
                                (sin * z + cos * x) / maxSpeed * MAX_TILT_ROLL,
                                -MAX_TILT_ROLL,
                                MAX_TILT_ROLL);

        if (isLocalInstanceAuthoritative()) {
            updateVelocity();
        }

        if (level().isClientSide()) {
            updateAnimationSpeed();
            spawnAmbientParticles();
        }

        collectAdditionalPassengers();

        if (hasControllingPassenger()) {
            //noinspection DataFlowIssue - should be obvious why
            setYRot(getControllingPassenger().getYRot());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {}

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isClientAuthoritative() {
        return false;
    }

    @Override
    protected boolean isLocalClientAuthoritative() {
        // Entity checks this separately from isClientAuthoritative on the controlling rider's
        // client.
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        InteractionResult actionResult = super.interact(player, hand, hitPos);
        if (actionResult != InteractionResult.PASS) {
            return actionResult;
        } else {
            return player.isSecondaryUseActive()
                            || !level().isClientSide() && !player.startRiding(this)
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

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        List<Entity> passengers = getPassengers();
        for (int i = 0; i < passengers.size(); i++) {
            if (passengers.get(i) instanceof Player player) {
                return player;
            }
        }
        Entity passenger = getFirstPassenger();
        if (passenger instanceof LivingEntity livingEntity) {
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
        Vec3 targetVelocity = Vec3.ZERO;
        if (getControllingPassenger() instanceof Player player) {
            if (player instanceof ServerPlayer serverPlayer) {
                updateInputs(serverPlayer.getLastClientInput());
            }
            targetVelocity = getTargetVelocity(player, (float) Math.toRadians(player.getYRot()));
        }

        Vec3 currentVelocity = getDeltaMovement();
        if (targetVelocity.lengthSqr() == 0.0 && currentVelocity.lengthSqr() < REST_SPEED_SQR) {
            if (currentVelocity.lengthSqr() != 0.0) {
                setDeltaMovement(Vec3.ZERO);
            }
            return;
        }

        setDeltaMovement(smoothVelocity(currentVelocity, targetVelocity));
        move(MoverType.PLAYER, this.getDeltaMovement());
    }

    private Vec3 smoothVelocity(Vec3 current, Vec3 target) {
        double acceleration = smoothingFactor(getAccelerationTime());
        double deceleration = smoothingFactor(getAccelerationTime() * DECELERATION_TIME_MULTIPLIER);

        double horizontal =
                target.horizontalDistanceSqr() < current.horizontalDistanceSqr()
                        ? deceleration
                        : acceleration;
        double vertical = Math.abs(target.y) < Math.abs(current.y) ? deceleration : acceleration;

        return new Vec3(
                Mth.lerp(horizontal, current.x, target.x),
                Mth.lerp(vertical, current.y, target.y),
                Mth.lerp(horizontal, current.z, target.z));
    }

    private Vec3 getTargetVelocity(Player player, float yaw) {
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);

        Vec3 targetVelocity =
                new Vec3(
                        cos * movementSideways - sin * movementForward,
                        pressingSpace ? getVerticalVelocity(player.getXRot()) : 0.0,
                        sin * movementSideways + cos * movementForward);

        return targetVelocity.scale(getMaxSpeed());
    }

    private void updateInputs(Input input) {
        movementForward = impulse(input.forward(), input.backward());
        movementSideways = impulse(input.left(), input.right());
        float length =
                Mth.sqrt(movementForward * movementForward + movementSideways * movementSideways);
        if (length > 1.0F) {
            movementForward /= length;
            movementSideways /= length;
        }
        pressingSpace = input.jump();
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }

    private void updateAnimationSpeed() {
        this.prevAnimationSpeed = this.animationSpeed;
        float target =
                (float) Math.min(getDeltaMovement().horizontalDistance() / getMaxSpeed(), 1.0);
        // velocity packets arrive in steps; ease towards them so animations don't twitch
        this.animationSpeed += (target - this.animationSpeed) * 0.25F;
    }

    private void spawnAmbientParticles() {
        RandomSource random = getRandom();
        float speed = this.animationSpeed;
        if (random.nextFloat() >= 0.12F + 0.55F * speed) {
            return;
        }

        float yaw = (float) Math.toRadians(getYRot());
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 sideways = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));

        double along =
                random.nextFloat() < speed
                        ? -HALF_LENGTH
                        : (random.nextDouble() * 2.0 - 1.0) * HALF_LENGTH;
        double across = (random.nextDouble() * 2.0 - 1.0) * HALF_WIDTH;

        Vec3 position =
                position()
                        .add(forward.scale(along))
                        .add(sideways.scale(across))
                        .add(0.0, SURFACE_HEIGHT, 0.0);
        spawnAmbientParticle(position, forward.reverse(), speed);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(
            Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        float offset = 0.0F;
        if (this.getPassengers().size() > 1) {
            int i = this.getPassengers().indexOf(passenger);
            if (i == 0) {
                offset = 0.4F;
            } else {
                offset = -0.4F;
            }
        }
        return new Vec3(0.0, dimensions.height() / 3.0F, offset)
                .yRot(-this.getYRot() * 0.017453292F);
    }

    private void collectAdditionalPassengers() {
        List<Entity> list =
                this.level()
                        .getEntities(
                                this,
                                this.getBoundingBox().inflate(0.2, -0.01, 0.2),
                                EntitySelector.pushableBy(this));

        if (!list.isEmpty()) {
            boolean bl =
                    !this.level().isClientSide()
                            && !(this.getControllingPassenger() instanceof Player);

            for (Entity entity : list) {
                if (bl
                        && this.getPassengers().size() < this.getMaxPassengers()
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
