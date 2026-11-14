package io.github.brainage04.magic_carpet.client;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * The wind the rider of a magic carpet hears: the elytra's flying loop, at the volume and pitch the
 * elytra gives it for the same speed (ElytraOnPlayerSoundInstance.java:28-46 in 26.3), read from
 * how far the carpet is drawn each tick. It plays on the rider's own client, as vanilla's riding
 * loops do (LocalPlayer.java:196-216), and stops when the rider leaves the carpet.
 */
public class MagicCarpetFlightSound extends AbstractTickableSoundInstance {
    private final LocalPlayer rider;
    private final MagicCarpetEntity carpet;

    public MagicCarpetFlightSound(LocalPlayer rider, MagicCarpetEntity carpet) {
        super(
                SoundEvents.ELYTRA_FLYING,
                carpet.getSoundSource(),
                SoundInstance.createUnseededRandom());
        this.rider = rider;
        this.carpet = carpet;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = carpet.getX();
        this.y = carpet.getY();
        this.z = carpet.getZ();
    }

    /** The elytra's volume for a movement of {@code speedSqr} squared blocks a tick. */
    public static float volumeFor(double speedSqr) {
        return speedSqr >= 1.0E-7 ? Mth.clamp((float) speedSqr / 4.0F, 0.0F, 1.0F) : 0.0F;
    }

    /**
     * The volume this instance asks for, before {@link #getVolume()} scales it by the sound
     * definition's own volume.
     */
    public float speedVolume() {
        return this.volume;
    }

    @Override
    public boolean canPlaySound() {
        return !this.carpet.isSilent();
    }

    // The carpet is still when the rider sits down, so the loop starts at volume 0, and the sound
    // engine drops a sound that starts silent unless it allows it (SoundEngine.java:407-411).
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.carpet.isRemoved() || this.rider.getVehicle() != this.carpet) {
            this.stop();
            return;
        }
        this.x = this.carpet.getX();
        this.y = this.carpet.getY();
        this.z = this.carpet.getZ();
        this.volume = volumeFor(this.carpet.getDrawnMovement().lengthSqr());
        this.pitch = this.volume > 0.8F ? 1.0F + (this.volume - 0.8F) : 1.0F;
    }
}
