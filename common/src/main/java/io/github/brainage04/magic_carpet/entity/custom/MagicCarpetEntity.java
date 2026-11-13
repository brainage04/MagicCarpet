package io.github.brainage04.magic_carpet.entity.custom;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SteppedInterpolationHandler;
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

public class MagicCarpetEntity extends VehicleEntity {
    private static final double REST_SPEED_SQR = 1.0E-6;

    private static final double HALF_LENGTH = 1.0;

    private static final double HALF_WIDTH = 0.75;
    private static final double SURFACE_HEIGHT = 0.1;

    private static final double MAX_TILT_PITCH = 15.0;

    private static final double MAX_TILT_ROLL = 12.0;

    /**
     * The share of the way the tilt and the ripple move towards the carpet's drawn movement each
     * tick. That movement changes in steps, once per position update from the server, because the
     * interpolation handler moves the carpet along a straight line from one update to the next
     * (SteppedInterpolationHandler.java:177-193 in 26.3). Easing keeps the tilt from snapping at
     * each step.
     */
    private static final float MOTION_EASING = 0.25F;

    private float movementForward = 0.0f;
    private float movementSideways = 0.0f;
    private boolean pressingSpace = false;

    public float prevRenderPitch;
    public float prevRenderRoll;
    public float renderPitch;
    public float renderRoll;

    public float prevAnimationSpeed;

    public float animationSpeed;

    private Vec3 drawnMovement = Vec3.ZERO;

    private @Nullable CarpetTier tier;

    public MagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * This carpet's tier, the {@code magic_carpet:tier} entry named like its entity type. The
     * registry is loaded once when the server starts and sent to each client as it joins, so the
     * entry never changes while the entity exists.
     */
    public CarpetTier tier() {
        if (this.tier == null) {
            this.tier =
                    level().registryAccess()
                            .lookupOrThrow(CarpetTier.REGISTRY)
                            .getValueOrThrow(CarpetTier.key(EntityType.getKey(getType())));
        }
        return this.tier;
    }

    @Override
    protected Item getDropItem() {
        return tier().dropItem();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        // The rider's client does not simulate the carpet, so the carpet only moves when a move
        // packet arrives. Without a handler the packet sets its position at once, between client
        // ticks (26.3 Entity.java:2618, ClientPacketListener.java:735-743), while the rider follows
        // on its next tick: the carpet is drawn up to three ticks of flight ahead of its rider.
        return SteppedInterpolationHandler.create(this);
    }

    /** How far the client drew the carpet in its last tick, in blocks. Zero on the server. */
    public Vec3 getDrawnMovement() {
        return this.drawnMovement;
    }

    // The first rider sitting down and the last one getting off play a vanilla sound from the
    // server, as the happy ghast's harness does (HappyGhast.java:309-334 in 26.3), so every player
    // nearby hears it.
    @Override
    protected void addPassenger(Entity passenger) {
        if (!this.isVehicle()) {
            this.playSound(SoundEvents.WOOL_PLACE);
        }
        super.addPassenger(passenger);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.isVehicle()) {
            this.playSound(SoundEvents.WOOL_STEP);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (isLocalInstanceAuthoritative()) {
            updateVelocity();
        }

        if (level().isClientSide()) {
            this.drawnMovement = position().subtract(this.xo, this.yo, this.zo);
            updateTilt(this.drawnMovement);
            updateAnimationSpeed(this.drawnMovement);
            spawnAmbientParticles();
        }

        collectAdditionalPassengers();

        if (hasControllingPassenger()) {
            //noinspection DataFlowIssue - should be obvious why
            setYRot(getControllingPassenger().getYRot());
        }
    }

    private void updateTilt(Vec3 movement) {
        this.prevRenderPitch = this.renderPitch;
        this.prevRenderRoll = this.renderRoll;

        float yaw = (float) Math.toRadians(this.getYRot());
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);

        double maxSpeed = tier().maxSpeed();
        float pitch =
                (float)
                        Mth.clamp(
                                (movement.y - (cos * movement.z - sin * movement.x))
                                        / maxSpeed
                                        * MAX_TILT_PITCH,
                                -MAX_TILT_PITCH,
                                MAX_TILT_PITCH);
        float roll =
                (float)
                        Mth.clamp(
                                (sin * movement.z + cos * movement.x) / maxSpeed * MAX_TILT_ROLL,
                                -MAX_TILT_ROLL,
                                MAX_TILT_ROLL);

        this.renderPitch += (pitch - this.renderPitch) * MOTION_EASING;
        this.renderRoll += (roll - this.renderRoll) * MOTION_EASING;
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

        setDeltaMovement(
                CarpetMotion.smoothVelocity(
                        currentVelocity, targetVelocity, tier().accelerationTime()));
        move(MoverType.PLAYER, this.getDeltaMovement());
    }

    private Vec3 getTargetVelocity(Player player, float yaw) {
        float sin = Mth.sin(yaw);
        float cos = Mth.cos(yaw);

        Vec3 targetVelocity =
                new Vec3(
                        cos * movementSideways - sin * movementForward,
                        pressingSpace ? CarpetMotion.verticalShare(player.getXRot()) : 0.0,
                        sin * movementSideways + cos * movementForward);

        return targetVelocity.scale(tier().maxSpeed());
    }

    private void updateInputs(Input input) {
        movementForward = CarpetMotion.impulse(input.forward(), input.backward());
        movementSideways = CarpetMotion.impulse(input.left(), input.right());
        float length =
                Mth.sqrt(movementForward * movementForward + movementSideways * movementSideways);
        if (length > 1.0F) {
            movementForward /= length;
            movementSideways /= length;
        }
        pressingSpace = input.jump();
    }

    private void updateAnimationSpeed(Vec3 movement) {
        this.prevAnimationSpeed = this.animationSpeed;
        float target = (float) Math.min(movement.horizontalDistance() / tier().maxSpeed(), 1.0);
        this.animationSpeed += (target - this.animationSpeed) * MOTION_EASING;
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
        spawnAmbientParticle(random, position, forward.reverse(), speed);
    }

    private void spawnAmbientParticle(
            RandomSource random, Vec3 position, Vec3 backward, float speed) {
        WeightedList<CarpetTier.Particle> particles = tier().particles();
        if (particles.isEmpty()) {
            return;
        }
        CarpetTier.Particle particle = particles.getRandomOrThrow(random);
        double trail = particle.trail() * speed;
        Vec3 velocity = particle.velocity();
        Vec3 spread = particle.velocitySpread();
        level().addParticle(
                        particle.options(),
                        position.x,
                        position.y,
                        position.z,
                        backward.x * trail + velocity.x + random.nextDouble() * spread.x,
                        backward.y * trail + velocity.y + random.nextDouble() * spread.y,
                        backward.z * trail + velocity.z + random.nextDouble() * spread.z);
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
