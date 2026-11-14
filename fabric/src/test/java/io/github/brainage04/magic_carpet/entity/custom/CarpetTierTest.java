package io.github.brainage04.magic_carpet.entity.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import net.minecraft.SharedConstants;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

class CarpetTierTest {
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Provide
    Arbitrary<CarpetTier> tiers() {
        Arbitrary<Double> positive = Arbitraries.doubles().between(1.0E-6, 1.0E6);
        Arbitrary<Item> items = Arbitraries.of(BuiltInRegistries.ITEM.stream().toList());
        Arbitrary<Double> component = Arbitraries.doubles().between(-10.0, 10.0);
        Arbitrary<Vec3> vectors = Combinators.combine(component, component, component).as(Vec3::new);
        Arbitrary<ParticleOptions> particleOptions =
                Arbitraries.of(
                        ParticleTypes.PORTAL,
                        ParticleTypes.END_ROD,
                        ParticleTypes.FLAME,
                        ParticleTypes.CLOUD,
                        ParticleTypes.ENCHANT);
        Arbitrary<Weighted<CarpetTier.Particle>> particles =
                Combinators.combine(
                                particleOptions,
                                vectors,
                                vectors,
                                Arbitraries.doubles().between(-1.0, 1.0),
                                Arbitraries.integers().between(1, 100))
                        .as(
                                (options, velocity, spread, trail, weight) ->
                                        new Weighted<>(
                                                new CarpetTier.Particle(
                                                        options, velocity, spread, trail),
                                                weight));
        Arbitrary<Boolean> flags = Arbitraries.of(true, false);
        return Combinators.combine(
                        positive,
                        positive,
                        items,
                        particles.list().ofMaxSize(4).map(WeightedList::of),
                        Combinators.combine(flags, flags, flags).as(CarpetTier.Model::new))
                .as(CarpetTier::new);
    }

    @Property
    void aTierSurvivesARoundTripThroughJson(@ForAll("tiers") CarpetTier tier) {
        JsonElement written = CarpetTier.CODEC.encodeStart(JsonOps.INSTANCE, tier).getOrThrow();
        JsonElement reread = JsonParser.parseString(written.toString());
        assertEquals(tier, CarpetTier.CODEC.parse(JsonOps.INSTANCE, reread).getOrThrow());
    }

    @Property
    void aSpeedOrTimeOfZeroOrLessIsRefused(
            @ForAll("tiers") CarpetTier tier,
            @ForAll @DoubleRange(min = -1.0E6, max = 0.0) double value) {
        assertRefused(tier, "max_speed", value);
        assertRefused(tier, "acceleration_time", value);
    }

    @Example
    void anEndlessSpeedOrTimeIsRefused() {
        CarpetTier tier = new CarpetTier(1.0, 1.0, Items.STICK, WeightedList.of(), CarpetTier.Model.PLAIN);
        for (double value : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertRefused(tier, "max_speed", value);
            assertRefused(tier, "acceleration_time", value);
        }
    }

    @Example
    void particlesAndModelMayBeLeftOut() {
        JsonElement json =
                JsonParser.parseString(
                        "{\"max_speed\": 0.8, \"acceleration_time\": 2.0, \"drop_item\": \"minecraft:stick\"}");
        CarpetTier tier = CarpetTier.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(
                new CarpetTier(0.8, 2.0, Items.STICK, WeightedList.of(), CarpetTier.Model.PLAIN),
                tier);
    }

    @Example
    void aParticleKeepsItsWeight() {
        JsonElement json =
                JsonParser.parseString(
                        "{\"max_speed\": 1, \"acceleration_time\": 1, \"drop_item\": \"minecraft:stick\","
                                + " \"particles\": [{\"weight\": 3, \"particle\": {\"type\": \"minecraft:portal\"},"
                                + " \"trail\": 0.05}]}");
        CarpetTier tier = CarpetTier.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(
                List.of(
                        new Weighted<>(
                                new CarpetTier.Particle(
                                        ParticleTypes.PORTAL, Vec3.ZERO, Vec3.ZERO, 0.05),
                                3)),
                tier.particles().unwrap());
    }

    private static void assertRefused(CarpetTier tier, String field, double value) {
        JsonObject json =
                CarpetTier.CODEC.encodeStart(JsonOps.INSTANCE, tier).getOrThrow().getAsJsonObject();
        json.addProperty(field, value);
        DataResult<CarpetTier> result = CarpetTier.CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(result.isError(), field + " " + value + " was accepted");
    }
}
