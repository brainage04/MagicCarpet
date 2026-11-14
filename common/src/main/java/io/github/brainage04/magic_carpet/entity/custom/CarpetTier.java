package io.github.brainage04.magic_carpet.entity.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.brainage04.magic_carpet.MagicCarpet;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

/**
 * How one carpet tier flies and looks. Each carpet entity type reads the entry named like itself
 * from the synced datapack registry {@code magic_carpet:tier}, so a datapack changes a tier with a
 * file at {@code data/magic_carpet/magic_carpet/tier/<entity name>.json}. The client reads the
 * speed, particles and model from the copy the server sends.
 *
 * @param maxSpeed top speed in blocks per tick
 * @param accelerationTime seconds to reach 99 % of a new target speed; slowing down takes twice as
 *     long
 * @param dropItem what the carpet drops when it is broken
 * @param particles the ambient particles, one picked by weight for each spawn
 * @param model the model's optional parts
 */
public record CarpetTier(
        double maxSpeed,
        double accelerationTime,
        Item dropItem,
        WeightedList<Particle> particles,
        Model model) {
    public static final ResourceKey<Registry<CarpetTier>> REGISTRY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "tier"));

    public static final Codec<CarpetTier> CODEC =
            RecordCodecBuilder.create(
                    instance ->
                            instance.group(
                                            positive("max_speed").forGetter(CarpetTier::maxSpeed),
                                            positive("acceleration_time")
                                                    .forGetter(CarpetTier::accelerationTime),
                                            BuiltInRegistries.ITEM
                                                    .byNameCodec()
                                                    .fieldOf("drop_item")
                                                    .forGetter(CarpetTier::dropItem),
                                            WeightedList.codec(Particle.CODEC)
                                                    .optionalFieldOf("particles", WeightedList.of())
                                                    .forGetter(CarpetTier::particles),
                                            Model.CODEC
                                                    .optionalFieldOf("model", Model.PLAIN)
                                                    .forGetter(CarpetTier::model))
                                    .apply(instance, CarpetTier::new));

    /** The tier of the entity type named {@code entityTypeId}. */
    public static ResourceKey<CarpetTier> key(Identifier entityTypeId) {
        return ResourceKey.create(REGISTRY, entityTypeId);
    }

    private static MapCodec<Double> positive(String name) {
        return Codec.DOUBLE
                .validate(
                        value ->
                                value > 0.0 && Double.isFinite(value)
                                        ? DataResult.success(value)
                                        : DataResult.error(
                                                () -> name + " must be above 0: " + value))
                .fieldOf(name);
    }

    /**
     * One kind of ambient particle. Its velocity is {@code velocity}, plus a random share of {@code
     * velocitySpread} on each axis, plus {@code trail} times the carpet's speed share (0 to 1)
     * blocks a tick towards the carpet's back.
     */
    public record Particle(
            ParticleOptions options, Vec3 velocity, Vec3 velocitySpread, double trail) {
        public static final MapCodec<Particle> CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                                ParticleTypes.CODEC
                                                        .fieldOf("particle")
                                                        .forGetter(Particle::options),
                                                Vec3.CODEC
                                                        .optionalFieldOf("velocity", Vec3.ZERO)
                                                        .forGetter(Particle::velocity),
                                                Vec3.CODEC
                                                        .optionalFieldOf(
                                                                "velocity_spread", Vec3.ZERO)
                                                        .forGetter(Particle::velocitySpread),
                                                Codec.DOUBLE
                                                        .optionalFieldOf("trail", 0.0)
                                                        .forGetter(Particle::trail))
                                        .apply(instance, Particle::new));
    }

    /**
     * The model's optional parts: tassels at both ends, curled front corners, and a second texture
     * ({@code <entity name>_glow.png}) drawn at full brightness.
     */
    public record Model(boolean tassels, boolean curls, boolean glow) {
        public static final Model PLAIN = new Model(false, false, false);

        public static final Codec<Model> CODEC =
                RecordCodecBuilder.create(
                        instance ->
                                instance.group(
                                                Codec.BOOL
                                                        .optionalFieldOf("tassels", false)
                                                        .forGetter(Model::tassels),
                                                Codec.BOOL
                                                        .optionalFieldOf("curls", false)
                                                        .forGetter(Model::curls),
                                                Codec.BOOL
                                                        .optionalFieldOf("glow", false)
                                                        .forGetter(Model::glow))
                                        .apply(instance, Model::new));
    }
}
